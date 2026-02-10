package org.regshield.ca.vault.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
  entities = [
    VaultMetaEntity::class,
    VaultItemEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
  abstract fun vaultDao(): VaultDao
}

