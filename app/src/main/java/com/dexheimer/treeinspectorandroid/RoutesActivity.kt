package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoutesActivity : AppCompatActivity() {

	private val API_URL = "https://tree-inspector-v5.vercel.app/api/rotas" // <-- Verifique se está correto
	private val LOG_TAG = "RoutesActivity" // <-- Tag para filtrar o Logcat

	private lateinit var listViewRoutes: ListView
	private lateinit var progressBar: ProgressBar
	private lateinit var textViewError: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_routes)

		listViewRoutes = findViewById(R.id.listViewRoutes)
		progressBar = findViewById(R.id.progressBar)
		textViewError = findViewById(R.id.textViewError)

		fetchRotas()

		listViewRoutes.setOnItemClickListener { parent, view, position, id ->
			val rotaSelecionada = parent.adapter.getItem(position) as Rota

			// Cria a intenção para abrir a nova Activity
			val intent = Intent(this, RotaDetalheActivity::class.java).apply {
				// Adiciona o ID da rota como um "extra"
				putExtra("ROTA_ID", rotaSelecionada.id)
				putExtra("ROTA_NOME", rotaSelecionada.nome) // Passa o nome também
			}
			startActivity(intent)
		}
	}

	private fun fetchRotas() {
		progressBar.visibility = View.VISIBLE
		listViewRoutes.visibility = View.GONE
		textViewError.visibility = View.GONE

		val queue = Volley.newRequestQueue(this)

		val jsonArrayRequest = JsonArrayRequest(
			Request.Method.GET, API_URL, null,
			{ response ->
				// SUCESSO! Vamos tentar o parsing
				Log.d(LOG_TAG, "JSON Recebido: $response") // <-- LOG DA RESPOSTA

				try {
					val gson = Gson()
					val tipoListaRotas = object : TypeToken<List<Rota>>() {}.type
					val rotas: List<Rota> = gson.fromJson(response.toString(), tipoListaRotas)

					val adapter = RotasAdapter(this, rotas)
					listViewRoutes.adapter = adapter

					progressBar.visibility = View.GONE
					listViewRoutes.visibility = View.VISIBLE

				} catch (e: Exception) {
					// Erro ao processar o JSON
					Log.e(LOG_TAG, "Erro no GSON (parsing): ${e.message}", e) // <-- LOG DO ERRO DE PARSING
					showError()
				}
			},
			{ error ->
				// Erro na requisição (Rede)
				Log.e(LOG_TAG, "Erro de Rede (Volley): ${error.message}", error) // <-- LOG DO ERRO DE REDE
				showError()
			}
		)
		queue.add(jsonArrayRequest)
	}

	private fun showError() {
		progressBar.visibility = View.GONE
		textViewError.visibility = View.VISIBLE
	}
}