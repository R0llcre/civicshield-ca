package org.regshield.ca.data

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.regshield.ca.api.RegShieldApi
import org.regshield.ca.api.VerifyRequest
import org.regshield.ca.api.VerifyResponse
import retrofit2.HttpException

class VerifyRepository @Inject constructor(
  private val api: RegShieldApi,
  private val json: Json,
) {
  suspend fun verify(name: String, state: String): Result<VerifyResponse> {
    return try {
      val res = api.verifyRa(VerifyRequest(name = name, state = state))
      if (res.isSuccessful) {
        Result.success(res.body() ?: VerifyResponse())
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

