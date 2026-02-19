package com.dexheimer.treeinspectorandroid.domain.repository

import com.dexheimer.treeinspectorandroid.data.remote.CreateDemandaRequest
import com.dexheimer.treeinspectorandroid.domain.model.Demanda

interface DemandaRepository {
    suspend fun getDemandasDaRota(rotaId: Int): List<Demanda>
    suspend fun atualizarStatus(demandaId: Int, novoStatus: String)
    suspend fun enviarVistoria(demandaId: Int, respostas: Map<String, Any>): Result<Unit>
    suspend fun criarDemanda(request: CreateDemandaRequest): Result<Unit>
    suspend fun uploadImage(filePath: String): Result<String>
    suspend fun getTiposDemanda():
            Result<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>>
}
