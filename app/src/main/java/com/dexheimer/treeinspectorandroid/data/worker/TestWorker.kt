package com.dexheimer.treeinspectorandroid.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class TestWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

	override suspend fun doWork(): Result {
		Log.d("TestWorker", ">>> INICIANDO O WORKER DE TESTE <<<")

		// Simula um processamento de 3 segundos
		delay(3000)

		Log.d("TestWorker", ">>> WORKER DE TESTE FINALIZADO COM SUCESSO <<<")

		return Result.success()
	}
}