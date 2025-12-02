package com.dexheimer.treeinspectorandroid.data.repository

import android.util.Log
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.FormularioDao
import com.dexheimer.treeinspectorandroid.data.local.RotaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.toDomain
import com.dexheimer.treeinspectorandroid.data.toEntity
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RotaRepositoryImpl @Inject constructor(
	private val api: ApiService,
	private val rotaDao: RotaDao,
	private val demandaDao: DemandaDao,
	private val formularioDao: FormularioDao // <--- INJETADO: Para salvar formulários offline
) : RotaRepository {

	override suspend fun getRotas(): Result<List<Rota>> = withContext(Dispatchers.IO) {
		val local = rotaDao.getAllRotas()
		if (local.isNotEmpty()) {
			Result.success(local.map { it.toDomain() })
		} else {
			try {
				val response = api.getRotas()
				if (response.isSuccessful && response.body() != null) {
					val entities = response.body()!!
					rotaDao.insertAll(entities)
					Result.success(entities.map { it.toDomain() })
				} else {
					Result.failure(Exception("Erro API: ${response.code()}"))
				}
			} catch (e: Exception) {
				Result.failure(e)
			}
		}
	}

	override suspend fun getRotaDetalhes(rotaId: Int): Result<Pair<Rota, List<Demanda>>> {
		return withContext(Dispatchers.IO) {

			// PASSO 1: Carregar o status local atual ANTES de fazer qualquer chamada à API
			val demandasLocais = demandaDao.getDemandasDaRota(rotaId)
			val statusLocalMap = demandasLocais.associate { it.id to it.statusVistoria }

			try {
				val response = api.getRotaDetalhes(rotaId)
				if (response.isSuccessful && response.body() != null) {
					val data = response.body()!!

					// PASSO 2: Mesclar o status local com a lista da API
					val demandasEntities = data.demandas.map { dto ->
						val entity = dto.toEntity(rotaId)
						val localStatus = statusLocalMap[entity.id]
						if (localStatus != null) {
							if (localStatus.startsWith("concluido", ignoreCase = true)) {
								entity.copy(statusVistoria = localStatus) // Preserva o status local
							} else {
								entity // Senão, usa o status padrão/API (que deve ser "pendente")
							}
						} else {
							entity
						}
					}

					// PASSO 3: Inserir (Com OnConflictStrategy.REPLACE no DAO, isso atualiza o registro)
					rotaDao.insertRota(data.rota)
					demandaDao.insertAll(demandasEntities)

					Result.success(Pair(data.rota.toDomain(), demandasEntities.map { it.toDomain() }))
				} else {
					// Lógica de fallback local
					buscarLocalmente(rotaId)
				}
			} catch (e: Exception) {
				buscarLocalmente(rotaId)
			}
		}
	}

	private suspend fun buscarLocalmente(rotaId: Int): Result<Pair<Rota, List<Demanda>>> {
		val rota = rotaDao.getRotaById(rotaId)
		val demandas = demandaDao.getDemandasDaRota(rotaId)
		return if (rota != null) {
			Result.success(Pair(rota.toDomain(), demandas.map { it.toDomain() }))
		} else {
			Result.failure(Exception("Rota não encontrada."))
		}
	}

	// --- CORREÇÃO DE EXCLUSÃO DE ROTAS ---
	override suspend fun sincronizarRotas(): Result<Unit> = withContext(Dispatchers.IO) {
		// Use runCatching para garantir que até erros graves como OOM sejam tratados
		return@withContext runCatching {
			Log.d("Sync", "Iniciando Sincronização Completa...")

			// 1. Baixar Lista de Rotas
			val rotasResponse = api.getRotas()
			if (!rotasResponse.isSuccessful || rotasResponse.body() == null) {
				throw Exception("Falha ao baixar rotas: ${rotasResponse.code()}")
			}
			val rotas = rotasResponse.body()!!

			// 1.1 Coleta IDs do servidor
			val currentIds = rotas.map { it.id }

			// CORREÇÃO ROTAS: Mantemos o cache e apenas atualizamos com REPLACE
			rotaDao.insertAll(rotas)

			// CORREÇÃO EXCLUSÃO: Remove rotas locais que não estão mais no servidor.
			rotaDao.deleteRotasExcluidas(currentIds)

			val tiposDeDemandaParaBaixar = mutableSetOf<String>()

			// 2. Para CADA Rota, baixar os detalhes (demandas)
			for (rota in rotas) {
				// PASSO A: Carregar o status local antes de baixar o detalhe
				val demandasLocais = demandaDao.getDemandasDaRota(rota.id)
				val statusLocalMap = demandasLocais.associate { it.id to it.statusVistoria }

				val detalheResponse = api.getRotaDetalhes(rota.id)
				if (detalheResponse.isSuccessful && detalheResponse.body() != null) {
					val dados = detalheResponse.body()!!

					// PASSO B: Mesclar o status local com a lista da API
					val demandasEntities = dados.demandas.map { dto ->
						val entity = dto.toEntity(rota.id)
						val localStatus = statusLocalMap[entity.id]
						if (localStatus != null) {
							// Se a demanda foi concluída localmente, mantemos o status local
							if (localStatus.startsWith("concluido", ignoreCase = true)) {
								entity.copy(statusVistoria = localStatus)
							} else {
								entity
							}
						} else {
							entity
						}
					}

					// Salva demandas no banco (Com REPLACE, após o merge)
					demandaDao.insertAll(demandasEntities)

					// Coleta os tipos de demanda para baixar formulários depois
					dados.demandas.forEach { dto ->
						dto.tipoDemanda?.let { tiposDeDemandaParaBaixar.add(it) }
					}
				}
			}

			// 3. Baixar Formulários (PRE-FETCH PARA OFFLINE)
			val gson = Gson()
			for (tipo in tiposDeDemandaParaBaixar) {
				try {
					val formResponse = api.getFormularioPorTipo(tipo)
					if (formResponse.isSuccessful && formResponse.body() != null) {
						val campos = formResponse.body()!!
						formularioDao.insertOrUpdate(tipo, gson.toJson(campos))
						Log.d("Sync", "Formulário para '$tipo' baixado com sucesso.")
					}
				} catch (e: Exception) {
					Log.e("Sync", "Erro ao baixar formulário de $tipo", e)
				}
			}

			Log.d("Sync", "Sincronização concluída. Retornando sucesso.")
			Unit // Retorna Unit como sucesso
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { e ->
				Log.e("Sync", "ERRO FATAL DURANTE A SINCRONIZAÇÃO. Motivo: ${e.message}", e)
				// Converte Throwable em uma Exception amigável
				Result.failure(Exception("Erro fatal na sincronização: ${e.message ?: "Desconhecido"}"))
			}
		)
	}
}