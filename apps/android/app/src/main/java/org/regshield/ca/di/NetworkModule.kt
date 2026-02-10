package org.regshield.ca.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import org.regshield.ca.BuildConfig
import org.regshield.ca.api.RegShieldApi
import retrofit2.Retrofit

@OptIn(ExperimentalSerializationApi::class)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  private fun normalizedBaseUrl(): String {
    val raw = BuildConfig.API_BASE_URL.trim()
    return if (raw.endsWith("/")) raw else "$raw/"
  }

  @Provides
  @Singleton
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()

  @Provides
  @Singleton
  fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
      .baseUrl(normalizedBaseUrl())
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory(contentType))
      .build()
  }

  @Provides
  @Singleton
  fun provideRegShieldApi(retrofit: Retrofit): RegShieldApi =
    retrofit.create(RegShieldApi::class.java)
}
