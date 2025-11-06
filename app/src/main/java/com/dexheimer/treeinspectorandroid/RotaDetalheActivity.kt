package com.dexheimer.treeinspectorandroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast // <-- O import está correto
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RotaDetalheActivity : AppCompatActivity() {

	private val API_BASE_URL = "https://tree-inspector-v5.vercel.app/api"

	// --- Variáveis de Estado ---
	private var listaDemandas: MutableList<Demanda> = mutableListOf()
	private var rotaCompleta: Rota? = null
	private var proximaDemanda: Demanda? = null
	private var totalParadasInicial: Int = 0

	// --- Componentes de UI ---
	private lateinit var toolbar: Toolbar
	private lateinit var fabIniciarRota: FloatingActionButton
	private lateinit var cardProximaVistoria: View
	private lateinit var tituloProximaParada: TextView
	private lateinit var enderecoProximaParada: TextView
	private lateinit var descricaoProximaParada: TextView
	private lateinit var btnIniciarVistoria: Button
	private lateinit var mapView: MapView

	// --- Banco de Dados ---
	private lateinit var db: AppDatabase
	private var rotaId: Int = -1

	/**
	 * Launcher para a tela de Detalhe.
	 * Agora, ao concluir, também remove a demanda do banco de dados local.
	 */
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			Log.d("RotaDetalheActivity", "Vistoria da demanda #${proximaDemanda?.id} concluída.")

			val demandaConcluida = proximaDemanda // Guarda a referência

			// 1. Remove da lista em memória
			if (listaDemandas.isNotEmpty()) {
				listaDemandas.removeAt(0)
			}

			// 2. Remove do banco de dados local (para persistência offline)
			if (demandaConcluida != null) {
				lifecycleScope.launch(Dispatchers.IO) {
					try {
						db.demandaDao().deleteDemanda(demandaConcluida)
						Log.d("RotaDetalheActivity", "Demanda #${demandaConcluida.id} removida do cache local.")
					} catch (e: Exception) {
						Log.e("RotaDetalheActivity", "Erro ao remover demanda do cache", e)
					}
				}
			}

			// 3. Atualiza a UI para mostrar a próxima demanda
			atualizarUI()
		} else {
			Log.d("RotaDetalheActivity", "Vistoria não finalizada (usuário voltou).")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
		setContentView(R.layout.activity_rota_detalhe)

		// Inicializa o BD
		db = AppDatabase.getInstance(applicationContext)

		// --- 1. Inicializar Componentes de UI ---
		toolbar = findViewById(R.id.toolbar)
		fabIniciarRota = findViewById(R.id.fabIniciarRota)
		cardProximaVistoria = findViewById(R.id.proximaVistoriaCard)
		tituloProximaParada = findViewById(R.id.tituloProximaParada)
		enderecoProximaParada = findViewById(R.id.enderecoProximaParada)
		descricaoProximaParada = findViewById(R.id.descricaoProximaParada)
		btnIniciarVistoria = findViewById(R.id.btnIniciarVistoria)
		mapView = findViewById(R.id.mapView)

		// Configurações de Toolbar e Mapa
		setupToolbarEMapa()

		// Esconde UI até os dados carregarem
		cardProximaVistoria.visibility = View.GONE
		fabIniciarRota.visibility = View.GONE

		// --- 2. Configurar Ações dos Botões ---
		fabIniciarRota.setOnClickListener { iniciarRotaGoogleMaps() }
		btnIniciarVistoria.setOnClickListener { abrirDetalheDemanda() }

		// --- 3. Buscar os Dados (Offline-First) ---
		rotaId = intent.getIntExtra("ROTA_ID", -1)
		if (rotaId != -1) {
			carregarDadosIniciais(rotaId)
		} else {
			Log.e("RotaDetalheActivity", "ID da Rota inválido.")
			supportActionBar?.title = "Erro: Rota não encontrada"
		}
	}

	private fun setupToolbarEMapa() {
		setSupportActionBar(toolbar)
		supportActionBar?.title = getString(R.string.titulo_activity_rota_detalhe)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.setBuiltInZoomControls(true)
		mapView.setMultiTouchControls(true)
	}

	/**
	 * Ponto de entrada da lógica de dados (Offline-First).
	 */
	private fun carregarDadosIniciais(rotaId: Int) {
		lifecycleScope.launch {
			// 1. Tenta carregar do cache
			val rotaLocal = carregarRotaDoCache(rotaId)
			val demandasLocais = carregarDemandasDoCache(rotaId)

			if (rotaLocal != null && demandasLocais.isNotEmpty()) {
				Log.d("RotaDetalheActivity", "Dados carregados do cache.")
				// 2. Cache HIT: Exibe os dados locais imediatamente
				processarDadosCarregados(rotaLocal, demandasLocais)
				supportActionBar?.title = "${rotaCompleta?.nome ?: "Detalhes"} (Offline)"
			} else {
				Log.d("RotaDetalheActivity", "Cache vazio ou incompleto.")
			}

			// 3. Sempre tenta buscar da rede (para atualizar ou buscar pela 1ª vez)
			fetchRotaDetalhesNetwork(rotaId)
		}
	}

	/**
	 * Busca os dados da REDE (Volley).
	 */
	private fun fetchRotaDetalhesNetwork(rotaId: Int) {
		val queue = Volley.newRequestQueue(this)
		val url = "$API_BASE_URL/rotas/$rotaId"

		Log.d("RotaDetalheActivity", "Buscando dados da rede: $url")

		val jsonObjectRequest = JsonObjectRequest(
			Request.Method.GET, url, null,
			{ response ->
				Log.d("RotaDetalheActivity", "Sucesso na rede.")
				try {
					val gson = Gson()
					val resposta = gson.fromJson(response.toString(), RotaDetalheResponse::class.java)

					// 1. Atualiza a UI com os dados frescos da rede
					processarDadosCarregados(resposta.rota, resposta.demandas)

					// 2. Salva os dados frescos no cache em segundo plano
					lifecycleScope.launch(Dispatchers.IO) {
						salvarDadosNoCache(resposta.rota, resposta.demandas, rotaId)
					}

				} catch (e: Exception) {
					Log.e("RotaDetalheActivity", "Erro ao processar JSON: ${e.message}", e)
				}
			},
			{ error ->
				Log.w("RotaDetalheActivity", "Erro de Rede (Volley): ${error.message}", error)
				// Se chegamos aqui, ou o cache já foi carregado ou está vazio.
				// Se a lista de demandas ainda estiver vazia, significa que falhamos em tudo.
				if (listaDemandas.isEmpty()) {
					supportActionBar?.title = "Erro: Sem conexão"
					Toast.makeText(this, "Falha na rede e sem dados locais.", Toast.LENGTH_LONG).show()
				}
			}
		)
		queue.add(jsonObjectRequest)
	}

	/**
	 * Lógica de negócios centralizada para preencher as variáveis de estado.
	 * Usado tanto pelo Cache quanto pela Rede.
	 */
	private fun processarDadosCarregados(rota: Rota, demandas: List<Demanda>) {
		rotaCompleta = rota
		listaDemandas = demandas.toMutableList()
		totalParadasInicial = listaDemandas.size

		supportActionBar?.title = rotaCompleta?.nome ?: "Detalhes da Rota"

		atualizarUI()
	}

	// --- Funções do Banco de Dados (Cache) ---

	private suspend fun carregarRotaDoCache(rotaId: Int): Rota? {
		return withContext(Dispatchers.IO) {
			try {
				db.rotaDao().getRotaById(rotaId)
			} catch (e: Exception) {
				Log.e("RotaDetalheActivity", "Erro ao buscar rota do cache", e)
				null
			}
		}
	}

	private suspend fun carregarDemandasDoCache(rotaId: Int): List<Demanda> {
		return withContext(Dispatchers.IO) {
			try {
				db.demandaDao().getDemandasDaRota(rotaId)
			} catch (e: Exception) {
				Log.e("RotaDetalheActivity", "Erro ao buscar demandas do cache", e)
				emptyList()
			}
		}
	}

	private suspend fun salvarDadosNoCache(rota: Rota, demandas: List<Demanda>, rotaId: Int) {
		withContext(Dispatchers.IO) {
			try {
				Log.d("RotaDetalheActivity", "Salvando ${demandas.size} demandas no cache para rota $rotaId...")

				db.rotaDao().insertRota(rota)

				db.demandaDao().clearDemandasDaRota(rotaId) // Limpa dados antigos
				demandas.forEach { it.rotaId = rotaId } // Associa as demandas à rota
				db.demandaDao().insertAll(demandas) // Insere dados novos

				Log.d("RotaDetalheActivity", "Cache salvo com sucesso.")
			} catch (e: Exception) {
				Log.e("RotaDetalheActivity", "Erro ao salvar dados no cache", e)
			}
		}
	}

	// --- Lógica da UI (Funções restantes) ---

	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}

	/**
	 * Atualiza a UI (Card, Mapa, Botões) com base no estado atual de 'listaDemandas'.
	 */
	private fun atualizarUI() {
		if (listaDemandas.isEmpty()) {
			// Rota Concluída
			tituloProximaParada.text = "Rota Concluída"
			enderecoProximaParada.text = "Não há mais paradas nesta rota."
			descricaoProximaParada.visibility = View.GONE
			btnIniciarVistoria.visibility = View.GONE
			fabIniciarRota.visibility = View.GONE
			cardProximaVistoria.visibility = View.VISIBLE
			mapView.overlays.clear()
			mapView.invalidate()
			return
		}

		proximaDemanda = listaDemandas[0]
		val paradasConcluidas = totalParadasInicial - listaDemandas.size
		val numeroParadaAtual = paradasConcluidas + 1

		// 1. Preenche o Card
		tituloProximaParada.text = "Próxima Parada ($numeroParadaAtual de $totalParadasInicial)"
		enderecoProximaParada.text = "${proximaDemanda?.logradouro ?: "End."} ${proximaDemanda?.numero ?: ""} - ${proximaDemanda?.bairro ?: ""}"
		descricaoProximaParada.text = proximaDemanda?.descricao ?: "Sem descrição."

		cardProximaVistoria.visibility = View.VISIBLE
		fabIniciarRota.visibility = View.VISIBLE
		btnIniciarVistoria.visibility = View.VISIBLE

		// 2. Preenche o Mapa
		val mapController = mapView.controller
		mapView.overlays.clear()

		for ((index, demanda) in listaDemandas.withIndex()) {
			val coords = demanda.geom?.coordinates
			if (coords != null && coords.size == 2) {
				val point = GeoPoint(coords[1], coords[0]) // Lat, Lng
				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				marker.title = "Parada ${index + numeroParadaAtual}: ${demanda.logradouro}"

				marker.icon = getDrawable(if (index == 0) R.drawable.ic_marker_green else R.drawable.ic_marker_blue)
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
			mapController.setZoom(12.0)
			mapController.setCenter(GeoPoint(-29.8608, -51.1789)) // Fallback
		}
		mapView.invalidate()
	}

	private fun iniciarRotaGoogleMaps() {
		if (proximaDemanda == null) return
		val coords = proximaDemanda?.geom?.coordinates
		if (coords != null && coords.size == 2) {
			val gmmIntentUri = Uri.parse("google.navigation:q=${coords[1]},${coords[0]}")
			val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
			mapIntent.setPackage("com.google.android.apps.maps")
			if (mapIntent.resolveActivity(packageManager) != null) {
				startActivity(mapIntent)
			} else {
				// ============== CORREÇÃO ESTÁ AQUI ==============
				Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
			}
		} else {
			// ============== E AQUI ==============
			// Esta é a linha 341 que você mencionou
			Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
		}
	}

	private fun abrirDetalheDemanda() {
		if (proximaDemanda == null) return
		val intent = Intent(this, DemandaDetalheActivity::class.java).apply {
			putExtra("DEMANDA_EXTRA", proximaDemanda)
		}
		vistoriaLauncher.launch(intent)
	}

	// --- Ciclo de Vida do Mapa ---
	override fun onResume() {
		super.onResume()
		mapView.onResume()
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
	}
}