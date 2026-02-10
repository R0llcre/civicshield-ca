from __future__ import annotations

import argparse
import contextlib
import hashlib
import os
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
    ap.add_argument("--output-json", default="", help="If set, write parsed entries to this path (JSONL)")
    args = ap.parse_args()

    orgs_url, reps_url = discover_sources(args.reports_page)
    if args.orgs_url:
        orgs_url = args.orgs_url
    if args.reps_url:
        reps_url = args.reps_url

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

    orgs_published, org_entries = parse_orgs_by_state_city(
        str(orgs_path), dataset="orgs_by_state_city", source_url=orgs_url, source_sha256=orgs_sha
    )
    reps_published, rep_entries = parse_accredited_reps_roster(
        str(reps_path), dataset="reps_roster", source_url=reps_url, source_sha256=reps_sha
    )

    all_entries = org_entries + rep_entries
    print(f"Parsed org entries: {len(org_entries)} (published: {orgs_published})")
    print(f"Parsed rep entries: {len(rep_entries)} (published: {reps_published})")

    if args.output_json:
        import json

        out = Path(args.output_json)
        with out.open("w", encoding="utf-8") as f:
            for e in all_entries:
                f.write(json.dumps(asdict(e), default=str) + "\\n")
        print(f"Wrote {len(all_entries)} entries to {out}")

    if args.dry_run:
        return 0

    conn = connect_db()
    if not conn:
        print("DATABASE_URL not set. Use --dry-run or configure DATABASE_URL to write to DB.", file=sys.stderr)
        return 2

    with contextlib.closing(conn):
        conn.autocommit = False

        # Skip parsing + writing per dataset if hash unchanged.
        org_changed = latest_success_sha(conn, "orgs_by_state_city") != orgs_sha
        reps_changed = latest_success_sha(conn, "reps_roster") != reps_sha
        if not org_changed and not reps_changed:
            print("No changes detected (hash unchanged). Skipping.")
            return 0

        try:
            org_count = 0
            rep_count = 0

            if org_changed:
                org_run_id = insert_run(conn, "orgs_by_state_city", orgs_url, orgs_sha)
                conn.commit()
                org_rows = [asdict(e) for e in org_entries]
                org_count = upsert_entries(conn, org_rows)
                finish_run(conn, org_run_id, "success", org_count, None)
                conn.commit()

            if reps_changed:
                rep_run_id = insert_run(conn, "reps_roster", reps_url, reps_sha)
                conn.commit()
                rep_rows = [asdict(e) for e in rep_entries]
                rep_count = upsert_entries(conn, rep_rows)
                finish_run(conn, rep_run_id, "success", rep_count, None)
                conn.commit()

            print(f"Upserted orgs={org_count} reps={rep_count}")
            return 0
        except Exception as e:
            conn.rollback()
            # Best-effort: record failures for the datasets we attempted.
            for dataset, url, sha, attempted in [
                ("orgs_by_state_city", orgs_url, orgs_sha, org_changed),
                ("reps_roster", reps_url, reps_sha, reps_changed),
            ]:
                if not attempted:
                    continue
                try:
                    run_id = insert_run(conn, dataset, url, sha)
                    finish_run(conn, run_id, "failed", 0, str(e))
                    conn.commit()
                except Exception:
                    conn.rollback()
            raise


if __name__ == "__main__":
    raise SystemExit(main())
