package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.presentation.login.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RoutesActivity : AppCompatActivity() {
	// ... resto do código igual ...

	private val viewModel: RotasViewModel by viewModels() // O Hilt cria o ViewModel aqui
	private lateinit var rotaAdapter: RotaAdapter
	private lateinit var recyclerView: RecyclerView
	private lateinit var progressBar: View // Vamos assumir que você tem ou vai adicionar uma ProgressBar
	private lateinit var sessionManager: SessionManager

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_routes)

		sessionManager = SessionManager(applicationContext)

		setupToolbar()
		setupRecyclerView()

		// Como seu layout original não tinha ProgressBar, vamos improvisar ou ignorar por enquanto
		// O ideal seria adicionar um ProgressBar no XML

		observarEstado()
	}

	private fun setupToolbar() {
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = getString(R.string.titulo_activity_rotas)
	}

	private fun setupRecyclerView() {
		recyclerView = findViewById(R.id.routesRecyclerView)
		recyclerView.layoutManager = LinearLayoutManager(this)

		rotaAdapter = RotaAdapter(emptyList()) { rotaId ->
			val intent = Intent(this, RotaDetalheActivity::class.java).apply {
				putExtra("ROTA_ID", rotaId)
			}
			startActivity(intent)
		}
		recyclerView.adapter = rotaAdapter
	}

	private fun observarEstado() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					when (state) {
						is RotasUiState.Loading -> {
							// Mostrar loading (se tiver view)
						}
						is RotasUiState.Success -> {
							rotaAdapter.updateData(state.rotas)
						}
						is RotasUiState.Empty -> {
							Toast.makeText(this@RoutesActivity, "Nenhuma rota encontrada", Toast.LENGTH_SHORT).show()
							rotaAdapter.updateData(emptyList())
						}
						is RotasUiState.Error -> {
							Toast.makeText(this@RoutesActivity, "Erro: ${state.message}", Toast.LENGTH_LONG).show()
							// Se for erro de autenticação (401), o repository deveria tratar ou lançar exceção específica
							if (state.message.contains("401")) {
								performLogout()
							}
						}
					}
				}
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.routes_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_logout -> {
				performLogout()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun performLogout() {
		sessionManager.setLoggedIn(false)
		val intent = Intent(this, MainActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
		finish()
	}
}