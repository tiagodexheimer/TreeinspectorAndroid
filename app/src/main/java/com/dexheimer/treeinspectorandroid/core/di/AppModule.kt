package com.dexheimer.treeinspectorandroid.core.di

import android.content.Context
import androidx.work.WorkManager
import com.dexheimer.treeinspectorandroid.core.util.SessionManager // <--- Não esqueça do Import
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	// ... (outros provides que já existam aqui, como provideDatabase)

	// --- ADICIONE ESTA FUNÇÃO ---
	@Provides
	@Singleton
	fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
		return SessionManager(context)
	}

	@Provides
	@Singleton
	fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
		return WorkManager.getInstance(context)
	}
}