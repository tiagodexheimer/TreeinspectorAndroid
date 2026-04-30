package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaPendente
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import android.graphics.Bitmap

class SyncVistoriasUseCase
@Inject
constructor(
        @ApplicationContext private val context: Context,
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
        val type = object : TypeToken<MutableMap<String, Any>>() {}.type

        for (vistoria in pendentes) {
            try {
                Log.d("Sync", "Iniciando sincronização da demanda ${vistoria.demandaId}")

                // 1. Carrega o JSON
                val respostasMap: MutableMap<String, Any> =
                        gson.fromJson(vistoria.jsonRespostas, type)

                // 2. SALVAMENTO INICIAL (TEXTO)
                // Garante que os dados textuais já vão pro servidor, mesmo com caminhos locais de
                // imagem.
                if (salvarNoServidor(vistoria.demandaId, respostasMap)) {
                    Log.d(
                            "Sync",
                            "Salvamento inicial (texto) concluído para demanda ${vistoria.demandaId}"
                    )
                } else {
                    Log.e(
                            "Sync",
                            "Falha no salvamento inicial da demanda ${vistoria.demandaId}. Abortando."
                    )
                    todasSincronizadas = false
                    continue
                }

                // 3. UPLOAD INCREMENTAL
                // Sobe foto e salva o estado imediatamente
                val uploadsSucesso = processarUploadsIncremental(vistoria, respostasMap, gson)

                if (!uploadsSucesso) {
                    Log.e(
                            "Sync",
                            "Falha parcial no upload de imagens da demanda ${vistoria.demandaId}."
                    )
                    todasSincronizadas = false
                    continue
                }

                // 4. Se chegou aqui, tudo foi enviado. Marca como CONCLUÍDO.
                vistoriaDao.marcarComoSincronizada(vistoria.id)
                demandaDao.updateStatus(vistoria.demandaId, "Concluído")
                Log.i("Sync", "Sucesso: Demanda ${vistoria.demandaId} totalmente sincronizada.")

                // Opcional: Limpar arquivos locais

            } catch (e: Exception) {
                Log.e("Sync", "Exceção na sincronização da demanda ${vistoria.demandaId}", e)
                todasSincronizadas = false
            }
        }

        return Result.success(todasSincronizadas)
    }

    /**
     * Varre o mapa e para cada imagem local:
     * 1. Faz upload
     * 2. Atualiza o mapa com a URL
     * 3. Salva no Servidor (salvarVistoria)
     * 4. Salva Local (atualizarVistoria)
     */
    private suspend fun processarUploadsIncremental(
            vistoria: VistoriaPendente,
            map: MutableMap<String, Any>,
            gson: Gson
    ): Boolean {
        var algumErro = false

        for ((key, value) in map) {
            // CASO 1: Campo de foto única (String)
            if (value is String && isLocalFilePath(value)) {
                val processedPath = ensureWebP(value)
                if (processedPath != value) {
                    map[key] = processedPath
                    salvarEstadoIntermediario(vistoria, map, gson)
                }

                val url = uploadToVercel(processedPath)
                if (url != null) {
                    map[key] = url
                    salvarEstadoIntermediario(vistoria, map, gson)
                } else {
                    algumErro = true
                }
            }
            // CASO 2: Campo de múltiplas fotos (List)
            else if (value is ArrayList<*>) {
                @Suppress("UNCHECKED_CAST") val lista = value as? ArrayList<String>
                if (lista != null) {
                    for (i in lista.indices) {
                        val item = lista[i]
                        if (isLocalFilePath(item)) {
                            val processedPath = ensureWebP(item)
                            if (processedPath != item) {
                                lista[i] = processedPath
                                salvarEstadoIntermediario(vistoria, map, gson)
                            }

                            val url = uploadToVercel(processedPath)
                            if (url != null) {
                                lista[i] = url
                                salvarEstadoIntermediario(vistoria, map, gson)
                            } else {
                                algumErro = true
                            }
                        }
                    }
                }
            }
        }
        return !algumErro
    }

    private suspend fun ensureWebP(path: String): String {
        if (path.endsWith(".webp", true)) return path

        Log.w("Sync", "Fallback: Arquivo não é WebP. Iniciando compressão de emergência: $path")
        return try {
            val originalFile = File(path)
            if (!originalFile.exists()) return path

            val compressedFile = Compressor.compress(context, originalFile) {
                resolution(1600, 1600)
                quality(60)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    format(Bitmap.CompressFormat.WEBP_LOSSY)
                } else {
                    @Suppress("DEPRECATION")
                    format(Bitmap.CompressFormat.WEBP)
                }
            }

            val finalFile = File(originalFile.parent, originalFile.name.substringBeforeLast(".") + "_emergency.webp")
            compressedFile.copyTo(finalFile, overwrite = true)
            
            Log.i("Sync", "Compressão WebP: Original=${originalFile.length() / 1024}KB, Novo=${finalFile.length() / 1024}KB")
            
            // Se por algum erro bizarro o WebP ficar maior, e já for pequeno, poderíamos manter o original.
            // Mas para o servidor, padronizar em WebP é melhor.
            
            compressedFile.delete()
            
            if (originalFile.exists()) originalFile.delete()
            
            Log.i("Sync", "Compressão de emergência concluída: ${finalFile.absolutePath}")
            finalFile.absolutePath
        } catch (e: Exception) {
            Log.e("Sync", "Falha na compressão de emergência", e)
            path
        }
    }

    private suspend fun salvarEstadoIntermediario(
            vistoria: VistoriaPendente,
            map: Map<String, Any>,
            gson: Gson
    ) {
        try {
            // 1. Salva no Servidor
            salvarNoServidor(vistoria.demandaId, map)

            // 2. Salva Localmente (para retomar em caso de crash)
            val novoJson = gson.toJson(map)
            vistoriaDao.atualizarVistoria(
                    demandaId = vistoria.demandaId,
                    json = novoJson,
                    sincronizado = false, // Ainda não acabou
                    data = vistoria.dataCriacao
            )
        } catch (e: Exception) {
            Log.w(
                    "Sync",
                    "Erro ao salvar estado intermediário (prosseguindo com uploads): ${e.message}"
            )
        }
    }

    private suspend fun salvarNoServidor(demandaId: Int, map: Map<String, Any>): Boolean {
        return try {
            val request = VistoriaRequest(demandaId, map)
            val response = apiService.salvarVistoria(request)
            if (!response.isSuccessful) {
                Log.e("Sync", "Erro API salvarVistoria: ${response.code()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("Sync", "Erro de rede salvarVistoria", e)
            false
        }
    }

    private fun isLocalFilePath(path: String): Boolean {
        return path.startsWith("/") &&
                (path.contains("/storage/") || path.contains("/data/")) &&
                (path.endsWith(".jpg", true) ||
                        path.endsWith(".png", true) ||
                        path.endsWith(".jpeg", true) ||
                        path.endsWith(".webp", true))
    }

    private suspend fun uploadToVercel(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w("Upload", "Arquivo não encontrado: $filePath")
            return null
        }

        return try {
            Log.d("Upload", "Iniciando upload: ${file.name} (${file.length() / 1024} KB)")
            
            val mediaType = if (file.name.endsWith(".webp", true)) {
                "image/webp".toMediaTypeOrNull()
            } else {
                "image/jpeg".toMediaTypeOrNull()
            }

            val requestFile = file.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

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
