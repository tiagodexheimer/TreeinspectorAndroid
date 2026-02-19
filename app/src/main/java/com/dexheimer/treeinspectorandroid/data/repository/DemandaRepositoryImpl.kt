package com.dexheimer.treeinspectorandroid.data.repository

import android.util.Log
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.CreateDemandaRequest
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.dexheimer.treeinspectorandroid.data.toDomain
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class DemandaRepositoryImpl
@Inject
constructor(private val api: ApiService, private val dao: DemandaDao) : DemandaRepository {

    override suspend fun getDemandasDaRota(rotaId: Int): List<Demanda> =
            withContext(Dispatchers.IO) { dao.getDemandasDaRota(rotaId).map { it.toDomain() } }

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

    override suspend fun enviarVistoria(demandaId: Int, respostas: Map<String, Any>): Result<Unit> =
            withContext(Dispatchers.IO) {
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

    override suspend fun criarDemanda(
            params: com.dexheimer.treeinspectorandroid.domain.model.CreateDemandaParams
    ): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val request =
                            CreateDemandaRequest(
                                    nome_solicitante = params.nome_solicitante,
                                    telefone_solicitante = params.telefone_solicitante,
                                    email_solicitante = params.email_solicitante,
                                    cep = params.cep,
                                    logradouro = params.logradouro,
                                    numero = params.numero,
                                    complemento = params.complemento,
                                    bairro = params.bairro,
                                    cidade = params.cidade,
                                    uf = params.uf,
                                    tipo_demanda = params.tipo_demanda,
                                    descricao = params.descricao,
                                    coordinates = params.coordinates,
                                    anexos = params.anexos
                            )
                    val response = api.criarDemanda(request)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Falha ao criar demanda: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    override suspend fun uploadImage(filePath: String): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    if (!file.exists())
                            return@withContext Result.failure(Exception("Arquivo não encontrado"))

                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    val response = api.uploadImage(body, file.name)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!.url)
                    } else {
                        Result.failure(Exception("Falha no upload: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    override suspend fun getTiposDemanda():
            Result<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>> =
            withContext(Dispatchers.IO) {
                try {
                    val response = api.getDemandTypes()
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(
                                Exception("Falha ao buscar tipos de demanda: ${response.code()}")
                        )
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
