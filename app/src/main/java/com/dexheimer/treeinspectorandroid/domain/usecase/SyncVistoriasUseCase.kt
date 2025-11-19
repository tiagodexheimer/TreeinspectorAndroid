package com.dexheimer.treeinspectorandroid.domain.usecase

import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class SyncVistoriasUseCase @Inject constructor(
	private val vistoriaDao: VistoriaDao,
	private val demandaDao: DemandaDao,
	private val apiService: ApiService,
	private val sessionManager: SessionManager // Ainda necessário para pegar o token aqui
) {
	/**
	 * Executa a sincronização de todas as vistorias pendentes.
	 * @return Result<Boolean> true se todas sincronizaram, false se houver falhas.
	 */
	suspend operator fun invoke(): Result<Boolean> {

		// O token é recuperado aqui e, idealmente, seria injetado via OkHttp
		val token = sessionManager.getAuthToken()
		if (token.isNullOrEmpty()) {
			return Result.failure(Exception("Token de sessão ausente."))
		}

		val pendentes = vistoriaDao.getTodasPendentes()
		if (pendentes.isEmpty()) return Result.success(true)

		var todasSincronizadas = true
		val gson = Gson()
		val type = object : TypeToken<Map<String, Any>>() {}.type

		for (vistoria in pendentes) {
			try {
				val respostasMap: Map<String, Any> = gson.fromJson(vistoria.jsonRespostas, type)
				val request = VistoriaRequest(vistoria.demandaId, respostasMap)

				// Chamada de Rede
				val response = apiService.salvarVistoria(request)

				if (response.isSuccessful) {
					vistoriaDao.removerDaFila(vistoria)
					demandaDao.updateStatus(vistoria.demandaId, "concluido")
				} else if (response.code() == 401) {
					// Sessão expirada, para de tentar
					todasSincronizadas = false
					break
				} else {
					todasSincronizadas = false
				}

			} catch (e: Exception) {
				todasSincronizadas = false
			}
		}

		return Result.success(todasSincronizadas)
	}
}