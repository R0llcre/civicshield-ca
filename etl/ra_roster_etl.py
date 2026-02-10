from __future__ import annotations

import argparse
import contextlib
import datetime as dt
import hashlib
import json
import os
import shutil
import sys
from dataclasses import asdict
from pathlib import Path
from typing import Iterable, Optional

import psycopg
import requests
from bs4 import BeautifulSoup
from dotenv import load_dotenv

# Allow running as both:
# - `python3 -m etl.ra_roster_etl` (recommended)
# - `python3 etl/ra_roster_etl.py`
if __name__ == "__main__" and __package__ is None:
    sys.path.append(str(Path(__file__).resolve().parents[1]))

from etl.ra_parser import parse_accredited_reps_roster, parse_orgs_by_state_city


DEFAULT_REPORTS_PAGE = "https://www.justice.gov/eoir/recognition-accreditation-roster-reports"
DEFAULT_ORGS_URL = "https://www.justice.gov/eoir/media/1398081/dl?inline"
DEFAULT_REPS_URL = "https://www.justice.gov/media/1193716/dl?inline"


def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def download(url: str) -> bytes:
    r = requests.get(url, timeout=60)
    r.raise_for_status()
    return r.content


def discover_sources(reports_page: str) -> tuple[str, str]:
    """
    Best-effort discovery of current DOJ roster PDF links.
    Falls back to defaults if discovery fails.
    """
    try:
        html = requests.get(reports_page, timeout=30).text
        soup = BeautifulSoup(html, "html.parser")
        links = [(a.get_text(strip=True), a.get("href")) for a in soup.find_all("a") if a.get("href")]

        orgs = None
        reps = None
        for text, href in links:
            if not href:
                continue
            full = href
            if full.startswith("/"):
                full = "https://www.justice.gov" + full
            if "Listed by State" in text or "Listed by State" in href:
                orgs = full
            if "Accredited Representatives List" in text or "Accredited Representatives" in text:
                reps = full
        return orgs or DEFAULT_ORGS_URL, reps or DEFAULT_REPS_URL
    except Exception:
        return DEFAULT_ORGS_URL, DEFAULT_REPS_URL


def ensure_tmp() -> Path:
    p = Path(__file__).parent / "_tmp"
    p.mkdir(parents=True, exist_ok=True)
    return p


def connect_db() -> Optional[psycopg.Connection]:
    db_url = os.getenv("DATABASE_URL")
    if not db_url:
        return None
    return psycopg.connect(db_url)


def latest_success_sha(conn: psycopg.Connection, dataset: str) -> Optional[str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            select source_sha256
            from public.ra_import_run
            where dataset = %s and status = 'success'
            order by ended_at desc nulls last
            limit 1
            """,
            (dataset,),
        )
        row = cur.fetchone()
        return row[0] if row else None


def fetch_dedupe_keys(conn: psycopg.Connection, dataset: str, sha: str) -> set[str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            select dedupe_key
            from public.ra_entry
            where dataset = %s and source_sha256 = %s
            """,
            (dataset, sha),
        )
        return {r[0] for r in cur.fetchall()}


def insert_run(conn: psycopg.Connection, dataset: str, url: str, sha: str) -> str:
    with conn.cursor() as cur:
        cur.execute(
            """
            insert into public.ra_import_run (dataset, source_url, source_sha256, status)
            values (%s, %s, %s, 'running')
            returning id
            """,
            (dataset, url, sha),
        )
        return str(cur.fetchone()[0])


def finish_run(conn: psycopg.Connection, run_id: str, status: str, row_count: int = 0, error_summary: Optional[str] = None):
    with conn.cursor() as cur:
        cur.execute(
            """
            update public.ra_import_run
            set ended_at = now(),
                status = %s,
                row_count = %s,
                error_summary = %s
            where id = %s
            """,
            (status, row_count, error_summary, run_id),
        )


def upsert_entries(conn: psycopg.Connection, entries: Iterable[dict]) -> int:
    rows = list(entries)
    if not rows:
        return 0

    cols = [
        "dataset",
        "entry_type",
        "name",
        "organization_name",
        "city",
        "state",
        "address",
        "phone",
        "website",
        "accreditation_level",
        "status",
        "recognized_at",
        "expires_at",
        "pending_renewal",
        "source_url",
        "source_sha256",
        "source_published_at",
        "dedupe_key",
    ]

    def chunks(lst: list[dict], size: int):
        for i in range(0, len(lst), size):
            yield lst[i : i + size]

    total = 0
    placeholders = "(" + ",".join(["%s"] * len(cols)) + ")"

    for chunk in chunks(rows, 250):
        values_sql = ",".join([placeholders] * len(chunk))
        values = []
        for r in chunk:
            values.extend([r.get(c) for c in cols])

        sql = f"""
        insert into public.ra_entry ({",".join(cols)})
        values {values_sql}
        on conflict (dataset, source_sha256, dedupe_key)
        do update set
          name = excluded.name,
          organization_name = excluded.organization_name,
          city = excluded.city,
          state = excluded.state,
          address = excluded.address,
          phone = excluded.phone,
          website = excluded.website,
          accreditation_level = excluded.accreditation_level,
          status = excluded.status,
          recognized_at = excluded.recognized_at,
          expires_at = excluded.expires_at,
          pending_renewal = excluded.pending_renewal,
          source_url = excluded.source_url,
          source_published_at = excluded.source_published_at,
          imported_at = now()
        """

        with conn.cursor() as cur:
            cur.execute(sql, values)
        total += len(chunk)

    return total


