from __future__ import annotations

import dataclasses
import datetime as dt
import re
from typing import Iterable, Iterator, List, Optional

import pdfplumber

DATE_RE = re.compile(r"(\d{2}/\d{2}/\d{2}\*?)")

US_STATE_ABBR = {
    "ALABAMA": "AL",
    "ALASKA": "AK",
    "ARIZONA": "AZ",
    "ARKANSAS": "AR",
    "CALIFORNIA": "CA",
    "COLORADO": "CO",
    "CONNECTICUT": "CT",
    "DELAWARE": "DE",
    "DISTRICT OF COLUMBIA": "DC",
    "FLORIDA": "FL",
    "GEORGIA": "GA",
    "HAWAII": "HI",
    "IDAHO": "ID",
    "ILLINOIS": "IL",
    "INDIANA": "IN",
    "IOWA": "IA",
    "KANSAS": "KS",
    "KENTUCKY": "KY",
    "LOUISIANA": "LA",
    "MAINE": "ME",
    "MARYLAND": "MD",
    "MASSACHUSETTS": "MA",
    "MICHIGAN": "MI",
    "MINNESOTA": "MN",
    "MISSISSIPPI": "MS",
    "MISSOURI": "MO",
    "MONTANA": "MT",
    "NEBRASKA": "NE",
    "NEVADA": "NV",
    "NEW HAMPSHIRE": "NH",
    "NEW JERSEY": "NJ",
    "NEW MEXICO": "NM",
    "NEW YORK": "NY",
    "NORTH CAROLINA": "NC",
    "NORTH DAKOTA": "ND",
    "OHIO": "OH",
    "OKLAHOMA": "OK",
    "OREGON": "OR",
    "PENNSYLVANIA": "PA",
    "RHODE ISLAND": "RI",
    "SOUTH CAROLINA": "SC",
    "SOUTH DAKOTA": "SD",
    "TENNESSEE": "TN",
    "TEXAS": "TX",
    "UTAH": "UT",
    "VERMONT": "VT",
    "VIRGINIA": "VA",
    "WASHINGTON": "WA",
    "WEST VIRGINIA": "WV",
    "WISCONSIN": "WI",
    "WYOMING": "WY",
}


def _parse_report_updated_date(text: str) -> Optional[dt.date]:
    m = re.search(r"Report Last Updated on:\s*(\d{2}/\d{2}/\d{2})", text)
    if not m:
        return None
    mm, dd, yy = m.group(1).split("/")
    year = 2000 + int(yy)
    return dt.date(year, int(mm), int(dd))


@dataclasses.dataclass(frozen=True)
class ParsedEntry:
    dataset: str
    entry_type: str  # 'organization' | 'representative'
    name: str
    organization_name: Optional[str]
    city: Optional[str]
    state: str
    address: Optional[str]
    phone: Optional[str]
    website: Optional[str]
    accreditation_level: Optional[str]
    status: Optional[str]
    recognized_at: Optional[dt.date]
    expires_at: Optional[dt.date]
    pending_renewal: bool
    source_url: str
    source_sha256: str
    source_published_at: Optional[dt.date]
    dedupe_key: str


def _dedupe_key(
    entry_type: str,
    name: str,
    organization_name: Optional[str],
    city: Optional[str],
    state: str,
) -> str:
    parts = [
        entry_type.strip().lower(),
        name.strip().lower(),
        (organization_name or "").strip().lower(),
        (city or "").strip().lower(),
        state.strip().upper(),
    ]
    return "|".join(parts)


