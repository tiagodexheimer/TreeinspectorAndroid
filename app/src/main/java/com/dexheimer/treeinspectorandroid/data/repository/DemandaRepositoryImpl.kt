package com.dexheimer.treeinspectorandroid.data.repository

import android.util.Log
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

	// [CORREÇÃO] Removido o '=' para evitar retorno implícito de Int (do Log)
	override suspend fun atualizarStatus(demandaId: Int, novoStatus: String) {
		withContext(Dispatchers.IO) {
			// 1. Atualização Local
			dao.updateStatus(demandaId, novoStatus)

			// 2. Atualização Remota
			try {
				val body = mapOf("status" to novoStatus)
				val response = api.updateStatus(demandaId, body)

				if (response.isSuccessful) {
					Log.d("DemandaRepo", "Status atualizado: $novoStatus")
				} else {
					Log.w("DemandaRepo", "Falha status remoto: ${response.code()}")
				}
			} catch (e: Exception) {
				Log.e("DemandaRepo", "Sem conexão: ${e.message}")
			}
		}
	}

	override suspend fun enviarVistoria(demandaId: Int, respostas: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
		try {
			val request = VistoriaRequest(demandaId, respostas)
			val response = api.salvarVistoria(request)

			if (response.isSuccessful) {
				dao.updateStatus(demandaId, "concluido")
				Result.success(Unit)
			} else {
				Result.failure(Exception("Falha no envio: ${response.code()}"))
			}
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}