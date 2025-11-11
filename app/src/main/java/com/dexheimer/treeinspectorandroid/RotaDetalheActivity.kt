package com.dexheimer.treeinspectorandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu // <-- ADICIONADO PARA O MENU
import android.view.MenuItem // <-- ADICIONADO PARA O MENU
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RotaDetalheActivity : AppCompatActivity() {

	private val API_BASE_URL = "https://tree-inspector-v5.vercel.app/api"

	// --- Variáveis de Estado ---
	private var demandasPendentes: MutableList<Demanda> = mutableListOf()
	private var demandasCarregadas: MutableList<Demanda> = mutableListOf()
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
	private lateinit var btnVerTodasDemandas: Button
	private lateinit var mapView: MapView

	// --- Banco de Dados ---
	private lateinit var db: AppDatabase
	private var rotaId: Int = -1

	// --- Variáveis de Localização ---
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private var localizacaoUsuario: Location? = null

	private val locationPermissionRequest = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			getUsersCurrentLocationAndAsk()
		} else {
			Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
			showOptimizeDialog(null)
		}
	}

	/**
	 * Launcher para o CARD DA PRÓXIMA VISTORIA.
	 */
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS") ?: return@registerForActivityResult
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1

			if (demandaId != -1 && demandaId == proximaDemanda?.id) {
				Log.d("RotaDetalheActivity", "Atualizando status da PRÓXIMA demanda")
				atualizarStatusDemanda(demandaId, novoStatus)
			}
		}
	}

	/**
	 * Launcher para a TELA DE LISTA.
	 * Quando ela fecha, recarregamos TUDO, pois qualquer item pode ter mudado.
	 */
	private val demandaListLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			Log.d("RotaDetalheActivity", "Retornando da lista de demandas, recarregando...")
			// Recarrega tudo do DB para garantir que os status estão corretos
			lifecycleScope.launch {
				val demandasLocais = carregarDemandasDoCache(rotaId)
				processarDadosCarregados(rotaCompleta!!, demandasLocais, false) // 'false' para não otimizar auto
			}
		}
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
		setContentView(R.layout.activity_rota_detalhe)

		db = AppDatabase.getInstance(applicationContext)
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

		// --- 1. Inicializar Componentes de UI ---
		toolbar = findViewById(R.id.toolbar)
		fabIniciarRota = findViewById(R.id.fabIniciarRota)
		cardProximaVistoria = findViewById(R.id.proximaVistoriaCard)
		tituloProximaParada = findViewById(R.id.tituloProximaParada)
		enderecoProximaParada = findViewById(R.id.enderecoProximaParada)
		descricaoProximaParada = findViewById(R.id.descricaoProximaParada)
		btnIniciarVistoria = findViewById(R.id.btnIniciarVistoria)
		btnVerTodasDemandas = findViewById(R.id.btnVerTodasDemandas)
		mapView = findViewById(R.id.mapView)

		setupToolbarEMapa()

		cardProximaVistoria.visibility = View.GONE
		fabIniciarRota.visibility = View.GONE

		// --- 2. Configurar Ações dos Botões ---
		fabIniciarRota.setOnClickListener { iniciarRotaGoogleMaps() }
		btnIniciarVistoria.setOnClickListener { abrirDetalheProximaDemanda() }
		btnVerTodasDemandas.setOnClickListener { abrirListaDeDemandas() }

		// --- 3. Buscar os Dados ---
		rotaId = intent.getIntExtra("ROTA_ID", -1)
		if (rotaId != -1) {
			carregarDadosIniciais(rotaId)
		} else {
			Log.e("RotaDetalheActivity", "ID da Rota inválido.")
			supportActionBar?.title = "Erro: Rota não encontrada"
		}
	}

	// --- FUNÇÕES DE LÓGICA DE STATUS ---

	private fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				db.demandaDao().updateStatus(demandaId, novoStatus)
			}
			demandasCarregadas.find { it.id == demandaId }?.status_vistoria = novoStatus
			filtrarDemandasPendentes()
		}
	}

	private fun filtrarDemandasPendentes() {
		demandasPendentes = demandasCarregadas.filter {
			it.status_vistoria.equals("pendente", ignoreCase = true)
		}.toMutableList()

		demandasPendentes.sortBy { demanda ->
			demandasCarregadas.indexOfFirst { it.id == demanda.id }
		}

		totalParadasInicial = demandasPendentes.size
		atualizarUI()
	}

	// --- LÓGICA DE CARREGAMENTO DE DADOS ---

	private fun setupToolbarEMapa() {
		setSupportActionBar(toolbar)
		supportActionBar?.title = getString(R.string.titulo_activity_rota_detalhe)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.setBuiltInZoomControls(true)
		mapView.setMultiTouchControls(true)
	}

	private fun carregarDadosIniciais(rotaId: Int) {
		lifecycleScope.launch {
			val rotaLocal = carregarRotaDoCache(rotaId)
			val demandasLocais = carregarDemandasDoCache(rotaId)

			if (rotaLocal != null && demandasLocais.isNotEmpty()) {
				Log.d("RotaDetalheActivity", "Dados carregados do cache.")
				processarDadosCarregados(rotaLocal, demandasLocais, false)
				supportActionBar?.title = "${rotaCompleta?.nome ?: "Detalhes"} (Offline)"
			} else {
				Log.d("RotaDetalheActivity", "Cache vazio ou incompleto.")
			}
			fetchRotaDetalhesNetwork(rotaId)
		}
	}

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

					lifecycleScope.launch {
						val demandasDaApi = resposta.demandas.toMutableList()
						val demandasLocais = withContext(Dispatchers.IO) {
							db.demandaDao().getDemandasDaRota(rotaId)
						}
						val mapaStatusLocal = demandasLocais.associateBy({ it.id }, { it.status_vistoria })

						val demandasMescladas = demandasDaApi.map { demandaApi ->
							val statusSalvo = mapaStatusLocal[demandaApi.id]
							if (statusSalvo != null) {
								demandaApi.status_vistoria = statusSalvo
							} else {
								demandaApi.status_vistoria = "pendente"
							}
							demandaApi
						}

						// DESLIGA O POP-UP AUTOMÁTICO
						val devePerguntarOtimizacao = false
						processarDadosCarregados(resposta.rota, demandasMescladas, devePerguntarOtimizacao)

						withContext(Dispatchers.IO) {
							salvarDadosNoCache(resposta.rota, demandasMescladas, rotaId)
						}
					}
				} catch (e: Exception) {
					Log.e("RotaDetalheActivity", "Erro ao processar JSON: ${e.message}", e)
				}
			},
			{ error ->
				Log.w("RotaDetalheActivity", "Erro de Rede (Volley): ${error.message}", error)
				if (demandasCarregadas.isEmpty()) {
					supportActionBar?.title = "Erro: Sem conexão"
					Toast.makeText(this, "Falha na rede e sem dados locais.", Toast.LENGTH_LONG).show()
				}
			}
		)
		queue.add(jsonObjectRequest)
	}

	private fun processarDadosCarregados(rota: Rota, demandas: List<Demanda>, devePerguntarOtimizacao: Boolean) {
		rotaCompleta = rota
		demandasCarregadas = demandas.toMutableList()

		filtrarDemandasPendentes()

		supportActionBar?.title = rotaCompleta?.nome ?: "Detalhes da Rota"

		// Otimização automática (agora desativada)
		if (demandasPendentes.isNotEmpty() && devePerguntarOtimizacao) {
			checkLocationPermissionAndAsk()
		}
	}

	// --- LÓGICA DE OTIMIZAÇÃO (AGORA MANUAL) ---

	private fun checkLocationPermissionAndAsk() {
		when {
			ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.ACCESS_FINE_LOCATION
			) == PackageManager.PERMISSION_GRANTED -> {
				getUsersCurrentLocationAndAsk()
			}
			shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
				locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
			else -> {
				locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
		}
	}

	@SuppressLint("MissingPermission")
	private fun getUsersCurrentLocationAndAsk() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return
		}

		fusedLocationClient.lastLocation
			.addOnSuccessListener { location: Location? ->
				if (location != null) {
					this.localizacaoUsuario = location
					showOptimizeDialog(location)
				} else {
					showOptimizeDialog(null)
				}
			}
			.addOnFailureListener {
				showOptimizeDialog(null)
			}
	}

	private fun showOptimizeDialog(userLocation: Location?) {
		if (isFinishing || isDestroyed) {
			return
		}

		val message = if (userLocation != null) {
			"Deseja otimizar as ${demandasPendentes.size} paradas pendentes a partir da sua localização atual?"
		} else {
			"Não foi possível obter sua localização. Deseja otimizar as ${demandasPendentes.size} paradas pendentes a partir da base?"
		}

		val builder = AlertDialog.Builder(this)
		builder.setTitle("Otimizar Rota?")
		builder.setMessage(message)

		builder.setPositiveButton("Otimizar Rota") { dialog, which ->
			optimizeRouteFromLocation(userLocation)
		}

		builder.setNegativeButton("Manter Atual") { dialog, which ->
			dialog.dismiss()
		}

		builder.show()
	}

	private fun optimizeRouteFromLocation(userLocation: Location?) {
		Log.d("RotaDetalheActivity", "Iniciando otimização manual...")

		val demandaIds = demandasPendentes.map { it.id }

		if (demandaIds.isEmpty()) {
			Toast.makeText(this, "Nenhuma demanda pendente para otimizar.", Toast.LENGTH_SHORT).show()
			return
		}

		Toast.makeText(this, "Otimizando ${demandaIds.size} paradas...", Toast.LENGTH_SHORT).show()
		val url = "$API_BASE_URL/rotas/optimize"
		val requestBody = JSONObject()
		requestBody.put("demandaIds", JSONArray(demandaIds))

		if (userLocation != null) {
			val locationJson = JSONObject()
			locationJson.put("latitude", userLocation.latitude)
			locationJson.put("longitude", userLocation.longitude)
			requestBody.put("userLocation", locationJson)
		}

		val jsonObjectRequest = JsonObjectRequest(
			Request.Method.POST, url, requestBody,
			{ response ->
				Log.d("RotaDetalheActivity", "Otimização recebida: $response")
				try {
					val optimizedDemandasJson = response.getJSONArray("optimizedDemands")

					val startPointJson = response.optJSONObject("startPoint")
					if (startPointJson != null) {
						val startLocation = Location("backend").apply {
							latitude = startPointJson.getDouble("lat")
							longitude = startPointJson.getDouble("lng")
						}
						this.localizacaoUsuario = startLocation
					}

					val gson = Gson()
					val sortedDemandaList = mutableListOf<Demanda>()

					for (i in 0 until optimizedDemandasJson.length()) {
						val demandaObj = optimizedDemandasJson.getJSONObject(i).toString()
						val demanda = gson.fromJson(demandaObj, Demanda::class.java)
						demanda.status_vistoria = "pendente"
						sortedDemandaList.add(demanda)
					}

					val demandasConcluidas = demandasCarregadas.filter {
						!it.status_vistoria.equals("pendente", ignoreCase = true)
					}

					demandasCarregadas = (sortedDemandaList + demandasConcluidas).toMutableList()
					demandasPendentes = sortedDemandaList.toMutableList()
					totalParadasInicial = demandasPendentes.size

					atualizarUI()
					Toast.makeText(this, "Rota otimizada!", Toast.LENGTH_SHORT).show()

					lifecycleScope.launch(Dispatchers.IO) {
						salvarDadosNoCache(this@RotaDetalheActivity.rotaCompleta!!, demandasCarregadas, this@RotaDetalheActivity.rotaId)
					}
				} catch (e: Exception) {
					Log.e("RotaDetalheActivity", "Erro ao processar JSON da otimização", e)
					Toast.makeText(this, "Falha ao ler otimização.", Toast.LENGTH_SHORT).show()
				}
			},
			{ error ->
				Log.e("RotaDetalheActivity", "Erro de Rede (Volley) ao otimizar.", error)
				Toast.makeText(this, "Falha de rede ao otimizar.", Toast.LENGTH_SHORT).show()
			}
		)
		Volley.newRequestQueue(this).add(jsonObjectRequest)
	}

	// --- LÓGICA DO BANCO DE DADOS ---

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
				Log.d("RotaDetalheActivity", "Salvando ${demandas.size} demandas (TODAS) no cache para rota $rotaId...")
				db.rotaDao().insertRota(rota)
				db.demandaDao().clearDemandasDaRota(rotaId)
				demandas.forEach { it.rotaId = rotaId }
				db.demandaDao().insertAll(demandas)
				Log.d("RotaDetalheActivity", "Cache salvo com sucesso.")
			} catch (e: Exception) {
				Log.e("RotaDetalheActivity", "Erro ao salvar dados no cache", e)
			}
		}
	}

	// --- LÓGICA DA UI (AÇÕES E CICLO DE VIDA) ---

	/**
	 * ADICIONADO: Cria o menu de 3 pontinhos na toolbar
	 */
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.rota_detalhe_menu, menu)
		return true
	}

	/**
	 * ADICIONADO: Lida com o clique no item de menu (Otimizar)
	 */
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_optimize -> {
				// O usuário clicou em "Otimizar Rota"
				Log.d("RotaDetalheActivity", "Otimização manual acionada.")
				// Inicia o fluxo de verificação de permissão
				checkLocationPermissionAndAsk()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}

	/**
	 * Atualiza a UI (Card, Mapa, Botões) com base no estado atual de 'demandasPendentes'.
	 */
	private fun atualizarUI() {
		// 1. Lógica de Rota Concluída (baseado na lista de PENDENTES)
		if (demandasPendentes.isEmpty()) {
			// Rota Concluída
			tituloProximaParada.text = "Rota Concluída"
			enderecoProximaParada.text = "Não há mais paradas pendentes nesta rota."
			descricaoProximaParada.visibility = View.GONE
			btnIniciarVistoria.visibility = View.GONE
			fabIniciarRota.visibility = View.GONE
			cardProximaVistoria.visibility = View.VISIBLE

		} else {
			// Ainda há paradas pendentes
			proximaDemanda = demandasPendentes[0]

			val numeroParadaAtual = 1
			tituloProximaParada.text = "Próxima Parada ($numeroParadaAtual de $totalParadasInicial)"

			// Preenche o Card
			enderecoProximaParada.text = "${proximaDemanda?.logradouro ?: "End."} ${proximaDemanda?.numero ?: ""} - ${proximaDemanda?.bairro ?: ""}"
			descricaoProximaParada.text = proximaDemanda?.descricao ?: "Sem descrição."
			descricaoProximaParada.visibility = View.VISIBLE

			cardProximaVistoria.visibility = View.VISIBLE
			fabIniciarRota.visibility = View.VISIBLE
			btnIniciarVistoria.visibility = View.VISIBLE
		}

		// 3. Preenche o Mapa (mostra apenas PENDENTES)
		val mapController = mapView.controller
		mapView.overlays.clear()

		localizacaoUsuario?.let { loc ->
			val userPoint = GeoPoint(loc.latitude, loc.longitude)
			val userMarker = Marker(mapView)
			userMarker.position = userPoint
			userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
			userMarker.title = "Ponto de Partida"
			mapView.overlays.add(userMarker)
		}

		// Adiciona marcadores para as demandas PENDENTES
		for ((index, demanda) in demandasPendentes.withIndex()) {
			val coords = demanda.geom?.coordinates
			if (coords != null && coords.size == 2) {
				val point = GeoPoint(coords[1], coords[0])
				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				val displayIndex = index + 1
				marker.title = "Parada $displayIndex: ${demanda.logradouro}"
				marker.icon = getDrawable(if (index == 0) R.drawable.ic_marker_green else R.drawable.ic_marker_blue)
				mapView.overlays.add(marker)
			}
		}

		// Centraliza o mapa
		val pontoFoco: GeoPoint
		if (localizacaoUsuario != null) {
			pontoFoco = GeoPoint(localizacaoUsuario!!.latitude, localizacaoUsuario!!.longitude)
		} else if (proximaDemanda?.geom?.coordinates != null) {
			pontoFoco = GeoPoint(proximaDemanda!!.geom!!.coordinates[1], proximaDemanda!!.geom!!.coordinates[0])
		} else {
			pontoFoco = GeoPoint(-29.8608, -51.1789) // Fallback
		}

		mapController.setZoom(15.0)
		mapController.setCenter(pontoFoco)
		mapView.invalidate()
	}

	private fun iniciarRotaGoogleMaps() {
		if (proximaDemanda == null) {
			Toast.makeText(this, "Nenhuma demanda pendente para navegar.", Toast.LENGTH_SHORT).show()
			return
		}
		val coords = proximaDemanda?.geom?.coordinates
		if (coords != null && coords.size == 2) {
			val gmmIntentUri = Uri.parse("google.navigation:q=${coords[1]},${coords[0]}")
			val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
			mapIntent.setPackage("com.google.android.apps.maps")
			if (mapIntent.resolveActivity(packageManager) != null) {
				startActivity(mapIntent)
			} else {
				Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
			}
		} else {
			Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
		}
	}

	/**
	 * Abre a tela de detalhe para a PRÓXIMA demanda pendente.
	 */
	private fun abrirDetalheProximaDemanda() {
		if (proximaDemanda == null) return
		val intent = Intent(this, DemandaDetalheActivity::class.java).apply {
			putExtra("DEMANDA_EXTRA", proximaDemanda)
		}
		vistoriaLauncher.launch(intent)
	}

	/**
	 * Abre a nova tela de lista
	 */
	private fun abrirListaDeDemandas() {
		val intent = Intent(this, DemandaListActivity::class.java).apply {
			putExtra("ROTA_ID", rotaId)
		}
		demandaListLauncher.launch(intent)
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