def parse_orgs_by_state_city(
    pdf_path: str,
    *,
    dataset: str,
    source_url: str,
    source_sha256: str,
) -> tuple[Optional[dt.date], list[ParsedEntry]]:
    """
    Parse the 'Organizations and Representatives, Listed by State' PDF.

    We only ingest the organization section (which contains contact info by city/state).
    """
    entries: list[ParsedEntry] = []
    current_state: Optional[str] = None
    current_city: Optional[str] = None
    published_at: Optional[dt.date] = None

    def next_nonempty(lines: list[str], i: int) -> Optional[str]:
        for j in range(i + 1, len(lines)):
            s = lines[j].strip()
            if s:
                return s
        return None

    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            text = page.extract_text() or ""
            if not published_at:
                published_at = _parse_report_updated_date(text)

            lines = [ln.strip() for ln in text.splitlines()]
            i = 0
            while i < len(lines):
                line = lines[i]
                if not line:
                    i += 1
                    continue

                # Stop once the accredited representative section begins.
                if line.startswith("Recognized Accredited Representative"):
                    return published_at, entries

                if line.isupper() and line in US_STATE_ABBR:
                    current_state = US_STATE_ABBR[line]
                    current_city = None
                    i += 1
                    continue

                # Skip table header fragments.
                if line in {
                    "Recognized Date Recognition Organization",
                    "Organization Recognized Expiration Date Status",
                    "Recognized Date Recognition Organization Organization Recognized Expiration Date Status",
                }:
                    i += 1
                    continue

                # City heading: the next non-empty line looks like an org row with two dates.
                nxt = next_nonempty(lines, i)
                if (
                    current_state
                    and nxt
                    and len(DATE_RE.findall(nxt)) >= 2
                    and not DATE_RE.search(line)
                    and not any(ch.isdigit() for ch in line)
                    and not line.isupper()
                    and len(line) < 80
                ):
                    current_city = line
                    i += 1
                    continue

                # Organization row with 2 dates.
                dates = DATE_RE.findall(line)
                if current_state and len(dates) >= 2:
                    # Split from the left: name + rec_date + exp_date + status.
                    parts = line.split()
                    # Find first 2 date tokens in order.
                    date_positions = [idx for idx, p in enumerate(parts) if DATE_RE.fullmatch(p)]
                    if len(date_positions) >= 2:
                        p1, p2 = date_positions[0], date_positions[1]
                        name = " ".join(parts[:p1]).strip()
                        rec = parts[p1]
                        exp = parts[p2]
                        status = " ".join(parts[p2 + 1 :]).strip() or None

                        pending = exp.endswith("*")
                        exp_clean = exp.rstrip("*")

                        recognized_at = _mmddyy(rec)
                        expires_at = _mmddyy(exp_clean)

                        # Consume address/phone lines until next city/state/org row.
                        addr_lines: list[str] = []
                        phone: Optional[str] = None
                        j = i + 1
                        while j < len(lines):
                            candidate = lines[j].strip()
                            if not candidate:
                                j += 1
                                continue
                            if candidate.startswith("Recognized Accredited Representative"):
                                break
                            if candidate.isupper() and candidate in US_STATE_ABBR:
                                break
                            # Next city heading?
                            nxt2 = next_nonempty(lines, j)
                            if (
                                nxt2
                                and len(DATE_RE.findall(nxt2)) >= 2
                                and not DATE_RE.search(candidate)
                                and not any(ch.isdigit() for ch in candidate)
                                and not candidate.isupper()
                                and len(candidate) < 80
                            ):
                                break
                            if current_state and len(DATE_RE.findall(candidate)) >= 2:
                                break

                            if re.match(r"^\(\d{3}\)\s*\d{3}-\d{4}$", candidate) or re.match(
                                r"^\d{3}-\d{3}-\d{4}$", candidate
                            ):
                                phone = candidate
                            else:
                                addr_lines.append(candidate)
                            j += 1

                        address = "\\n".join(addr_lines).strip() or None

                        entries.append(
                            ParsedEntry(
                                dataset=dataset,
                                entry_type="organization",
                                name=name,
                                organization_name=None,
                                city=current_city,
                                state=current_state,
                                address=address,
                                phone=phone,
                                website=None,
                                accreditation_level=None,
                                status=status,
                                recognized_at=recognized_at,
                                expires_at=expires_at,
                                pending_renewal=pending or ("Pending Renewal" in (address or "")),
                                source_url=source_url,
                                source_sha256=source_sha256,
                                source_published_at=published_at,
                                dedupe_key=_dedupe_key("organization", name, None, current_city, current_state),
                            )
                        )

                        i = j
                        continue

                i += 1

    return published_at, entries


def _mmddyy(value: str) -> Optional[dt.date]:
    try:
        mm, dd, yy = value.split("/")
        return dt.date(2000 + int(yy), int(mm), int(dd))
    except Exception:
        return None


