package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley

class RoutesActivity : AppCompatActivity() {

	private val API_URL = "${BuildConfig.API_BASE_URL}rotas"
	private lateinit var recyclerView: RecyclerView
	private lateinit var rotaAdapter: RotaAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_routes)

		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)

		supportActionBar?.title = getString(R.string.titulo_activity_rotas)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		recyclerView = findViewById(R.id.routesRecyclerView)
		recyclerView.layoutManager = LinearLayoutManager(this)

		// O adapter agora é inicializado corretamente (sem erro de referência)
		rotaAdapter = RotaAdapter(emptyList()) { rotaId ->
			val intent = Intent(this, RotaDetalheActivity::class.java).apply {
				putExtra("ROTA_ID", rotaId)
			}
			startActivity(intent)
		}
		recyclerView.adapter = rotaAdapter

		fetchRoutes()
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}

	private fun fetchRoutes() {
		val queue = Volley.newRequestQueue(this)
		val jsonArrayRequest = JsonArrayRequest(
			Request.Method.GET, API_URL, null,
			{ response ->
				val rotas = mutableListOf<Rota>()
				for (i in 0 until response.length()) {
					val rotaJson = response.getJSONObject(i)

					// *** ESTA É A MUDANÇA PRINCIPAL ***
					// Mapeia TODOS os dados do JSON para o Rota.kt
					val rota = Rota(
						id = rotaJson.getInt("id"),
						nome = rotaJson.getString("nome"),

						// --- CORREÇÃO: Adicionar os campos que faltavam ---
						// Use optString para campos que podem ser nulos
						responsavel = rotaJson.optString("responsavel", null),
						status = rotaJson.optString("status", null),
						data_rota = rotaJson.optString("data_rota", null),

						// --- CORREÇÃO: Corrigir o nome do parâmetro ---
						created_at = rotaJson.getString("created_at"),

						// Mapeia "total_demandas" (Int)
						total_demandas = rotaJson.getInt("total_demandas")
					)
					rotas.add(rota)
				}
				rotaAdapter.updateData(rotas)
			},
			{ error ->
				Log.e("RoutesActivity", "Erro de Rede (Volley): ${error.message}", error)
			}
		)
		queue.add(jsonArrayRequest)
	}
}