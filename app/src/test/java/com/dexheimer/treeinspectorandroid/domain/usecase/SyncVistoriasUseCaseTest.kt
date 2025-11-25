package com.dexheimer.treeinspectorandroid.domain.usecase

import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncVistoriasUseCaseTest {

	// Mocks das dependências
	private val vistoriaDao = mockk<VistoriaDao>(relaxed = true)
	private val apiService = mockk<ApiService>()
	private val sessionManager = mockk<SessionManager>()
	private val demandaDao = mockk<DemandaDao>(relaxed = true)

	// Classe sob teste
	private lateinit var useCase: SyncVistoriasUseCase

	@Before
	fun setup() {
		useCase = SyncVistoriasUseCase(vistoriaDao, demandaDao, apiService, sessionManager)
	}

	@Test
	fun `deve retornar erro quando token de sessao for invalido`() = runTest {
		// Arrange (Preparar)
		// Simula que o getCookieString retorna string vazia
		every { sessionManager.getCookieString() } returns ""

		// Act (Agir)
		val result = useCase()

		// Assert (Verificar)
		assertTrue(result.isFailure)
		assertEquals("Sessão inválida. Faça login novamente.", result.exceptionOrNull()?.message)
	}
}