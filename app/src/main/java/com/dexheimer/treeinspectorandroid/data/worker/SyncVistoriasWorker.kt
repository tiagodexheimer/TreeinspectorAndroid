package com.dexheimer.treeinspectorandroid.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.AppDatabase
import com.dexheimer.treeinspectorandroid.data.remote.NetworkClient
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncVistoriasWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

	private val TAG = "SyncWorker"

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		// 1. Restaurar a Sessão (Token)
		// O Worker roda em um processo limpo, então precisamos pegar o token salvo no login
		// e injetar manualmente no NetworkClient para que a API aceite a requisição.
		val sessionManager = SessionManager(applicationContext)
		val token = sessionManager.getAuthToken()

		if (!token.isNullOrEmpty()) {
			NetworkClient.authToken = token
			Log.d(TAG, "Token de sessão restaurado para sincronização em background.")
		} else {
			Log.e(TAG, "Abortando sincronização: Nenhum token de sessão encontrado.")
			// Retorna failure para não ficar tentando infinitamente sem login
			return@withContext Result.failure()
		}

		// 2. Buscar Vistorias Pendentes no Banco Local
		val db = AppDatabase.Companion.getInstance(applicationContext)
		val pendentes = db.vistoriaDao().getTodasPendentes()

		if (pendentes.isEmpty()) {
			Log.d(TAG, "Nenhuma vistoria pendente para sincronizar.")
			return@withContext Result.success()
		}

		Log.i(TAG, "Iniciando sincronização de ${pendentes.size} vistorias...")

		var todasSincronizadas = true
		val gson = Gson()

		// 3. Iterar e Enviar
		for (vistoria in pendentes) {
			try {
				Log.d(TAG, "Enviando vistoria da demanda #${vistoria.demandaId}...")

				// Converte o JSON string de volta para Map
				val type = object : TypeToken<Map<String, Any>>() {}.type
				val respostasMap: Map<String, Any> = gson.fromJson(vistoria.jsonRespostas, type)

				// Monta a requisição
				val request = VistoriaRequest(
					demandaId = vistoria.demandaId,
					respostas = respostasMap
				)

				// Chamada de Rede Síncrona (bloqueante) dentro da Coroutine
				val response = NetworkClient.api.salvarVistoria(request)

				if (response.isSuccessful) {
					Log.i(TAG, "Vistoria #${vistoria.demandaId} sincronizada com SUCESSO!")

					// a) Remove da fila de pendentes
					db.vistoriaDao().removerDaFila(vistoria)

					// b) Atualiza o status definitivo da demanda localmente
					// Isso faz o pino mudar de cor na próxima vez que a tela abrir
					db.demandaDao().updateStatus(vistoria.demandaId, "concluido")

				} else {
					Log.e(TAG, "Erro ao enviar #${vistoria.demandaId}: Código ${response.code()}")
					// Se for erro 401 (Token expirado), não adianta tentar de novo agora
					if (response.code() == 401) {
						todasSincronizadas = false
						break
					}
					todasSincronizadas = false
				}

			} catch (e: Exception) {
				Log.e(TAG, "Exceção ao sincronizar #${vistoria.demandaId}", e)
				todasSincronizadas = false
			}
		}

		// 4. Resultado do Trabalho
		if (todasSincronizadas) {
			Log.i(TAG, "Sincronização completa com sucesso.")
			Result.success()
		} else {
			Log.w(TAG, "Algumas vistorias falharam. Agendando nova tentativa (Retry).")
			Result.retry() // O WorkManager tentará novamente mais tarde (com backoff exponencial)
		}
	}
}