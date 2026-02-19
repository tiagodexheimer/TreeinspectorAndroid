package com.dexheimer.treeinspectorandroid.domain.repository

import com.dexheimer.treeinspectorandroid.domain.model.CreateDemandaParams
import com.dexheimer.treeinspectorandroid.domain.model.Demanda

interface DemandaRepository {
    suspend fun getDemandasDaRota(rotaId: Int): List<Demanda>
    suspend fun atualizarStatus(demandaId: Int, novoStatus: String)
    suspend fun enviarVistoria(demandaId: Int, respostas: Map<String, Any>): Result<Unit>
    suspend fun criarDemanda(params: CreateDemandaParams): Result<Unit>
    suspend fun uploadImage(filePath: String): Result<String>
    suspend fun getTiposDemanda():
            Result<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>>
}
