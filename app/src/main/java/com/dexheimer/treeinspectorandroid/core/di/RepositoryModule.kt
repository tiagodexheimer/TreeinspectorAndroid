package com.dexheimer.treeinspectorandroid.core.di

import com.dexheimer.treeinspectorandroid.data.repository.DemandaRepositoryImpl
import com.dexheimer.treeinspectorandroid.data.repository.RotaRepositoryImpl
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

	@Binds
	@Singleton
	abstract fun bindRotaRepository(
		rotaRepositoryImpl: RotaRepositoryImpl
	): RotaRepository

	@Binds
	@Singleton
	abstract fun bindDemandaRepository(
		demandaRepositoryImpl: DemandaRepositoryImpl
	): DemandaRepository
}