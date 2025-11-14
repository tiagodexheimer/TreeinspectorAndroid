package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log // Importe o Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	// --- LOGGING TAG ---
	// Usaremos isso para filtrar os logs no Logcat
	private val TAG = "MainActivity"

	// Gerenciador de Sessão para "lembrar" do login
	private lateinit var sessionManager: SessionManager

	// Componentes da UI da tela de login
	private lateinit var editTextEmail: TextInputEditText
	private lateinit var editTextPassword: TextInputEditText
	private lateinit var buttonLogin: Button
	private lateinit var progressBar: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// 1. Inicializa o SessionManager
		sessionManager = SessionManager(applicationContext)

		// 2. Verifica se o usuário JÁ ESTÁ LOGADO
		if (sessionManager.isLoggedIn()) {
			Log.i(TAG, "Usuário já está logado. Pulando para o app.")
			navigateToApp() // Pula direto para a tela de rotas
			return // Impede que o resto do onCreate (tela de login) seja executado
		}

		// 3. Se não estiver logado, mostra a tela de login
		setContentView(R.layout.activity_main)
		Log.d(TAG, "Mostrando tela de login.")

		// 4. Referencia os views do layout
		editTextEmail = findViewById(R.id.editTextEmail)
		editTextPassword = findViewById(R.id.editTextPassword)
		buttonLogin = findViewById(R.id.buttonLogin)
		progressBar = findViewById(R.id.progressBar)

		// 5. Define o clique do botão de login
		buttonLogin.setOnClickListener {
			performLogin()
		}
	}

	/**
	 * Inicia o processo de login em 2 etapas:
	 * 1. Busca o token CSRF.
	 * 2. Envia as credenciais (email, senha) + token CSRF.
	 */
	private fun performLogin() {
		val email = editTextEmail.text.toString().trim()
		val password = editTextPassword.text.toString().trim()

		// --- FASE 0: VALIDAÇÃO LOCAL ---
		if (email.isEmpty() || password.isEmpty()) {
			Log.w(TAG, "Fase 0: FALHA - Email ou senha vazios.")
			Toast.makeText(this, "Email e senha são obrigatórios", Toast.LENGTH_SHORT).show()
			return
		}
		Log.d(TAG, "Fase 0: SUCESSO - Inputs validados.")

		showLoading(true)

		lifecycleScope.launch {
			try {
				// --- FASE 1: BUSCAR TOKEN CSRF ---
				Log.i(TAG, "Fase 1: Buscando token CSRF...")
				val csrfResponse = NetworkClient.api.getCsrfToken()

				if (!csrfResponse.isSuccessful || csrfResponse.body()?.csrfToken == null) {
					Log.e(TAG, "Fase 1: FALHA ao buscar token CSRF! Código: ${csrfResponse.code()}")
					throw Exception("Erro de segurança ao iniciar login (CSRF)")
				}

				val csrfToken = csrfResponse.body()!!.csrfToken
				Log.i(TAG, "Fase 1: SUCESSO - Token CSRF obtido.")

				// --- FASE 2: REALIZAR O LOGIN COM O TOKEN ---
				Log.i(TAG, "Fase 2: Enviando credenciais...")
				val request = LoginRequest(
					email = email,
					password = password,
					csrfToken = csrfToken
				)

				val loginResponse = NetworkClient.api.login(request)

				// --- INÍCIO DA CORREÇÃO ---

				// Se for 401 (Não autorizado), a senha está errada.
				if (loginResponse.code() == 401) {
					Log.w(TAG, "Fase 2: FALHA (401) - Email ou senha inválidos.")
					throw Exception("Email ou senha inválidos (401)")
				}

				// Se for 302 (Redirecionamento)
				if (loginResponse.code() == 302) {
					// Verificamos se é um 302 de SUCESSO (que envia o cookie de sessão)
					val cookies = loginResponse.headers().values("Set-Cookie")
					val hasSessionCookie = cookies.any { it.startsWith("next-auth.session-token") }

					if (hasSessionCookie) {
						// SUCESSO! Este é um redirecionamento de login válido.
						Log.i(TAG, "Fase 3: SUCESSO! Login OK (Code: 302) e Cookie de Sessão recebido.")
						sessionManager.setLoggedIn(true)
						navigateToApp()
					} else {
						// FALHA! Este é um redirecionamento para uma página de erro.
						Log.w(TAG, "Fase 3: FALHA (302) - Sem cookie de sessão. Provavelmente senha errada.")
						throw Exception("Email ou senha inválidos")
					}
				}
				// Se for 200 (Sucesso direto)
				else if (loginResponse.isSuccessful) {
					Log.i(TAG, "Fase 3: SUCESSO! Login OK (Code: ${loginResponse.code()}).")
					sessionManager.setLoggedIn(true)
					navigateToApp()
				}
				// Qualquer outro caso é um erro.
				else {
					Log.e(TAG, "Fase 3: FALHA - Erro inesperado (Code: ${loginResponse.code()})")
					throw Exception("Erro inesperado do servidor: ${loginResponse.code()}")
				}
				// --- FIM DA CORREÇÃO ---

			} catch (e: Exception) {
				// --- FASE 4: FALHA GERAL ---
				Log.e(TAG, "Fase 4: FALHA GERAL (Exceção)", e)
				Toast.makeText(this@MainActivity, e.message ?: "Erro de conexão", Toast.LENGTH_LONG).show()
				showLoading(false)
			}
		}
	}

	/**
	 * Navega para a tela principal do app (RoutesActivity)
	 * e fecha a tela de login (MainActivity).
	 */
	private fun navigateToApp() {
		// --- ESTA É A LINHA QUE VOCÊ MUDOU ---
		val intent = Intent(this, RoutesActivity::class.java)
		startActivity(intent)
		finish() // Fecha a MainActivity para que o usuário não possa "voltar" para o login
	}

	/**
	 * Controla a visibilidade dos elementos de UI de loading.
	 */
	private fun showLoading(isLoading: Boolean) {
		if (isLoading) {
			progressBar.visibility = View.VISIBLE
			buttonLogin.isEnabled = false
			buttonLogin.text = "Entrando..."
		} else {
			progressBar.visibility = View.GONE
			buttonLogin.isEnabled = true
			buttonLogin.text = "Entrar"
		}
	}
}