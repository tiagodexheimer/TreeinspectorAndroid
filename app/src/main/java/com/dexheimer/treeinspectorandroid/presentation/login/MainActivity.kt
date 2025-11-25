package com.dexheimer.treeinspectorandroid.presentation.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.presentation.rotas.RoutesActivity
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	private val viewModel: LoginViewModel by viewModels()
	private lateinit var sessionManager: SessionManager

	private lateinit var editTextEmail: TextInputEditText
	private lateinit var editTextPassword: TextInputEditText
	private lateinit var buttonLogin: Button
	private lateinit var progressBar: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Verifica se veio do Interceptor
		if (intent.getBooleanExtra("LOGIN_EXPIRED", false)) {
			Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
		}

		sessionManager = SessionManager(applicationContext)

		if (sessionManager.isLoggedIn()) {
			navigateToApp()
			return
		}

		setContentView(R.layout.activity_main)

		setupUI()
		observarEstado()



	}

	private fun setupUI() {
		editTextEmail = findViewById(R.id.editTextEmail)
		editTextPassword = findViewById(R.id.editTextPassword)
		buttonLogin = findViewById(R.id.buttonLogin)
		progressBar = findViewById(R.id.progressBar)

		buttonLogin.setOnClickListener {
			performLogin()
		}
	}

	private fun observarEstado() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					showLoading(state.isLoading)

					if (state.isLoggedIn) {
						navigateToApp()
					}

					if (state.error != null) {
						Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_LONG).show()
					}
				}
			}
		}
	}

	private fun performLogin() {
		val email = editTextEmail.text.toString().trim()
		val password = editTextPassword.text.toString().trim()

		if (email.isEmpty() || password.isEmpty()) {
			Toast.makeText(this, "Email e senha são obrigatórios", Toast.LENGTH_SHORT).show()
			return
		}

		viewModel.login(email, password)
	}

	private fun navigateToApp() {
		val intent = Intent(this, RoutesActivity::class.java)
		startActivity(intent)
		finish()
	}

	private fun showLoading(isLoading: Boolean) {
		progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
		buttonLogin.isEnabled = !isLoading
		buttonLogin.text = if (isLoading) "Entrando..." else "Entrar"
		editTextEmail.isEnabled = !isLoading
		editTextPassword.isEnabled = !isLoading
	}
}