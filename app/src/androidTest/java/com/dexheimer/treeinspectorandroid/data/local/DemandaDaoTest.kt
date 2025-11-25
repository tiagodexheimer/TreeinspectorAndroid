package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class DemandaDaoTest {

	private lateinit var database: AppDatabase
	private lateinit var demandaDao: DemandaDao

	@Before
	fun createDb() {
		// Cria um banco em memória (in-memory)
		// Os dados são apagados quando o processo morre, ideal para testes.
		val context = ApplicationProvider.getApplicationContext<android.content.Context>()
		database = Room.inMemoryDatabaseBuilder(
			context,
			AppDatabase::class.java
		).allowMainThreadQueries().build() // Permitimos main thread apenas para teste simples

		demandaDao = database.demandaDao()
	}

	@After
	@Throws(IOException::class)
	fun closeDb() {
		database.close()
	}

	@Test
	fun insertAndGetDemanda() = runBlocking {
		// 1. Criar uma entidade de teste
		val demanda = DemandaEntity(
			id = 1,
			rotaId = 10,
			lat = -30.0,
			lng = -51.0,
			descricao = "Árvore caída",
			statusVistoria = "pendente",
			// Preencha outros campos obrigatórios com null ou valores dummy
			idStatus = 1, statusNome = "Aberto", statusCor = "#FF0000",
			protocolo = "PROTO-123", nomeSolicitante = "Teste",
			telefoneSolicitante = null, emailSolicitante = null, prazo = null,
			createdAt = "2023-01-01", updatedAt = "2023-01-01",
			cep = null, logradouro = "Rua Teste", numero = "100",
			complemento = null, bairro = null, cidade = null, uf = null,
			tipoDemanda = "Poda", geom = null
		)

		// 2. Inserir no banco
		demandaDao.insertAll(listOf(demanda))

		// 3. Buscar do banco
		val demandasDaRota = demandaDao.getDemandasDaRota(10)

		// 4. Validar
		assertTrue(demandasDaRota.isNotEmpty())
		assertEquals(1, demandasDaRota.size)
		assertEquals("Árvore caída", demandasDaRota[0].descricao)
	}

	@Test
	fun updateStatusDemanda() = runBlocking {
		// 1. Inserir demanda inicial
		val demanda = DemandaEntity(
			id = 2, rotaId = 20, lat = 0.0, lng = 0.0,
			descricao = "Buraco", statusVistoria = "pendente",
			idStatus = null, statusNome = null, statusCor = null,
			protocolo = null, nomeSolicitante = null, telefoneSolicitante = null,
			emailSolicitante = null, prazo = null, createdAt = null, updatedAt = null,
			cep = null, logradouro = null, numero = null, complemento = null,
			bairro = null, cidade = null, uf = null, tipoDemanda = null, geom = null
		)
		demandaDao.insertAll(listOf(demanda))

		// 2. Atualizar status
		demandaDao.updateStatus(2, "concluido")

		// 3. Verificar se atualizou
		val demandas = demandaDao.getDemandasDaRota(20)
		assertEquals("concluido", demandas[0].statusVistoria)
	}
}