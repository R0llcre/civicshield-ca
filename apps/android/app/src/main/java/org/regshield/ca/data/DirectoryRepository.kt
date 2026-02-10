package org.regshield.ca.data

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.regshield.ca.api.DirectoryResponse
import org.regshield.ca.api.RegShieldApi
import retrofit2.HttpException

class DirectoryRepository @Inject constructor(
  private val api: RegShieldApi,
  private val json: Json,
) {
  suspend fun search(county: String?, lang: String?, service: String?): Result<DirectoryResponse> {
    return try {
      val res = api.searchDirectory(
        county = county?.takeIf { it.isNotBlank() },
        lang = lang?.takeIf { it.isNotBlank() },
        service = service?.takeIf { it.isNotBlank() },
      )
      if (res.isSuccessful) {
        Result.success(res.body() ?: DirectoryResponse())
      } else {
        Result.failure(RuntimeException(parseError(res.errorBody()?.string())))
      }
    } catch (e: HttpException) {
      Result.failure(RuntimeException("Request failed"))
    } catch (e: Exception) {
      Result.failure(RuntimeException("Network error"))
    }
  }

  private fun parseError(raw: String?): String {
    if (raw.isNullOrBlank()) return "Request failed"
    return try {
      val el = json.parseToJsonElement(raw)
      el.jsonObject["error"]?.jsonPrimitive?.content ?: "Request failed"
    } catch (_: Exception) {
      "Request failed"
    }
  }
}

