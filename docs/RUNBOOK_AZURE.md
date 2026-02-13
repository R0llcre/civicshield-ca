# Runbook (Azure - Full Azure)

This runbook describes a **Full Azure** deployment using:

- Azure Container Apps (Web + `/api/*`)
- Azure Database for PostgreSQL Flexible Server (private access)
- Azure Key Vault + Azure Storage via private endpoints
- IaC with Bicep under `infra/azure/`

## 1. Provision staging infrastructure (Bicep)

1. Create the resource group:

```bash
az group create -n rg-regshieldca-stg-westus2 -l westus2
```

2. Deploy Bicep:

```bash
az deployment group create \
  -g rg-regshieldca-stg-westus2 \
  -f infra/azure/main.bicep \
  -p infra/azure/environments/staging.bicepparam \
  -p pgAdminPassword='<strong-password>'
```

Outputs include:
- Container App FQDN (once an app image is deployed)
- Postgres FQDN (private DNS)
- Key Vault URI
- Storage Blob endpoint

## 2. Apply database migrations (Azure Postgres)

Use the portable migrations under `db/migrations/` (not the Supabase ones):

1. Connect with `psql` (from a machine with VNet access):

```bash
psql "postgresql://<user>:<pass>@<pg-fqdn>:5432/regshield?sslmode=require"
```

2. Apply migrations in order:

```sql
\\i db/migrations/0001_init.sql
\\i db/migrations/0002_aid_org_provenance.sql
\\i db/migrations/0003_search_ra_entries.sql
\\i db/migrations/0004_partner_security.sql
```

## 3. Deploy Web/API to Azure Container Apps

This repo includes a staging deploy workflow:
- `.github/workflows/deploy-azure-staging.yml`

Prereqs:
- Configure GitHub OIDC for Azure and set secrets:
  - `AZURE_CLIENT_ID`
  - `AZURE_TENANT_ID`
  - `AZURE_SUBSCRIPTION_ID`

Run the workflow and provide:
- Resource group name
- Container App name
- ACR name

After deploy, set required app env vars in the Container App configuration:
- `DATABASE_URL` (should include `?sslmode=require` for Azure PG)
- `APP_ENV=stg` (already set by the workflow)

## 4. ETL (DOJ roster) in Full Azure

If Postgres is private (recommended), GitHub-hosted runners cannot reach it.

Recommended approach (Milestone 6):
- Run ETL on a **self-hosted GitHub Actions runner** inside the VNet, then point the ETL workflow `runs-on` to that runner label.

Infrastructure support:
- `infra/azure/main.bicep` supports `deployRunner=true` to create a small VM in `snet-runner`.

Until the runner is set up:
- Do not switch ETL to private-only DB access, or ETL runs will fail.
