// app/src/main/java/com/dexheimer/treeinspectorandroid/MainActivity.kt
package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

		// 2. Se já logado, vai para o App
		if (sessionManager.isLoggedIn()) {
			navigateToApp()
			return
		}

		// 3. Se não, mostra login
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
				// FASE 1: CSRF
				Log.d(TAG, "Buscando CSRF...")
				val csrfResponse = NetworkClient.api.getCsrfToken()
				if (!csrfResponse.isSuccessful || csrfResponse.body()?.csrfToken == null) {
					throw Exception("Falha ao obter token de segurança.")
				}
				val csrfToken = csrfResponse.body()!!.csrfToken

				// FASE 2: LOGIN
				Log.d(TAG, "Enviando credenciais...")
				val request = LoginRequest(email = email, password = password, csrfToken = csrfToken)
				val loginResponse = NetworkClient.api.login(request)

				if (loginResponse.isSuccessful) {
					Log.i(TAG, "Login bem-sucedido (200).")
					sessionManager.setLoggedIn(true)
					navigateToApp()
				} else if (loginResponse.code() == 302) {
					// Tratamento de Redirecionamento (padrão NextAuth)
					val cookies = loginResponse.headers().values("Set-Cookie")

					// Verifica se recebemos o cookie de sessão
					val hasSessionCookie = cookies.any { cookie ->
						cookie.contains("next-auth.session-token") ||
								cookie.contains("authjs.session-token")
					}

					if (hasSessionCookie) {
						Log.i(TAG, "Login bem-sucedido (302 com cookie).")
						sessionManager.setLoggedIn(true)
						navigateToApp()
					} else {
						// Verifica se o redirecionamento foi para uma página de erro
						val location = loginResponse.headers()["Location"] ?: ""
						if (location.contains("error")) {
							throw Exception("Credenciais inválidas.")
						} else {
							// Fallback: Assume sucesso se não for erro explícito
							Log.w(TAG, "Redirecionamento neutro, assumindo sucesso...")
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
		finish()
	}

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