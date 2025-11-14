package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu // <-- ADICIONADO
import android.view.MenuItem // <-- ADICIONADO
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley

class RoutesActivity : AppCompatActivity() {

	// --- CORREÇÃO DA "BARRA DUPLA" ---
	// Removida a barra "/" do início da string
	private val API_URL = "${BuildConfig.API_BASE_URL}api/rotas"

	private lateinit var recyclerView: RecyclerView
	private lateinit var rotaAdapter: RotaAdapter

	// --- ADICIONADO ---
	// Gerenciador de Sessão para fazer o logout
	private lateinit var sessionManager: SessionManager

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_routes)

		// --- ADICIONADO ---
		// Inicializa o SessionManager
		sessionManager = SessionManager(applicationContext)

		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)

		supportActionBar?.title = getString(R.string.titulo_activity_rotas)

		// Removido o "botão voltar" daqui, já que esta é a tela principal pós-login
		// supportActionBar?.setDisplayHomeAsUpEnabled(true)

		recyclerView = findViewById(R.id.routesRecyclerView)
		recyclerView.layoutManager = LinearLayoutManager(this)

		rotaAdapter = RotaAdapter(emptyList()) { rotaId ->
			val intent = Intent(this, RotaDetalheActivity::class.java).apply {
				putExtra("ROTA_ID", rotaId)
			}
			startActivity(intent)
		}
		recyclerView.adapter = rotaAdapter

		fetchRoutes()
	}

	// --- ADICIONADO (FUNÇÃO 1) ---
	/**
	 * Infla (cria) o menu de 3 pontinhos na Toolbar.
	 */
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.routes_menu, menu)
		return true
	}

	// --- ADICIONADO (FUNÇÃO 2) ---
	/**
	 * Lida com o clique em um item do menu (ex: "Sair").
	 */
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_logout -> {
				// Usuário clicou em "Sair"
				performLogout()
				true
			}
			// Lida com o botão "Voltar" da toolbar (se estivesse habilitado)
			android.R.id.home -> {
				onBackPressedDispatcher.onBackPressed()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	// --- ADICIONADO (FUNÇÃO 3) ---
	/**
	 * Limpa a sessão do usuário e o redireciona para a tela de Login.
	 */
	private fun performLogout() {
		// 1. Limpa o status de "logado"
		sessionManager.setLoggedIn(false)

		// 2. Navega de volta para a MainActivity (Login)
		val intent = Intent(this, MainActivity::class.java)

		// 3. Limpa o histórico de telas
		// Isso impede que o usuário aperte "Voltar" e retorne
		// para a tela de rotas após o logout.
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

		startActivity(intent)
		finish() // Fecha a RoutesActivity
	}

	// Esta função não existe mais na sua versão, mas se existisse,
	// seria tratada pelo onOptionsItemSelected
	/*
	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}
	*/

	private fun fetchRoutes() {
		val queue = Volley.newRequestQueue(this)
		val jsonArrayRequest = JsonArrayRequest(
			Request.Method.GET, API_URL, null,
			{ response ->
				val rotas = mutableListOf<Rota>()
				for (i in 0 until response.length()) {
					val rotaJson = response.getJSONObject(i)

					try { // Adicionado um try/catch para mais segurança
						val rota = Rota(
							id = rotaJson.getInt("id"),
							nome = rotaJson.getString("nome"),
							responsavel = rotaJson.optString("responsavel", null),
							status = rotaJson.optString("status", null),
							data_rota = rotaJson.optString("data_rota", null),
							created_at = rotaJson.getString("created_at"),
							total_demandas = rotaJson.getInt("total_demandas")
						)
						rotas.add(rota)
					} catch (e: Exception) {
						Log.e("RoutesActivity", "Erro ao processar Rota JSON: ${e.message}", e)
					}
				}
				rotaAdapter.updateData(rotas)
			},
			{ error ->
				Log.e("RoutesActivity", "Erro de Rede (Volley): ${error.message}", error)
				// --- ADICIONADO ---
				// Se o erro for de autenticação (401, 403), desloga o usuário
				if (error.networkResponse?.statusCode == 401 || error.networkResponse?.statusCode == 403) {
					Log.w("RoutesActivity", "Sessão inválida (401/403). Deslogando...")
					performLogout()
				}
			}
		)
		queue.add(jsonArrayRequest)
	}
}