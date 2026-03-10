package com.dexheimer.treeinspectorandroid.presentation.login

import com.dexheimer.treeinspectorandroid.domain.usecase.LoginUseCase
import com.dexheimer.treeinspectorandroid.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase = mockk<LoginUseCase>()
    private lateinit var viewModel: LoginViewModel

    @Test
    fun `deve iniciar com estado inicial correto`() {
        viewModel = LoginViewModel(loginUseCase)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertNull(state.error)
    }

    @Test
    fun `deve atualizar estado para logado quando loginUseCase retorna sucesso`() = runTest {
        // Arrange
        coEvery { loginUseCase(any(), any()) } returns Result.success(Unit)
        viewModel = LoginViewModel(loginUseCase)

        // Act
        viewModel.login("teste@exemplo.com", "123456")

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isLoggedIn)
        assertNull(state.error)
    }

    @Test
    fun `deve atualizar estado com erro quando loginUseCase retorna falha`() = runTest {
        // Arrange
        val errorMsg = "Credenciais inválidas"
        coEvery { loginUseCase(any(), any()) } returns Result.failure(Exception(errorMsg))
        viewModel = LoginViewModel(loginUseCase)

        // Act
        viewModel.login("teste@exemplo.com", "errado")

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertEquals(errorMsg, state.error)
    }
}
