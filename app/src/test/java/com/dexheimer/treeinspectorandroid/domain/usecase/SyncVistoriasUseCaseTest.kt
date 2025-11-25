import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.domain.usecase.SyncVistoriasUseCase
import org.junit.Before
import org.junit.Test

class SyncVistoriasUseCaseTest {

	// Mocks das dependências
	private val vistoriaDao = mockk<VistoriaDao>(relaxed = true)
	private val apiService = mockk<ApiService>()
	private val sessionManager = mockk<SessionManager>()

	// Classe sob teste
	private lateinit var useCase: SyncVistoriasUseCase

	@Before
	fun setup() {
		useCase = SyncVistoriasUseCase(vistoriaDao, mockk(), apiService, sessionManager)
	}

	@Test
	fun `deve retornar erro quando token de sessao for invalido`() = runTest {
		// Arrange (Preparar)
		every { sessionManager.getCookieString() } returns "" // Simula token vazio

		// Act (Agir)
		val result = useCase()

		// Assert (Verificar)
		assertTrue(result.isFailure)
		assertEquals("Sessão inválida. Faça login novamente.", result.exceptionOrNull()?.message)
	}
}