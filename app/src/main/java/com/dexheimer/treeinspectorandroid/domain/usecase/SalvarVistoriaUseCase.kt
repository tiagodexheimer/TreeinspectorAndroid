package com.dexheimer.treeinspectorandroid.domain.usecase

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaPendente
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.dexheimer.treeinspectorandroid.data.worker.SyncVistoriasWorker
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.google.gson.Gson
import javax.inject.Inject

// Resultado do salvamento
sealed class SaveResult {
	object SuccessOnline : SaveResult()
	object SuccessOffline : SaveResult()
	data class Failure(val message: String) : SaveResult()
}

class SalvarVistoriaUseCase @Inject constructor(
	private val apiService: ApiService,
	private val vistoriaDao: VistoriaDao,
	private val demandaDao: DemandaDao,
	private val workManager: WorkManager // Hilt deve fornecer a instância correta
) {
	suspend operator fun invoke(demanda: Demanda, respostas: Map<String, Any>): SaveResult {

		val demandaId = demanda.id

		// 1. Tentar Enviar Online
		try {
			val request = VistoriaRequest(demandaId, respostas)
			val response = apiService.salvarVistoria(request)

			if (response.isSuccessful) {
				demandaDao.updateStatus(demandaId, "concluido") // Atualiza status local
				return SaveResult.SuccessOnline
			}
		} catch (e: Exception) {
			// Falha de rede ou API. Ignora e tenta salvar offline.
		}

		// 2. Salvar na Fila Offline (Fallback)
		return salvarLocalmenteParaSincronizar(demandaId, respostas)
	}

	private suspend fun salvarLocalmenteParaSincronizar(demandaId: Int, respostas: Map<String, Any>): SaveResult {
		return try {
			val gson = Gson()
			val jsonRespostas = gson.toJson(respostas)

			val vistoriaPendente = VistoriaPendente(
				demandaId = demandaId,
				jsonRespostas = jsonRespostas
			)

			// Salva na fila e atualiza status para 'pendente de sync'
			vistoriaDao.adicionarFila(vistoriaPendente)
			demandaDao.updateStatus(demandaId, "concluido_pendente")

			// Agenda o Worker
			agendarSincronizacao()

			SaveResult.SuccessOffline

		} catch (e: Exception) {
			SaveResult.Failure("Erro crítico ao salvar localmente: ${e.message}")
		}
	}

	private fun agendarSincronizacao() {
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val syncRequest = OneTimeWorkRequestBuilder<SyncVistoriasWorker>()
			.setConstraints(constraints)
			.build()

		// CORREÇÃO: O workManager agora é injetado, mas é preciso enfileirar no contexto global.
		workManager.enqueue(syncRequest)
	}
}