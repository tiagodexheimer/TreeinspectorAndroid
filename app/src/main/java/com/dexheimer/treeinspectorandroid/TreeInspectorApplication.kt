package com.dexheimer.treeinspectorandroid

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dexheimer.treeinspectorandroid.data.worker.SyncVistoriasWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TreeInspectorApplication : Application(), Configuration.Provider {

	@Inject lateinit var workerFactory: HiltWorkerFactory

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		agendarSincronizacao()
	}

	private fun agendarSincronizacao() {
		// 1. Define as restrições (só roda se tiver internet)
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		// 2. Cria a requisição periódica (mínimo de 15 minutos no Android)
		val syncRequest = PeriodicWorkRequestBuilder<SyncVistoriasWorker>(
			15, TimeUnit.MINUTES
		)
			.setConstraints(constraints)
			.build()

		// 3. Enfileira a tarefa
		// Usamos KEEP para não substituir a tarefa se ela já estiver agendada
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			"SyncVistoriasWork",
			ExistingPeriodicWorkPolicy.KEEP,
			syncRequest
		)
	}
}