package org.regshield.ca.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RegShieldApi {
  @POST("api/verify/ra")
  suspend fun verifyRa(@Body req: VerifyRequest): Response<VerifyResponse>

  @GET("api/directory/search")
  suspend fun searchDirectory(
    @Query("county") county: String?,
    @Query("lang") lang: String?,
    @Query("service") service: String?,
  ): Response<DirectoryResponse>

  @GET("api/meta/health")
  suspend fun health(): Response<HealthResponse>
}

