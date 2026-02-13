targetScope = 'resourceGroup'

@description('Short environment name, e.g. stg or prod.')
param envShort string

@description('Resource name prefix, e.g. regshieldca.')
param prefix string = 'regshieldca'

@description('Azure region. Locked by decision: West US 2.')
param location string = 'westus2'

@description('Postgres admin login (do not use "postgres").')
param pgAdminLogin string

@secure()
@description('Postgres admin password. Provide via CLI or a secure parameters pipeline; do not commit real values.')
param pgAdminPassword string

@description('Postgres SKU name (staging: Burstable B1ms is typical).')
param pgSkuName string = 'Standard_B1ms'

@description('Database name for the app.')
param pgDatabaseName string = 'regshield'

@description('Initial container image (placeholder). Deployment workflow will update to the real image in ACR.')
param containerImage string = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'

@description('App container port. Next.js standalone defaults to 3000.')
param containerPort int = 3000

@description('Deploy a self-hosted GitHub runner VM in the VNet (for private ETL). Default off; enable later once infra is stable.')
param deployRunner bool = false

@description('Runner VM admin username (only used when deployRunner=true).')
param runnerAdminUsername string = 'azureuser'

@description('Runner VM SSH public key (only used when deployRunner=true).')
param runnerSshPublicKey string = ''

// Keep the suffix stable within the RG, but short enough for KV/Storage naming constraints.
var suffix = toLower(substring(uniqueString(resourceGroup().id), 0, 4))

var envTag = envShort

var names = {
  vnet: 'vnet-${prefix}-${envTag}'
  log: 'log-${prefix}-${envTag}'
  appi: 'appi-${prefix}-${envTag}'
  kv: 'kv-${prefix}-${envTag}-${suffix}'
  storage: 'st${prefix}${envTag}${suffix}' // storage must be lower alnum only
  pg: 'pg-${prefix}-${envTag}-${suffix}'
  acr: 'acr${prefix}${envTag}${suffix}' // acr must be alnum only
  cae: 'cae-${prefix}-${envTag}'
  caWeb: 'ca-${prefix}-web-${envTag}'
  runnerVm: 'vm-ghrunner-${prefix}-${envTag}'
}

module network './modules/network.bicep' = {
  name: 'network-${envTag}'
  params: {
    location: location
    vnetName: names.vnet
  }
}

module logging './modules/logging.bicep' = {
  name: 'logging-${envTag}'
  params: {
    location: location
    logAnalyticsName: names.log
    appInsightsName: names.appi
  }
}

module postgres './modules/postgres.bicep' = {
  name: 'postgres-${envTag}'
  params: {
    location: location
    serverName: names.pg
    databaseName: pgDatabaseName
    adminLogin: pgAdminLogin
    adminPassword: pgAdminPassword
    skuName: pgSkuName
    delegatedSubnetId: network.outputs.pgSubnetId
    vnetId: network.outputs.vnetId
  }
}

module keyvault './modules/keyvault.bicep' = {
  name: 'keyvault-${envTag}'
  params: {
    location: location
    keyVaultName: names.kv
    peSubnetId: network.outputs.peSubnetId
    vnetId: network.outputs.vnetId
  }
}

module storage './modules/storage.bicep' = {
  name: 'storage-${envTag}'
  params: {
    location: location
    storageAccountName: names.storage
    peSubnetId: network.outputs.peSubnetId
    vnetId: network.outputs.vnetId
  }
}

module app './modules/containerapps.bicep' = {
  name: 'app-${envTag}'
  params: {
    location: location
    acrName: names.acr
    environmentName: names.cae
    containerAppName: names.caWeb
    infrastructureSubnetId: network.outputs.acaSubnetId
    logAnalyticsCustomerId: logging.outputs.logAnalyticsCustomerId
    logAnalyticsSharedKey: logging.outputs.logAnalyticsSharedKey
    containerImage: containerImage
    containerPort: containerPort
    appEnv: envTag
  }
}

module runner './modules/runner_vm.bicep' = if (deployRunner) {
  name: 'runner-${envTag}'
  params: {
    location: location
    vmName: names.runnerVm
    subnetId: network.outputs.runnerSubnetId
    adminUsername: runnerAdminUsername
    sshPublicKey: runnerSshPublicKey
  }
}

output containerAppFqdn string = app.outputs.fqdn
output postgresFqdn string = postgres.outputs.fqdn
output keyVaultUri string = keyvault.outputs.vaultUri
output storageBlobEndpoint string = storage.outputs.blobEndpoint
