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

// Important: VNet PUT operations can behave like "full state" updates.
// Defining subnets inline prevents accidental subnet deletions on redeploy.
resource vnet 'Microsoft.Network/virtualNetworks@2023-09-01' = {
  name: vnetName
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        vnetAddressPrefix
      ]
    }
    subnets: [
      {
        name: 'snet-aca'
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
      {
        name: 'snet-pe'
        properties: {
          addressPrefix: peSubnetPrefix
          privateEndpointNetworkPolicies: 'Disabled'
          privateLinkServiceNetworkPolicies: 'Disabled'
        }
      }
      {
        name: 'snet-runner'
        properties: {
          addressPrefix: runnerSubnetPrefix
        }
      }
      {
        name: 'snet-pg'
        properties: {
          addressPrefix: pgSubnetPrefix
          privateEndpointNetworkPolicies: 'Disabled'
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
    ]
  }
}

output vnetId string = vnet.id
output acaSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', vnet.name, 'snet-aca')
output peSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', vnet.name, 'snet-pe')
output runnerSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', vnet.name, 'snet-runner')
output pgSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', vnet.name, 'snet-pg')

