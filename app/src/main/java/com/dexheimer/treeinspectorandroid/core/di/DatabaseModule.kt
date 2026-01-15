package com.dexheimer.treeinspectorandroid.core.di

import android.content.Context
import androidx.room.Room
import com.dexheimer.treeinspectorandroid.data.local.AppDatabase
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.FormularioDao
import com.dexheimer.treeinspectorandroid.data.local.RotaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDraftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

	@Provides
	@Singleton
	fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
		return Room.databaseBuilder(
			context,
			AppDatabase::class.java,
			"tree_inspector_db"
		)
			.fallbackToDestructiveMigration()
			.build()
	}

	@Provides
	fun provideRotaDao(db: AppDatabase): RotaDao = db.rotaDao()

	@Provides
	fun provideDemandaDao(db: AppDatabase): DemandaDao = db.demandaDao()

	@Provides
	fun provideVistoriaDao(db: AppDatabase): VistoriaDao = db.vistoriaDao()

	@Provides
	fun provideFormularioDao(db: AppDatabase): FormularioDao = db.formularioDao()

	@Provides
	fun provideVistoriaDraftDao(db: AppDatabase): VistoriaDraftDao = db.vistoriaDraftDao()
}