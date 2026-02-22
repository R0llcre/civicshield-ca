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

0. Allow required extensions (Azure Postgres allow-list)

Azure Database for PostgreSQL Flexible Server requires extensions to be allow-listed
before they can be created. Our schema uses:
- `pgcrypto` (UUID generation)
- `pg_trgm` (similarity search)

Run (once per server):

```bash
az postgres flexible-server parameter set \
  -g rg-regshieldca-stg-westus2 \
  -s <pg-server-name> \
  -n azure.extensions \
  -v "pgcrypto,pg_trgm"
```

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

## 3.1 Enable AI Q&A (OpenAI) + RAG on staging

The AI Q&A feature is served by `POST /api/ask` on the same Next.js app.

By default, `/api/ask` returns `501` until the Container App has:
- `OPENAI_API_KEY` configured (as a secret ref)
- `OPENAI_VECTOR_STORE_ID` configured (RAG required)
- optional `OPENAI_MODEL` configured (defaults to `gpt-5-mini`)

Security note:
- Do **not** paste API keys into chat/PRs/issues. If an API key was ever pasted into chat, **revoke/rotate it** immediately.

To enable OpenAI + RAG on Azure staging without putting secrets into shell history:

1. Save your **new** OpenAI API key to a local file (ignored by git):

```bash
mkdir -p .tmp
$EDITOR .tmp/openai_api_key.txt
```

2. Sync the local KB under `rag/kb/` to an OpenAI Vector Store (writes the id to `.tmp/openai_vector_store_id.txt`):

```bash
node scripts/rag_sync_openai_vector_store.mjs
```

3. Run the helper script (it writes the key to an Azure Container App secret, sets env vars, and verifies `/api/ask`):

```bash
bash scripts/azure_staging_enable_ask_rag.sh
```

## 4. ETL (DOJ roster) in Full Azure

If Postgres is private (recommended), GitHub-hosted runners cannot reach it.

Recommended approach (Milestone 6):
- Run ETL on a **self-hosted GitHub Actions runner** inside the VNet, then point the ETL workflow `runs-on` to that runner label.

Infrastructure support:
- `infra/azure/main.bicep` supports `deployRunner=true` to create a small VM in `snet-runner`.

Until the runner is set up:
- Do not switch ETL to private-only DB access, or ETL runs will fail.
