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

			// 1. Converte o mapa (com caminhos locais) para JSON
			val jsonRespostas = gson.toJson(respostas)

			// 2. Cria a entidade de persistência
			val vistoriaPendente = VistoriaPendente(
				demandaId = demandaId,
				jsonRespostas = jsonRespostas
			)

			// 3. Salva no banco local e marca demanda como "Aguardando Sincronização"
			vistoriaDao.adicionarFila(vistoriaPendente)
			demandaDao.updateStatus(demandaId, "Concluída")

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
			.build()

		workManager.enqueue(syncRequest)
	}
}