# Data Sources (MVP)

## DOJ EOIR Recognition & Accreditation (R&A) rosters

Source page:
- https://www.justice.gov/eoir/recognition-accreditation-roster-reports

ETL ingests:
- Organizations and Representatives, Listed by State (PDF)
- Accredited Representatives List (PDF)

Notes:
- Links and file IDs may change; ETL attempts best-effort discovery and supports overrides via env:
  - `RA_ORGS_URL`
  - `RA_REPS_URL`

## CA State Bar

Manual verification link used by the app:
- https://apps.calbar.ca.gov/attorney/LicenseeSearch/QuickSearch

## CA Secretary of State (Special Filings)

Manual verification link used by the app:
- https://specialfilings.sos.ca.gov/icbs

