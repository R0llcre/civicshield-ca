using '../main.bicep'

param envShort = 'stg'
param prefix = 'regshieldca'
param location = 'westus2'

// Do not commit real credentials. Provide a strong password at deploy time:
//   az deployment group create ... --parameters pgAdminPassword='<strong>' ...
param pgAdminLogin = 'regshieldadmin'
param pgAdminPassword = '__REPLACE_ME__'

// Smallest practical staging SKU.
param pgSkuName = 'Standard_B1ms'
param pgDatabaseName = 'regshield'

// Placeholder until CI pushes the real image to ACR.
param containerImage = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'
param containerPort = 3000

param deployRunner = false

