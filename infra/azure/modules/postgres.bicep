targetScope = 'resourceGroup'

@description('Azure region.')
param location string

@description('Postgres Flexible Server name.')
param serverName string

@description('Database name.')
param databaseName string

@description('Admin login (do not use "postgres").')
param adminLogin string

@secure()
@description('Admin password.')
param adminPassword string

@description('SKU name, e.g. Standard_B1ms.')
param skuName string

@description('Delegated subnet id for private access.')
param delegatedSubnetId string

@description('VNet id (for private DNS link).')
param vnetId string

// For Flexible Server "private access" (delegated subnet), the recommended zone is:
//   private.postgres.database.azure.com
resource pgDns 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'private.postgres.database.azure.com'
  location: 'global'
}

resource pgDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  name: 'vnet-link'
  parent: pgDns
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnetId
    }
    registrationEnabled: false
  }
}

resource pg 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: serverName
  location: location
  sku: {
    name: skuName
    tier: 'Burstable'
  }
  properties: {
    version: '16'
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    storage: {
      storageSizeGB: 32
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: {
      delegatedSubnetResourceId: delegatedSubnetId
      privateDnsZoneArmResourceId: pgDns.id
    }
  }
  dependsOn: [
    pgDnsLink
  ]
}

resource db 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  name: databaseName
  parent: pg
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

output fqdn string = pg.properties.fullyQualifiedDomainName
output server string = pg.name
output database string = db.name
output privateDnsZoneId string = pgDns.id
