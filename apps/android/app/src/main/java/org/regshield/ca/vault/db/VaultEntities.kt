package org.regshield.ca.vault.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_meta")
data class VaultMetaEntity(
  @PrimaryKey val id: Int = 1,
  val version: Int,
  val saltB64: String,
  val iterations: Int,
  val manifestIv: ByteArray?,
  val manifestCiphertext: ByteArray?,
)

@Entity(tableName = "vault_item")
data class VaultItemEntity(
  @PrimaryKey val id: String,
  val iv: ByteArray,
  val ciphertext: ByteArray,
)