def main() -> int:
    load_dotenv()

    ap = argparse.ArgumentParser(description="RegShield CA - R&A roster ETL")
    ap.add_argument("--reports-page", default=DEFAULT_REPORTS_PAGE)
    ap.add_argument("--orgs-url", default=os.getenv("RA_ORGS_URL") or "")
    ap.add_argument("--reps-url", default=os.getenv("RA_REPS_URL") or "")
    ap.add_argument("--dry-run", action="store_true", help="Parse and print stats, do not write to DB")
    ap.add_argument("--artifacts-dir", default="", help="If set, write PDFs + summary artifacts under this directory")
    ap.add_argument("--output-json", default="", help="If set, write parsed entries to this path (JSONL)")
    ap.add_argument("--output-summary", default="", help="If set, write a run summary JSON to this path")
    args = ap.parse_args()

    artifacts_dir = Path(args.artifacts_dir).resolve() if args.artifacts_dir else None
    if artifacts_dir:
        artifacts_dir.mkdir(parents=True, exist_ok=True)

    output_json = Path(args.output_json).resolve() if args.output_json else None
    output_summary = Path(args.output_summary).resolve() if args.output_summary else None
    if not output_summary and artifacts_dir:
        output_summary = artifacts_dir / "summary.json"

    summary: dict = {
        "ran_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "overall_status": "running",
        "db_connected": False,
        "datasets": {},
        "artifacts": {},
    }

    try:
        orgs_url, reps_url = discover_sources(args.reports_page)
        if args.orgs_url:
            orgs_url = args.orgs_url
        if args.reps_url:
            reps_url = args.reps_url

        summary["sources"] = {"orgs_url": orgs_url, "reps_url": reps_url, "reports_page": args.reports_page}

        tmp = ensure_tmp()

        print(f"Fetching orgs PDF: {orgs_url}")
        orgs_pdf = download(orgs_url)
        orgs_sha = sha256_bytes(orgs_pdf)
        orgs_path = tmp / f"orgs_{orgs_sha}.pdf"
        orgs_path.write_bytes(orgs_pdf)

        print(f"Fetching reps PDF: {reps_url}")
        reps_pdf = download(reps_url)
        reps_sha = sha256_bytes(reps_pdf)
        reps_path = tmp / f"reps_{reps_sha}.pdf"
        reps_path.write_bytes(reps_pdf)

        if artifacts_dir:
            orgs_copy = artifacts_dir / f"doj_orgs_{orgs_sha}.pdf"
            reps_copy = artifacts_dir / f"doj_reps_{reps_sha}.pdf"
            shutil.copyfile(orgs_path, orgs_copy)
            shutil.copyfile(reps_path, reps_copy)
            summary["artifacts"]["orgs_pdf"] = str(orgs_copy)
            summary["artifacts"]["reps_pdf"] = str(reps_copy)

        if args.dry_run:
            orgs_published, org_entries = parse_orgs_by_state_city(
                str(orgs_path), dataset="orgs_by_state_city", source_url=orgs_url, source_sha256=orgs_sha
            )
            reps_published, rep_entries = parse_accredited_reps_roster(
                str(reps_path), dataset="reps_roster", source_url=reps_url, source_sha256=reps_sha
            )
            print(f"Parsed org entries: {len(org_entries)} (published: {orgs_published})")
            print(f"Parsed rep entries: {len(rep_entries)} (published: {reps_published})")

            if output_json:
                output_json.parent.mkdir(parents=True, exist_ok=True)
                with output_json.open("w", encoding="utf-8") as f:
                    for e in org_entries + rep_entries:
                        f.write(json.dumps(asdict(e), default=str) + "\\n")
                summary["artifacts"]["entries_jsonl"] = str(output_json)
                print(f"Wrote {len(org_entries) + len(rep_entries)} entries to {output_json}")

            summary["datasets"]["orgs_by_state_city"] = {
                "source_url": orgs_url,
                "source_sha256": orgs_sha,
                "published_at": str(orgs_published) if orgs_published else None,
                "parsed_count": len(org_entries),
                "status": "dry_run",
            }
            summary["datasets"]["reps_roster"] = {
                "source_url": reps_url,
                "source_sha256": reps_sha,
                "published_at": str(reps_published) if reps_published else None,
                "parsed_count": len(rep_entries),
                "status": "dry_run",
            }
            summary["overall_status"] = "dry_run"
            return 0

        conn = connect_db()
        if not conn:
            summary["overall_status"] = "failed"
            summary["error"] = "DATABASE_URL not set"
            print("DATABASE_URL not set. Use --dry-run or configure DATABASE_URL to write to DB.", file=sys.stderr)
            return 2

        summary["db_connected"] = True
        with contextlib.closing(conn):
            conn.autocommit = False

            prev_org_sha = latest_success_sha(conn, "orgs_by_state_city")
            prev_rep_sha = latest_success_sha(conn, "reps_roster")
            org_changed = prev_org_sha != orgs_sha
            reps_changed = prev_rep_sha != reps_sha

            summary["datasets"]["orgs_by_state_city"] = {
                "source_url": orgs_url,
                "source_sha256": orgs_sha,
                "prev_success_sha256": prev_org_sha,
                "changed": org_changed,
                "status": "skipped" if not org_changed else "running",
            }
            summary["datasets"]["reps_roster"] = {
                "source_url": reps_url,
                "source_sha256": reps_sha,
                "prev_success_sha256": prev_rep_sha,
                "changed": reps_changed,
                "status": "skipped" if not reps_changed else "running",
            }

            if not org_changed and not reps_changed:
                summary["overall_status"] = "skipped_no_change"
                print("No changes detected (hash unchanged). Skipping.")
                return 0

            parsed_entries_all = []

            def process_dataset(
                *,
                dataset: str,
                source_url: str,
                source_sha: str,
                prev_sha: Optional[str],
                pdf_path: Path,
                parse_fn,
            ):
                run = summary["datasets"][dataset]
                run_id = insert_run(conn, dataset, source_url, source_sha)
                run["run_id"] = run_id
                conn.commit()

                try:
                    published, entries = parse_fn(
                        str(pdf_path), dataset=dataset, source_url=source_url, source_sha256=source_sha
                    )
                    run["published_at"] = str(published) if published else None
                    run["parsed_count"] = len(entries)

                    cur_keys = {e.dedupe_key for e in entries}
                    prev_keys = fetch_dedupe_keys(conn, dataset, prev_sha) if prev_sha else set()
                    run["diff"] = {
                        "added": len(cur_keys - prev_keys),
                        "removed": len(prev_keys - cur_keys),
                        "unchanged": len(cur_keys & prev_keys),
                    }

                    rows = [asdict(e) for e in entries]
                    upserted = upsert_entries(conn, rows)
                    run["upserted"] = upserted
                    finish_run(conn, run_id, "success", upserted, None)
                    conn.commit()
                    run["status"] = "success"
                    return entries
                except Exception as e:
                    conn.rollback()
                    run["status"] = "failed"
                    run["error_summary"] = str(e)
                    finish_run(conn, run_id, "failed", 0, str(e))
                    conn.commit()
                    raise

            if org_changed:
                org_entries = process_dataset(
                    dataset="orgs_by_state_city",
                    source_url=orgs_url,
                    source_sha=orgs_sha,
                    prev_sha=prev_org_sha,
                    pdf_path=orgs_path,
                    parse_fn=parse_orgs_by_state_city,
                )
                parsed_entries_all.extend(org_entries)

            if reps_changed:
                rep_entries = process_dataset(
                    dataset="reps_roster",
                    source_url=reps_url,
                    source_sha=reps_sha,
                    prev_sha=prev_rep_sha,
                    pdf_path=reps_path,
                    parse_fn=parse_accredited_reps_roster,
                )
                parsed_entries_all.extend(rep_entries)

            if output_json and parsed_entries_all:
                output_json.parent.mkdir(parents=True, exist_ok=True)
                with output_json.open("w", encoding="utf-8") as f:
                    for e in parsed_entries_all:
                        f.write(json.dumps(asdict(e), default=str) + "\\n")
                summary["artifacts"]["entries_jsonl"] = str(output_json)
                print(f"Wrote {len(parsed_entries_all)} entries to {output_json}")

            summary["overall_status"] = "success"
            return 0
    except Exception as e:
        summary["overall_status"] = "failed"
        summary["error"] = str(e)
        raise
    finally:
        if output_summary:
            output_summary.parent.mkdir(parents=True, exist_ok=True)
            output_summary.write_text(json.dumps(summary, indent=2, sort_keys=True), encoding="utf-8")
            print(f"Wrote summary to {output_summary}")


if __name__ == "__main__":
    raise SystemExit(main())
