# Azure Infrastructure (Bicep)

This folder defines **Full Azure** infrastructure for RegShield CA using **Bicep**, targeting:

- Compute: Azure Container Apps
- DB: Azure Database for PostgreSQL Flexible Server (private access)
- Secrets: Azure Key Vault (private endpoint)
- Artifacts: Azure Storage (Blob) (private endpoint)
- Observability: Log Analytics + Application Insights
- Region: West US 2

## Deploy (staging)

1. Create a resource group (once):

```bash
az group create -n rg-regshieldca-stg-westus2 -l westus2
```

2. Deploy:

```bash
az deployment group create \
  -g rg-regshieldca-stg-westus2 \
  -f infra/azure/main.bicep \
  -p infra/azure/environments/staging.bicepparam \
  -p pgAdminPassword='<strong-password>'
```

## Notes

- The Container App is created with a **placeholder image**. The real app image is built and deployed by CI.
- Postgres is created with **public network access disabled** and a **private DNS zone** linked to the VNet.
- Key Vault/Storage are created with **public network access disabled** + **private endpoints**.

