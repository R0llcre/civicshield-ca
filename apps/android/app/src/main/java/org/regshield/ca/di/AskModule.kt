package org.regshield.ca.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import org.regshield.ca.data.AskLocalStore

@Module
@InstallIn(SingletonComponent::class)
object AskModule {
  @Provides
  @Singleton
  fun provideAskLocalStore(
    @ApplicationContext ctx: Context,
    json: Json,
  ): AskLocalStore = AskLocalStore(ctx, json)
}

