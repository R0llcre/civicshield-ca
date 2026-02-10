package org.regshield.ca

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.regshield.ca.api.RegShieldApi
import org.regshield.ca.data.VerifyRepository
import retrofit2.Retrofit

@OptIn(ExperimentalSerializationApi::class)
class VerifyRepositoryTest {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun verify_success_parsesResults() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
          {
            "results": [
              {
                "entry_type": "representative",
                "name": "Jane Doe",
                "organization_name": "Org",
                "city": "Los Angeles",
                "state": "CA",
                "source_url": "https://example.org",
                "score": 100,
                "matched_on": "name"
              }
            ],
            "data_freshness": {
              "ended_at": "2026-02-10T00:00:00Z",
              "source_sha256": "abc"
            }
          }
          """.trimIndent(),
        ),
    )
    server.start()
    try {
      val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

      val api = retrofit.create(RegShieldApi::class.java)
      val repo = VerifyRepository(api, json)

      val res = repo.verify("Jane", "CA")
      assertThat(res.isSuccess).isTrue()
      val body = res.getOrThrow()
      assertThat(body.results).hasSize(1)
      assertThat(body.results[0].name).isEqualTo("Jane Doe")
      assertThat(body.dataFreshness?.endedAt).isEqualTo("2026-02-10T00:00:00Z")
    } finally {
      server.shutdown()
    }
  }
}

