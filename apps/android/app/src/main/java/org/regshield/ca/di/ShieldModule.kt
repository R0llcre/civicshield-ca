package org.regshield.ca.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.regshield.ca.data.ShieldPrefsRepository

@Module
@InstallIn(SingletonComponent::class)
object ShieldModule {
  @Provides
  @Singleton
  fun provideShieldPrefsRepository(@ApplicationContext ctx: Context): ShieldPrefsRepository =
    ShieldPrefsRepository(ctx)
}

