package org.regshield.ca.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequest(
  val name: String,
  val state: String = "CA",
)

@Serializable
data class VerifyResponse(
  val results: List<RaEntry> = emptyList(),
  @SerialName("data_freshness") val dataFreshness: DataFreshness? = null,
  val error: String? = null,
)

@Serializable
data class DataFreshness(
  @SerialName("ended_at") val endedAt: String? = null,
  @SerialName("source_sha256") val sourceSha256: String? = null,
)

@Serializable
data class RaEntry(
  @SerialName("entry_type") val entryType: String? = null,
  val name: String,
  @SerialName("organization_name") val organizationName: String? = null,
  val city: String? = null,
  val state: String? = null,
  val address: String? = null,
  val phone: String? = null,
  val website: String? = null,
  @SerialName("accreditation_level") val accreditationLevel: String? = null,
  val status: String? = null,
  @SerialName("expires_at") val expiresAt: String? = null,
  @SerialName("pending_renewal") val pendingRenewal: Boolean? = null,
  @SerialName("source_url") val sourceUrl: String? = null,
  @SerialName("source_sha256") val sourceSha256: String? = null,
  val score: Int? = null,
  @SerialName("matched_on") val matchedOn: String? = null,
)

@Serializable
data class DirectoryResponse(
  val results: List<AidOrg> = emptyList(),
  val error: String? = null,
)

@Serializable
data class AidOrg(
  val id: String,
  val name: String,
  val website: String? = null,
  val phone: String? = null,
  val email: String? = null,
  val counties: List<String> = emptyList(),
  val languages: List<String> = emptyList(),
  val services: List<String> = emptyList(),
  @SerialName("intake_status") val intakeStatus: String,
  @SerialName("next_clinic_at") val nextClinicAt: String? = null,
  @SerialName("eta_callback_days") val etaCallbackDays: Int? = null,
  @SerialName("source_url") val sourceUrl: String? = null,
  @SerialName("verified_at") val verifiedAt: String? = null,
  @SerialName("verified_by") val verifiedBy: String? = null,
  @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class HealthResponse(
  val ok: Boolean,
  @SerialName("supabase_configured") val supabaseConfigured: Boolean,
  @SerialName("ra_import") val raImport: HealthRaImport? = null,
)

@Serializable
data class HealthRaImport(
  @SerialName("orgs_by_state_city") val orgsByStateCity: HealthRunRow? = null,
  @SerialName("reps_roster") val repsRoster: HealthRunRow? = null,
)

@Serializable
data class HealthRunRow(
  @SerialName("ended_at") val endedAt: String? = null,
  @SerialName("source_sha256") val sourceSha256: String? = null,
)

