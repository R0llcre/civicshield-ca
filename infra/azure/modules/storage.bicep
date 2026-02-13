targetScope = 'resourceGroup'

@description('Azure region.')
param location string

@description('Storage account name (lowercase letters/numbers only).')
param storageAccountName string

@description('Private endpoints subnet id.')
param peSubnetId string

@description('VNet id for private DNS link.')
param vnetId string

resource blobDns 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.blob.core.windows.net'
  location: 'global'
}

resource blobDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  name: 'vnet-link'
  parent: blobDns
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnetId
    }
    registrationEnabled: false
  }
}

resource st 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    minimumTlsVersion: 'TLS1_2'
    allowBlobPublicAccess: false
    publicNetworkAccess: 'Disabled'
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-01-01' = {
  name: 'default'
  parent: st
  properties: {}
}

resource container 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  name: 'etl-artifacts'
  parent: blobService
  properties: {
    publicAccess: 'None'
  }
}

resource pe 'Microsoft.Network/privateEndpoints@2023-09-01' = {
  name: 'pe-${storageAccountName}-blob'
  location: location
  properties: {
    subnet: {
      id: peSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'blob'
        properties: {
          privateLinkServiceId: st.id
          groupIds: [
            'blob'
          ]
        }
      }
    ]
  }
  dependsOn: [
    blobDnsLink
  ]
}

resource peDns 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-09-01' = {
  name: 'default'
  parent: pe
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'blob'
        properties: {
          privateDnsZoneId: blobDns.id
        }
      }
    ]
  }
}

output storageAccountId string = st.id
output blobEndpoint string = st.properties.primaryEndpoints.blob
