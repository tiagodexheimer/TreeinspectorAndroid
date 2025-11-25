package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
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
	private val sessionManager: SessionManager
) {
	suspend operator fun invoke(): Result<Boolean> {

		// CORREÇÃO: Usar 'getCookieString()' em vez de 'getAuthToken()'
		val token = sessionManager.getCookieString()

		if (token.isNullOrEmpty()) {
			return Result.failure(Exception("Sessão inválida. Faça login novamente."))
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

				val response = apiService.salvarVistoria(request)

				if (response.isSuccessful) {
					// Sucesso: Remove da fila
					vistoriaDao.removerDaFila(vistoria)
					demandaDao.updateStatus(vistoria.demandaId, "concluido")

				} else if (response.code() == 404 || response.code() == 400) {
					// --- CORREÇÃO AQUI ---
					// Erro 404: A demanda não existe mais no servidor.
					// Não adianta tentar de novo. Removemos da fila para destravar a sincronização.
					Log.w("Sync", "Demanda ${vistoria.demandaId} não existe mais. Removendo vistoria da fila.")

					vistoriaDao.removerDaFila(vistoria)

					// Opcional: Apagar a demanda local também para limpar
					// demandaDao.deleteById(vistoria.demandaId)

				} else if (response.code() == 401) {
					// Sessão expirada, para tudo
					todasSincronizadas = false
					break
				} else {
					// Outro erro (500, timeout): Mantém na fila para tentar depois
					todasSincronizadas = false
				}

			} catch (e: Exception) {
				todasSincronizadas = false
			}
		}

		return Result.success(todasSincronizadas)
	}
}