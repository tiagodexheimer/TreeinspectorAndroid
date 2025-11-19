package com.dexheimer.treeinspectorandroid.presentation.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.presentation.rotas.RoutesActivity
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.remote.LoginRequest
import com.dexheimer.treeinspectorandroid.data.remote.NetworkClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	private val TAG = "MainActivity"

	private lateinit var sessionManager: SessionManager
	private lateinit var editTextEmail: TextInputEditText
	private lateinit var editTextPassword: TextInputEditText
	private lateinit var buttonLogin: Button
	private lateinit var progressBar: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// 1. Inicializa Sessão
		sessionManager = SessionManager(applicationContext)

		// 2. Se já logado, vai direto para o App
		if (sessionManager.isLoggedIn()) {
			Log.d(TAG, "Usuário já logado, redirecionando...")
			navigateToApp()
			return
		}

		// 3. Se não, mostra a tela de login
		setContentView(R.layout.activity_main)

		editTextEmail = findViewById(R.id.editTextEmail)
		editTextPassword = findViewById(R.id.editTextPassword)
		buttonLogin = findViewById(R.id.buttonLogin)
		progressBar = findViewById(R.id.progressBar)

		buttonLogin.setOnClickListener {
			performLogin()
		}
	}

	private fun performLogin() {
		val email = editTextEmail.text.toString().trim()
		val password = editTextPassword.text.toString().trim()

		if (email.isEmpty() || password.isEmpty()) {
			Toast.makeText(this, "Email e senha são obrigatórios", Toast.LENGTH_SHORT).show()
			return
		}

		showLoading(true)

		lifecycleScope.launch {
			try {
				// FASE 1: Obter Token CSRF (Segurança do NextAuth)
				Log.d(TAG, "Buscando CSRF...")
				val csrfResponse = NetworkClient.api.getCsrfToken()

				if (!csrfResponse.isSuccessful || csrfResponse.body()?.csrfToken == null) {
					throw Exception("Falha ao obter token de segurança.")
				}
				val csrfToken = csrfResponse.body()!!.csrfToken

				// FASE 2: Enviar Credenciais de Login
				Log.d(TAG, "Enviando credenciais...")
				val request =
					LoginRequest(email = email, password = password, csrfToken = csrfToken)
				val loginResponse = NetworkClient.api.login(request)

				// FASE 3: Processar Resposta
				if (loginResponse.isSuccessful) {
					// Caso 200 OK (Raro no NextAuth padrão, mas possível)
					Log.i(TAG, "Login bem-sucedido (200).")
					sessionManager.setLoggedIn(true)
					navigateToApp()
				} else if (loginResponse.code() == 302) {
					// Caso 302 Found (Padrão do NextAuth: Redireciona após login)
					val cookies = loginResponse.headers().values("Set-Cookie")

					// Procura pelo cookie de sessão (suporta v4 e v5 beta do Auth.js)
					val cookieString = cookies.find {
						it.contains("next-auth.session-token") || it.contains("authjs.session-token")
					}

					if (cookieString != null) {
						// SUCESSO: Cookie encontrado!

						// Extrai apenas o valor "nome=token" antes do primeiro ponto-e-vírgula
						// Isso é crucial para injetar no header depois
						val rawToken = cookieString.split(";")[0]

						// Salva o token para uso posterior no Worker (Sync)
						sessionManager.saveAuthToken(rawToken)
						Log.i(TAG, "Token de sessão salvo com sucesso.")

						// Salva estado de login e navega
						sessionManager.setLoggedIn(true)
						navigateToApp()
					} else {
						// Redirecionou, mas sem cookie. Verifica se foi para página de erro.
						val location = loginResponse.headers()["Location"] ?: ""
						if (location.contains("error")) {
							throw Exception("Credenciais inválidas.")
						} else {
							// Fallback: Se redirecionou para home/callback sem erro explícito,
							// assumimos que o CookieJar pegou o cookie automaticamente.
							Log.w(TAG, "Redirecionamento sem cookie explícito no header, assumindo sucesso...")
							sessionManager.setLoggedIn(true)
							navigateToApp()
						}
					}
				} else if (loginResponse.code() == 401) {
					throw Exception("Email ou senha incorretos.")
				} else {
					throw Exception("Falha no login. Código: ${loginResponse.code()}")
				}

			} catch (e: Exception) {
				Log.e(TAG, "Erro no login: ${e.message}", e)
				Toast.makeText(this@MainActivity, e.message ?: "Erro de conexão", Toast.LENGTH_LONG).show()
				showLoading(false)
			}
		}
	}

	private fun navigateToApp() {
		val intent = Intent(this, RoutesActivity::class.java)
		startActivity(intent)
		finish() // Fecha a tela de login para não voltar com "Voltar"
	}

	private fun showLoading(isLoading: Boolean) {
		if (isLoading) {
			progressBar.visibility = View.VISIBLE
			buttonLogin.isEnabled = false
			buttonLogin.text = "Entrando..."
			editTextEmail.isEnabled = false
			editTextPassword.isEnabled = false
		} else {
			progressBar.visibility = View.GONE
			buttonLogin.isEnabled = true
			buttonLogin.text = "Entrar"
			editTextEmail.isEnabled = true
			editTextPassword.isEnabled = true
		}
	}
}