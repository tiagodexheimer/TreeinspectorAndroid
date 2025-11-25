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

	// ... (getRotas e getRotaDetalhes mantém a lógica existente) ...

	override suspend fun getRotas(): Result<List<Rota>> = withContext(Dispatchers.IO) {
		// Mantém implementação atual de cache-first ou network-first
		// Recomendo: Tenta local primeiro para ser rápido, a sincronização forçada via swipe atualiza tudo.
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
		// ... (Sua implementação atual)
		// OBS: Se não tiver local, tenta API.
		return withContext(Dispatchers.IO) {
			try {
				val response = api.getRotaDetalhes(rotaId)
				if (response.isSuccessful && response.body() != null) {
					val data = response.body()!!
					rotaDao.insertRota(data.rota)
					demandaDao.clearDemandasDaRota(rotaId)
					val demandasEntities = data.demandas.map { it.toEntity(rotaId) }
					demandaDao.insertAll(demandasEntities)
					Result.success(Pair(data.rota.toDomain(), demandasEntities.map { it.toDomain() }))
				} else {
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

	// --- AQUI ESTÁ A SOLUÇÃO CRÔNICA ---
	override suspend fun sincronizarRotas(): Result<Unit> = withContext(Dispatchers.IO) {
		try {
			Log.d("Sync", "Iniciando Sincronização Completa...")

			// 1. Baixar Lista de Rotas
			val rotasResponse = api.getRotas()
			if (!rotasResponse.isSuccessful || rotasResponse.body() == null) {
				return@withContext Result.failure(Exception("Falha ao baixar rotas"))
			}
			val rotas = rotasResponse.body()!!

			// Atualiza rotas no banco (limpa antigas se necessário ou usa Insert OnConflict)
			rotaDao.deleteAll()
			rotaDao.insertAll(rotas)

			val tiposDeDemandaParaBaixar = mutableSetOf<String>()

			// 2. Para CADA Rota, baixar os detalhes (demandas)
			for (rota in rotas) {
				val detalheResponse = api.getRotaDetalhes(rota.id)
				if (detalheResponse.isSuccessful && detalheResponse.body() != null) {
					val dados = detalheResponse.body()!!

					// Salva demandas no banco
					val demandasEntities = dados.demandas.map { it.toEntity(rota.id) }
					demandaDao.clearDemandasDaRota(rota.id) // Limpa versão antiga
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
					// Verifica se já temos o formulário atualizado (opcional, mas bom pra performance)
					// Se não, baixa da API
					val formResponse = api.getFormularioPorTipo(tipo)
					if (formResponse.isSuccessful && formResponse.body() != null) {
						val campos = formResponse.body()!!

						// Salva no FormularioDao (Você precisará garantir que tem um método insert ou update)
						// Assumindo que você tem uma entidade FormularioEntity ou similar
						// Aqui uso o Cache que vi nos seus arquivos, ou DAO direto

						// Exemplo de salvamento (Adapte para sua Entidade de Banco):
						formularioDao.insertOrUpdate(tipo, gson.toJson(campos))

						Log.d("Sync", "Formulário para '$tipo' baixado com sucesso.")
					}
				} catch (e: Exception) {
					Log.e("Sync", "Erro ao baixar formulário de $tipo", e)
					// Não abortamos tudo se um formulário falhar, tentamos os outros
				}
			}

			Result.success(Unit)
		} catch (e: Exception) {
			Log.e("Sync", "Erro fatal na sincronização", e)
			Result.failure(e)
		}
	}
}