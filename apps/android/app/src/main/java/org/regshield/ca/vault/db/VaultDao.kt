package org.regshield.ca.vault.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaultDao {
  @Query("SELECT * FROM vault_meta WHERE id = 1")
  suspend fun getMeta(): VaultMetaEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMeta(meta: VaultMetaEntity)

  @Query("DELETE FROM vault_item")
  suspend fun clearItems()

  @Query("SELECT * FROM vault_item WHERE id = :id")
  suspend fun getItem(id: String): VaultItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItem(item: VaultItemEntity)

  @Query("DELETE FROM vault_item WHERE id = :id")
  suspend fun deleteItem(id: String)
}

