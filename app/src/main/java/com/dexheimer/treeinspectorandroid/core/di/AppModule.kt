package com.dexheimer.treeinspectorandroid.core.di

import android.content.Context
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	@Provides
	@Singleton
	fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
		// O Hilt injeta o Context da aplicação, e nós o passamos para o construtor do SessionManager
		return SessionManager(context)
	}
}