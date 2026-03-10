package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.DemandaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.local.VistoriaPendente
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

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
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        useCase = SyncVistoriasUseCase(vistoriaDao, demandaDao, apiService, sessionManager)
    }

    @Test
    fun `deve retornar erro quando token de sessao for invalido`() = runTest {
        // Arrange
        every { sessionManager.getCookieString() } returns ""

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertEquals("Sessão inválida", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deve retornar sucesso quando nao houver vistorias pendentes`() = runTest {
        // Arrange
        every { sessionManager.getCookieString() } returns "mock_token"
        coEvery { vistoriaDao.getTodasPendentes() } returns emptyList()

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        coVerify(exactly = 0) { apiService.salvarVistoria(any()) }
    }

    @Test
    fun `deve sincronizar com sucesso uma vistoria sem imagens`() = runTest {
        // Arrange
        every { sessionManager.getCookieString() } returns "mock_token"
        val jsonData = "{\"pergunta1\":\"resposta1\"}"
        val vistoria = VistoriaPendente(id = 1, demandaId = 100, jsonRespostas = jsonData)

        coEvery { vistoriaDao.getTodasPendentes() } returns listOf(vistoria)
        coEvery { apiService.salvarVistoria(any()) } returns Response.success(null)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        coVerify { apiService.salvarVistoria(match { it.demandaId == 100 }) }
        coVerify { vistoriaDao.marcarComoSincronizada(1) }
        coVerify { demandaDao.updateStatus(100, "concluido") }
    }

    @Test
    fun `deve retornar falha se o salvamento inicial no servidor falhar`() = runTest {
        // Arrange
        every { sessionManager.getCookieString() } returns "mock_token"
        val vistoria = VistoriaPendente(id = 1, demandaId = 100, jsonRespostas = "{}")

        coEvery { vistoriaDao.getTodasPendentes() } returns listOf(vistoria)
        coEvery { apiService.salvarVistoria(any()) } returns
                Response.error(500, mockk(relaxed = true))

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess) // O UseCase retorna Result.success(todasSincronizadas)
        assertEquals(false, result.getOrNull())
        coVerify(exactly = 0) { vistoriaDao.marcarComoSincronizada(any()) }
    }
}
