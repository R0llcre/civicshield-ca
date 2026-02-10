package org.regshield.ca.vault

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VaultParams(
  val version: Int = 1,
  @SerialName("saltB64") val saltB64: String,
  val iterations: Int,
)

@Serializable
data class VaultItemMeta(
  val id: String,
  val createdAt: String,
  val originalName: String,
  val contentType: String,
  val size: Long,
)

@Serializable
data class VaultPlain(
  val items: List<VaultItemMeta> = emptyList(),
  val files: List<VaultFile> = emptyList(),
)

@Serializable
data class VaultFile(
  val id: String,
  @SerialName("bytesB64") val bytesB64: String,
)

@Serializable
data class EncryptedBlob(
  @SerialName("ivB64") val ivB64: String,
  @SerialName("ciphertextB64") val ciphertextB64: String,
)

@Serializable
data class VaultManifest(
  val items: List<VaultItemMeta> = emptyList(),
)

