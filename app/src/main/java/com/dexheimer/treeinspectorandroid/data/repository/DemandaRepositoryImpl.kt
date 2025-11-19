package com.dexheimer.treeinspectorandroid.data.repository

import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.dexheimer.treeinspectorandroid.data.toDomain
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DemandaRepositoryImpl @Inject constructor(
	private val api: ApiService,
	private val dao: DemandaDao
) : DemandaRepository {

	override suspend fun getDemandasDaRota(rotaId: Int): List<Demanda> = withContext(Dispatchers.IO) {
		dao.getDemandasDaRota(rotaId).map { it.toDomain() }
	}

	override suspend fun atualizarStatus(demandaId: Int, novoStatus: String) = withContext(Dispatchers.IO) {
		dao.updateStatus(demandaId, novoStatus)
	}

	override suspend fun enviarVistoria(demandaId: Int, respostas: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
		try {
			val request = VistoriaRequest(demandaId, respostas)
			val response = api.salvarVistoria(request)

			if (response.isSuccessful) {
				// Se sucesso, atualiza localmente para 'concluido'
				dao.updateStatus(demandaId, "concluido")
				Result.success(Unit)
			} else {
				// Se erro 401, etc.
				Result.failure(Exception("Falha no envio: ${response.code()}"))
			}
		} catch (e: Exception) {
			// Se sem internet, apenas lança erro (quem chamar decide se salva offline)
			Result.failure(e)
		}
	}
}