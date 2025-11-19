package com.dexheimer.treeinspectorandroid.data.repository

import android.util.Log
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.RotaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.toDomain
import com.dexheimer.treeinspectorandroid.data.toEntity
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RotaRepositoryImpl @Inject constructor(
	private val api: ApiService,
	private val rotaDao: RotaDao,
	private val demandaDao: DemandaDao
) : RotaRepository {

	override suspend fun getRotas(): Result<List<Rota>> = withContext(Dispatchers.IO) {
		try {
			// 1. Tenta API
			val response = api.getRotas()
			if (response.isSuccessful && response.body() != null) {
				val entities = response.body()!!
				Result.success(entities.map { it.toDomain() })
			} else {
				Result.failure(Exception("Erro API: ${response.code()}"))
			}
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override suspend fun getRotaDetalhes(rotaId: Int): Result<Pair<Rota, List<Demanda>>> = withContext(Dispatchers.IO) {
		try {
			val response = api.getRotaDetalhes(rotaId)

			if (response.isSuccessful && response.body() != null) {
				// LÓGICA DE SUCESSO RESTAURADA:
				val data = response.body()!! // <--- Variável 'data' declarada aqui

				// 2. Salva no Banco Local (Cache)
				rotaDao.insertRota(data.rota)

				demandaDao.clearDemandasDaRota(rotaId)
				val demandasEntities = data.demandas.map { it.toEntity(rotaId) } // <--- Variável 'demandasEntities' declarada aqui
				demandaDao.insertAll(demandasEntities)

				// 3. Retorna os dados convertidos para Domínio
				Result.success(Pair(data.rota.toDomain(), demandasEntities.map { it.toDomain() }))
			} else {
				// TRATAMENTO DE ERRO APRIMORADO:
				val erro = when (response.code()) {
					401 -> "Sessão Expirada. Faça login novamente."
					404 -> "Rota não encontrada na API."
					else -> "Erro na API: ${response.code()}"
				}

				Log.w("RotaRepo", "Falha na API ($rotaId): $erro")
				return@withContext buscarLocalmente(rotaId) // <--- Adicionado 'return@withContext' para garantir o retorno
			}
		} catch (e: Exception) {
			Log.e("RotaRepo", "Erro de Conexão ou Parse. Buscando localmente...", e)
			return@withContext buscarLocalmente(rotaId)
		}
	}

	private suspend fun buscarLocalmente(rotaId: Int): Result<Pair<Rota, List<Demanda>>> {
		val rota = rotaDao.getRotaById(rotaId)
		val demandas = demandaDao.getDemandasDaRota(rotaId)

		return if (rota != null) {
			Result.success(Pair(rota.toDomain(), demandas.map { it.toDomain() }))
		} else {
			Result.failure(Exception("Rota não encontrada (nem local, nem remota)."))
		}
	}

	override suspend fun sincronizarRotas(): Result<Unit> {
		return Result.success(Unit)
	}
}