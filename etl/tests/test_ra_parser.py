import datetime as dt

from etl.ra_parser import _parse_rep_record, _parse_report_updated_date, _dedupe_key


def test_parse_report_updated_date():
    d = _parse_report_updated_date("Report Last Updated on: 02/09/26")
    assert d == dt.date(2026, 2, 9)


def test_parse_rep_record_with_wrapped_cells():
    # Mirrors the "Abasto Revilla, Claudia ... Latin Advocacy Network ..." row on page 1 of the roster.
    out = _parse_rep_record(
        main_rep="Abasto Revilla, Claudia 02/28/26 Active",
        rep_extras=["Mabel"],
        main_org="Latin Advocacy Network 02/28/23 06/25/31 Active",
        org_extras=["(LATINAN)"],
    )
    assert out is not None
    assert out["rep_name"] == "Abasto Revilla, Claudia Mabel"
    assert out["org_name"] == "Latin Advocacy Network (LATINAN)"
    assert out["rep_expires"] == dt.date(2026, 2, 28)


def test_dedupe_key_is_stable():
    k1 = _dedupe_key("representative", "Diaz, Samantha", "Org", None, "US")
    k2 = _dedupe_key("representative", "  Diaz, Samantha  ", "Org", "", "us")
    assert k1 == k2