def parse_accredited_reps_roster(
    pdf_path: str,
    *,
    dataset: str,
    source_url: str,
    source_sha256: str,
) -> tuple[Optional[dt.date], list[ParsedEntry]]:
    """
    Parse the 'Accredited Representatives Roster' PDF.

    This PDF is a table; we use word coordinates to split into two columns:
    - left column: representative info
    - right column: recognized organization info
    """

    entries: list[ParsedEntry] = []
    published_at: Optional[dt.date] = None

    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            if not published_at:
                published_at = _parse_report_updated_date(page.extract_text() or "")

            words = page.extract_words()  # type: ignore[no-untyped-call]
            if not words:
                continue

            # Group words into lines by Y coordinate.
            lines_map: dict[float, list[dict]] = {}
            for w in words:
                top = round(float(w["top"]), 1)
                lines_map.setdefault(top, []).append(w)

            line_items = []
            for top in sorted(lines_map.keys()):
                ws = sorted(lines_map[top], key=lambda x: float(x["x0"]))
                left = [x["text"] for x in ws if float(x["x0"]) < 250]
                right = [x["text"] for x in ws if float(x["x0"]) >= 250]
                rep_text = " ".join(left).strip()
                org_text = " ".join(right).strip()
                if not rep_text and not org_text:
                    continue
                # Drop section letter markers like "A", "B", etc.
                if len(rep_text) == 1 and rep_text.isalpha() and rep_text.isupper() and not org_text:
                    continue
                line_items.append((rep_text, org_text))

            # Build logical records: start at a "main" row containing rep expiry and org dates.
            records: list[list[tuple[str, str]]] = []
            current: list[tuple[str, str]] = []
            for rep_text, org_text in line_items:
                rep_dates = DATE_RE.findall(rep_text)
                org_dates = DATE_RE.findall(org_text)
                is_main = bool(rep_dates) and len(org_dates) >= 2 and (rep_text.endswith("Active") or org_text.endswith("Active"))

                if is_main:
                    if current:
                        records.append(current)
                    current = [(rep_text, org_text)]
                else:
                    if current:
                        current.append((rep_text, org_text))
            if current:
                records.append(current)

            for rec in records:
                main_rep, main_org = rec[0]
                rep_extras = [r for r, _ in rec[1:] if r]
                org_extras = [o for _, o in rec[1:] if o]

                parsed = _parse_rep_record(main_rep, rep_extras, main_org, org_extras)
                if parsed:
                    entries.append(
                        ParsedEntry(
                            dataset=dataset,
                            entry_type="representative",
                            name=parsed["rep_name"],
                            organization_name=parsed["org_name"],
                            city=None,
                            state="US",
                            address=None,
                            phone=None,
                            website=None,
                            accreditation_level=parsed.get("level"),
                            status=parsed.get("rep_status"),
                            recognized_at=None,
                            expires_at=parsed.get("rep_expires"),
                            pending_renewal=parsed.get("rep_pending", False),
                            source_url=source_url,
                            source_sha256=source_sha256,
                            source_published_at=published_at,
                            dedupe_key=_dedupe_key("representative", parsed["rep_name"], parsed["org_name"], None, "US"),
                        )
                    )

    return published_at, entries


def _parse_rep_record(
    main_rep: str, rep_extras: list[str], main_org: str, org_extras: list[str]
) -> Optional[dict]:
    # Representative main row: "<name...> <mm/dd/yy[*]> <Status>"
    rep_parts = main_rep.split()
    if len(rep_parts) < 3:
        return None
    rep_status = rep_parts[-1]
    rep_exp = rep_parts[-2]
    if not DATE_RE.fullmatch(rep_exp):
        return None
    rep_name = " ".join(rep_parts[:-2])

    # Add continuation pieces before the date.
    if rep_extras:
        rep_name = f"{rep_name} {' '.join(rep_extras)}".strip()

    level = None
    m_level = re.search(r"\(([^)]+)\)", rep_name)
    if m_level:
        level = m_level.group(1).strip()
        rep_name = (rep_name[: m_level.start()] + rep_name[m_level.end() :]).strip()

    rep_pending = rep_exp.endswith("*") or any("Pending" in x for x in rep_extras)
    rep_expires = _mmddyy(rep_exp.rstrip("*"))

    # Organization main row: "<org...> <mm/dd/yy> <mm/dd/yy[*]> <Status>"
    org_parts = main_org.split()
    if len(org_parts) < 4:
        return None
    org_status = org_parts[-1]
    org_exp = org_parts[-2]
    org_rec = org_parts[-3]
    if not (DATE_RE.fullmatch(org_exp) and DATE_RE.fullmatch(org_rec)):
        return None
    org_name = " ".join(org_parts[:-3])
    if org_extras:
        org_name = f"{org_name} {' '.join(org_extras)}".strip()

    # We currently keep org status/dates out of the representative row, but could be added later.
    return {
        "rep_name": rep_name,
        "rep_status": rep_status,
        "rep_expires": rep_expires,
        "rep_pending": rep_pending,
        "level": level,
        "org_name": org_name,
        "org_status": org_status,
        "org_recognized": _mmddyy(org_rec),
        "org_expires": _mmddyy(org_exp.rstrip("*")),
        "org_pending": org_exp.endswith("*") or any("Pending" in x for x in org_extras),
    }
