targetScope = 'resourceGroup'

@description('Azure region.')
param location string

@description('Log Analytics workspace name.')
param logAnalyticsName string

@description('Application Insights name.')
param appInsightsName string

resource log 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsName
  location: location
  properties: {
    retentionInDays: 30
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

resource appi 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: log.id
  }
}

// Shared key is required by Container Apps log forwarding.
var logKeys = listKeys(log.id, log.apiVersion)

output logAnalyticsCustomerId string = log.properties.customerId
output logAnalyticsSharedKey string = logKeys.primarySharedKey
output appInsightsConnectionString string = appi.properties.ConnectionString

