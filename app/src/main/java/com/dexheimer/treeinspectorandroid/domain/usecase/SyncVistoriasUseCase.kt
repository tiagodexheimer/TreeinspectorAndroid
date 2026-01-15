package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class SyncVistoriasUseCase @Inject constructor(
	private val vistoriaDao: VistoriaDao,
	private val demandaDao: DemandaDao,
	private val apiService: ApiService,
	private val sessionManager: SessionManager
) {

	suspend operator fun invoke(): Result<Boolean> {
		val token = sessionManager.getCookieString()
		if (token.isNullOrEmpty()) {
			return Result.failure(Exception("Sessão inválida"))
		}

		val pendentes = vistoriaDao.getTodasPendentes()
		if (pendentes.isEmpty()) return Result.success(true)

		var todasSincronizadas = true
		val gson = Gson()
		// Usamos MutableMap para poder substituir o valor (Path -> URL)
		val type = object : TypeToken<MutableMap<String, Any>>() {}.type

		for (vistoria in pendentes) {
			try {
				Log.d("Sync", "Iniciando sincronização da demanda ${vistoria.demandaId}")

				// 1. Carrega o JSON salvo localmente
				val respostasMap: MutableMap<String, Any> = gson.fromJson(vistoria.jsonRespostas, type)

				// 2. PROCESSAMENTO DE IMAGENS (UPLOAD)
				// Varre o mapa, sobe as fotos e atualiza o mapa com as URLs
				val uploadsSucesso = processarUploadsRecursivamente(respostasMap)

				if (!uploadsSucesso) {
					Log.e("Sync", "Falha no upload de imagens da demanda ${vistoria.demandaId}. Tentaremos depois.")
					todasSincronizadas = false
					continue
				}

				// 3. Envia o JSON final (agora com URLs da Vercel)
				val request = VistoriaRequest(vistoria.demandaId, respostasMap)
				val response = apiService.salvarVistoria(request)

				if (response.isSuccessful) {
					// EM VEZ DE REMOVER, MARCA COMO SINCRONIZADA
					vistoriaDao.marcarComoSincronizada(vistoria.id)
					
					demandaDao.updateStatus(vistoria.demandaId, "concluido")
					Log.i("Sync", "Sucesso: Demanda ${vistoria.demandaId} sincronizada.")

					// Opcional: Limpar arquivos locais de imagem aqui para liberar espaço
				} else {
					val code = response.code()
					Log.e("Sync", "Erro API ao salvar vistoria: $code")

					if (code == 404 || code == 400) {
						// Demanda não existe mais, remove da fila pra não travar
						vistoriaDao.removerDaFila(vistoria)
					} else {
						todasSincronizadas = false
					}
				}

			} catch (e: Exception) {
				Log.e("Sync", "Exceção na sincronização", e)
				todasSincronizadas = false
			}
		}

		return Result.success(todasSincronizadas)
	}

	/**
	 * Função recursiva que varre o mapa de respostas.
	 * Se encontrar uma String que parece caminho de arquivo local, faz upload.
	 * Se encontrar uma Lista de Strings, faz upload de cada item.
	 */
	private suspend fun processarUploadsRecursivamente(map: MutableMap<String, Any>): Boolean {
		for ((key, value) in map) {

			// CASO 1: Campo de foto única (String)
			if (value is String) {
				if (isLocalFilePath(value)) {
					val url = uploadToVercel(value)
					if (url != null) {
						map[key] = url // Substitui o Path pela URL
					} else {
						return false // Falha no upload, aborta sync
					}
				}
			}
			// CASO 2: Campo de múltiplas fotos (List<String>)
			else if (value is ArrayList<*>) {
				val listaAtualizada = mutableListOf<String>()

				@Suppress("UNCHECKED_CAST")
				val listaOriginal = value as? List<String>

				if (listaOriginal != null) {
					for (item in listaOriginal) {
						if (isLocalFilePath(item)) {
							val url = uploadToVercel(item)
							if (url != null) {
								listaAtualizada.add(url)
							} else {
								return false // Falha em um upload da lista
							}
						} else {
							listaAtualizada.add(item) // Já era URL ou texto
						}
					}
					map[key] = listaAtualizada // Substitui a lista inteira
				}
			}
		}
		return true
	}

	private fun isLocalFilePath(path: String): Boolean {
		// Verifica se é um caminho absoluto interno do Android e tem extensão de imagem
		return path.startsWith("/") &&
				(path.contains("/storage/") || path.contains("/data/")) &&
				(path.endsWith(".jpg", true) || path.endsWith(".png", true) || path.endsWith(".jpeg", true))
	}

	private suspend fun uploadToVercel(filePath: String): String? {
		val file = File(filePath)
		if (!file.exists()) {
			Log.w("Upload", "Arquivo não encontrado: $filePath")
			return null
		}

		return try {
			Log.d("Upload", "Iniciando upload: ${file.name}")

			val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
			val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

			// Usa a rota criada no Passo 2
			val response = apiService.uploadImage(body, file.name)

			if (response.isSuccessful && response.body() != null) {
				val url = response.body()!!.url
				Log.d("Upload", "Upload concluído: $url")
				url
			} else {
				Log.e("Upload", "Erro no upload: ${response.code()}")
				null
			}
		} catch (e: Exception) {
			Log.e("Upload", "Falha de rede no upload", e)
			null
		}
	}
}