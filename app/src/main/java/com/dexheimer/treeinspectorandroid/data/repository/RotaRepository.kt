package com.dexheimer.treeinspectorandroid.domain.repository

import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota

interface RotaRepository {
	// Apenas devolvemos listas de objetos de DOMÍNIO ou Result (sucesso/erro)
	suspend fun getRotas(): Result<List<Rota>>
	suspend fun getRotaDetalhes(rotaId: Int): Result<Pair<Rota, List<Demanda>>>
	suspend fun sincronizarRotas(): Result<Unit>
}