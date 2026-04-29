package com.dexheimer.treeinspectorandroid.domain.usecase

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaPendente
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.google.gson.Gson
import javax.inject.Inject

sealed class SaveResult {
	object Success : SaveResult() // Agora só temos um tipo de sucesso (salvo localmente)
	data class Failure(val message: String) : SaveResult()
}

class SalvarVistoriaUseCase @Inject constructor(
	private val vistoriaDao: VistoriaDao,
	private val demandaDao: DemandaDao,
	private val workManager: WorkManager
) {
	suspend operator fun invoke(demanda: Demanda, respostas: Map<String, Any>): SaveResult {
		return try {
			val demandaId = demanda.id
			val gson = Gson()

			val jsonRespostas = gson.toJson(respostas)

			// 2. Verifica se j exists para atualizao
			val vistoriaExistente = vistoriaDao.getVistoriaPorDemanda(demandaId)

			if (vistoriaExistente != null) {
				// ATUALIZA (Mesmo se j estava sincronizada, ao editar ela volta a ser no-sincronizada para reenviar)
				vistoriaDao.atualizarVistoria(
					demandaId = demandaId,
					json = jsonRespostas,
					sincronizado = false,
					data = System.currentTimeMillis()
				)
			} else {
				// INSERE NOVA
				val vistoriaPendente = VistoriaPendente(
					demandaId = demandaId,
					jsonRespostas = jsonRespostas
				)
				vistoriaDao.adicionarFila(vistoriaPendente)
			}
			
			demandaDao.updateStatus(demandaId, "Concluído")

			// 4. Agenda o Worker para rodar assim que tiver internet
			agendarSincronizacao()

			SaveResult.Success

		} catch (e: Exception) {
			e.printStackTrace()
			SaveResult.Failure("Erro ao salvar vistoria: ${e.message}")
		}
	}

	private fun agendarSincronizacao() {
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val syncRequest = OneTimeWorkRequestBuilder<com.dexheimer.treeinspectorandroid.data.worker.SyncVistoriasWorker>()
			.setConstraints(constraints)
			.setBackoffCriteria(
				androidx.work.BackoffPolicy.EXPONENTIAL,
				androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
				java.util.concurrent.TimeUnit.MILLISECONDS
			)
			.addTag("SYNC_VISTORIAS")
			.build()

		workManager.enqueueUniqueWork(
			"SYNC_VISTORIAS_TASK",
			androidx.work.ExistingWorkPolicy.REPLACE,
			syncRequest
		)
	}
}