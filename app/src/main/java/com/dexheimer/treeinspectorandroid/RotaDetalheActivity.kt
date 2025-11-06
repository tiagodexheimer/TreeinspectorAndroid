package com.dexheimer.treeinspectorandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker // Importar o Marker

class RotaDetalheActivity : AppCompatActivity() {

	private val API_BASE_URL = "https://tree-inspector-v5.vercel.app/api"
	private var listaDemandas: List<Demanda> = emptyList()
	private var rotaCompleta: Rota? = null
	private var proximaDemanda: Demanda? = null

	// Declaração dos componentes de UI
	private lateinit var toolbar: Toolbar
	private lateinit var fabIniciarRota: FloatingActionButton
	private lateinit var cardProximaVistoria: View
	private lateinit var tituloProximaParada: TextView
	private lateinit var enderecoProximaParada: TextView
	private lateinit var descricaoProximaParada: TextView
	private lateinit var btnIniciarVistoria: Button
	private lateinit var mapView: MapView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Configuração do osmdroid (precisa estar ANTES do setContentView)
		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

		setContentView(R.layout.activity_rota_detalhe)

		// --- 1. Inicializar Componentes de UI ---
		toolbar = findViewById(R.id.toolbar)
		fabIniciarRota = findViewById(R.id.fabIniciarRota)
		cardProximaVistoria = findViewById(R.id.proximaVistoriaCard)
		tituloProximaParada = findViewById(R.id.tituloProximaParada)
		enderecoProximaParada = findViewById(R.id.enderecoProximaParada)
		descricaoProximaParada = findViewById(R.id.descricaoProximaParada)
		btnIniciarVistoria = findViewById(R.id.btnIniciarVistoria)
		mapView = findViewById(R.id.mapView)

		// Configuração da Toolbar
		setSupportActionBar(toolbar)
		supportActionBar?.title = getString(R.string.titulo_activity_rota_detalhe)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		// Configuração inicial do Mapa
		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.setBuiltInZoomControls(true)
		mapView.setMultiTouchControls(true)

		// Esconde o card e botões até os dados carregarem
		cardProximaVistoria.visibility = View.GONE
		fabIniciarRota.visibility = View.GONE

		// --- 2. Configurar Ações dos Botões ---
		fabIniciarRota.setOnClickListener {
			iniciarRotaGoogleMaps()
		}
		btnIniciarVistoria.setOnClickListener {
			abrirDetalheDemanda()
		}

