package org.regshield.ca.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.regshield.ca.vault.db.VaultDao
import org.regshield.ca.vault.db.VaultDatabase

@Module
@InstallIn(SingletonComponent::class)
object VaultModule {
  @Provides
  @Singleton
  fun provideVaultDatabase(@ApplicationContext ctx: Context): VaultDatabase =
    Room.databaseBuilder(ctx, VaultDatabase::class.java, "regshield-vault.db")
      .fallbackToDestructiveMigration()
      .build()

  @Provides
  fun provideVaultDao(db: VaultDatabase): VaultDao = db.vaultDao()
}

