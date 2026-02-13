targetScope = 'resourceGroup'

@description('Azure region.')
param location string

@description('VNet name.')
param vnetName string

@description('VNet address space.')
param vnetAddressPrefix string = '10.10.0.0/16'

@description('Container Apps infrastructure subnet. Must be /23 or larger.')
param acaSubnetPrefix string = '10.10.0.0/23'

@description('Private Endpoints subnet.')
param peSubnetPrefix string = '10.10.2.0/24'

@description('Self-hosted runner subnet.')
param runnerSubnetPrefix string = '10.10.3.0/24'

@description('Postgres Flexible Server delegated subnet (private access).')
param pgSubnetPrefix string = '10.10.4.0/24'

resource vnet 'Microsoft.Network/virtualNetworks@2023-09-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        vnetAddressPrefix
      ]
    }
  }
}

resource snetAca 'Microsoft.Network/virtualNetworks/subnets@2023-09-01' = {
  name: 'snet-aca'
  parent: vnet
  properties: {
    addressPrefix: acaSubnetPrefix
    delegations: [
      {
        name: 'aca'
        properties: {
          serviceName: 'Microsoft.App/environments'
        }
      }
    ]
  }
}

resource snetPe 'Microsoft.Network/virtualNetworks/subnets@2023-09-01' = {
  name: 'snet-pe'
  parent: vnet
  properties: {
    addressPrefix: peSubnetPrefix
    privateEndpointNetworkPolicies: 'Disabled'
    privateLinkServiceNetworkPolicies: 'Disabled'
  }
}

resource snetRunner 'Microsoft.Network/virtualNetworks/subnets@2023-09-01' = {
  name: 'snet-runner'
  parent: vnet
  properties: {
    addressPrefix: runnerSubnetPrefix
  }
}

resource snetPg 'Microsoft.Network/virtualNetworks/subnets@2023-09-01' = {
  name: 'snet-pg'
  parent: vnet
  properties: {
    addressPrefix: pgSubnetPrefix
    delegations: [
      {
        name: 'postgres'
        properties: {
          serviceName: 'Microsoft.DBforPostgreSQL/flexibleServers'
        }
      }
    ]
  }
}

output vnetId string = vnet.id
output acaSubnetId string = snetAca.id
output peSubnetId string = snetPe.id
output runnerSubnetId string = snetRunner.id
output pgSubnetId string = snetPg.id

