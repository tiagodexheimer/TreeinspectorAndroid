package com.dexheimer.treeinspectorandroid.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dexheimer.treeinspectorandroid.domain.usecase.SyncVistoriasUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncVistoriasWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val syncVistoriasUseCase: SyncVistoriasUseCase // <--- INJETADO
) : CoroutineWorker(appContext, params) {

	private val TAG = "SyncWorker"

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

		// 1. O Hilt injetou o UseCase com todas as dependências.
		// 2. O UseCase cuida da autenticação (via SessionManager) e do envio.

		val result = syncVistoriasUseCase()

		return@withContext result.fold(
			onSuccess = { todasSincronizadas ->
				if (todasSincronizadas) {
					Log.i(TAG, "Sincronização completa com sucesso.")
					Result.success()
				} else {
					Log.w(TAG, "Algumas vistorias falharam ou token expirou. Retentando.")
					Result.retry()
				}
			},
			onFailure = { e ->
				Log.e(TAG, "Erro crítico na sincronização: ${e.message}", e)
				Result.retry()
			}
		)
	}
}