		// --- 3. Buscar os Dados ---
		val rotaId = intent.getIntExtra("ROTA_ID", -1)
		if (rotaId != -1) {
			fetchRotaDetalhes(rotaId)
		} else {
			Log.e("RotaDetalheActivity", "ID da Rota inválido.")
			supportActionBar?.title = "Erro: Rota não encontrada"
		}
	}

	// Função para o botão "Voltar" da Toolbar
	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}

	// Busca os dados da API usando GSON
	private fun fetchRotaDetalhes(rotaId: Int) {
		val queue = Volley.newRequestQueue(this)
		val url = "$API_BASE_URL/rotas/$rotaId"

		Log.d("RotaDetalheActivity", "Buscando detalhes da rota: $url")

		val jsonObjectRequest = JsonObjectRequest(
			Request.Method.GET, url, null,
			{ response ->
				try {
					val gson = Gson()
					val resposta = gson.fromJson(response.toString(), RotaDetalheResponse::class.java)

					// Armazena os dados
					rotaCompleta = resposta.rota
					listaDemandas = resposta.demandas

					// Atualiza o título da Toolbar
					supportActionBar?.title = rotaCompleta?.nome ?: "Detalhes da Rota"

					// Atualiza a UI com os dados
					atualizarUI()

				} catch (e: Exception) {
					Log.e("RotaDetalheActivity", "Erro ao processar JSON com GSON: ${e.message}", e)
				}
			},
			{ error ->
				Log.e("RotaDetalheActivity", "Erro de Rede (Volley): ${error.message}", error)
			}
		)
		queue.add(jsonObjectRequest)
	}

	// Nova função para preencher a UI com os dados da API
	private fun atualizarUI() {
		if (listaDemandas.isEmpty()) {
			// Caso a rota esteja vazia
			tituloProximaParada.text = "Rota Concluída"
			enderecoProximaParada.text = "Não há mais paradas nesta rota."
			descricaoProximaParada.visibility = View.GONE
			btnIniciarVistoria.visibility = View.GONE
			fabIniciarRota.visibility = View.GONE
			cardProximaVistoria.visibility = View.VISIBLE
			return
		}

		// Pega a próxima demanda (a primeira da lista)
		proximaDemanda = listaDemandas[0]
		val totalParadas = listaDemandas.size

		// 1. Preenche o Card "Próxima Vistoria"
		tituloProximaParada.text = "Próxima Parada (1 de $totalParadas)"
		enderecoProximaParada.text = "${proximaDemanda?.logradouro ?: "Endereço"} ${proximaDemanda?.numero ?: ""} - ${proximaDemanda?.bairro ?: ""}"
		descricaoProximaParada.text = proximaDemanda?.descricao ?: "Sem descrição."

		cardProximaVistoria.visibility = View.VISIBLE
		fabIniciarRota.visibility = View.VISIBLE
		btnIniciarVistoria.visibility = View.VISIBLE

		// 2. Preenche o Mapa
		val mapController = mapView.controller
		mapView.overlays.clear() // Limpa marcadores antigos

		// Adiciona marcadores para TODAS as demandas
		for ((index, demanda) in listaDemandas.withIndex()) {
			val coords = demanda.geom?.coordinates
			if (coords != null && coords.size == 2) {
				val point = GeoPoint(coords[1], coords[0]) // Lat, Lng

				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				marker.title = "Parada ${index + 1}: ${demanda.logradouro}"

				// Define o ícone: verde para a próxima, azul para as restantes
				if (index == 0) {
					marker.icon = getDrawable(R.drawable.ic_marker_green) // Ícone verde (precisamos criar)
				} else {
					marker.icon = getDrawable(R.drawable.ic_marker_blue) // Ícone azul (precisamos criar)
				}

				mapView.overlays.add(marker)
			}
		}

		// Centraliza o mapa na primeira demanda
		val primeiraParadaCoords = proximaDemanda?.geom?.coordinates
		if (primeiraParadaCoords != null && primeiraParadaCoords.size == 2) {
			val startPoint = GeoPoint(primeiraParadaCoords[1], primeiraParadaCoords[0])
			mapController.setZoom(15.0)
			mapController.setCenter(startPoint)
		} else {
			// Fallback se a primeira parada não tiver coords
			mapController.setZoom(12.0)
			mapController.setCenter(GeoPoint(-29.8608, -51.1789)) // Ponto fixo
		}
		mapView.invalidate() // Redesenha o mapa
	}

	// Ação do clique no FAB "Iniciar Rota"
	private fun iniciarRotaGoogleMaps() {
		if (proximaDemanda == null) return

		val coords = proximaDemanda?.geom?.coordinates
		if (coords != null && coords.size == 2) {
			val latitude = coords[1]
			val longitude = coords[0]
			val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
			val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
			mapIntent.setPackage("com.google.android.apps.maps")

			if (mapIntent.resolveActivity(packageManager) != null) {
				startActivity(mapIntent)
			} else {
				Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
			}
		} else {
			Toast.makeText(this, "Coordenadas inválidas para a parada", Toast.LENGTH_SHORT).show()
		}
	}

	// Ação do clique no botão "Iniciar Vistoria"
	private fun abrirDetalheDemanda() {
		if (proximaDemanda == null) return

		val intent = Intent(this, DemandaDetalheActivity::class.java).apply {
			putExtra("DEMANDA_EXTRA", proximaDemanda)
		}
		startActivity(intent)
	}

	// Ciclo de vida do Mapa (necessário)
	override fun onResume() {
		super.onResume()
		mapView.onResume()
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
	}
}