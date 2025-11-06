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

	// --- Variáveis de Localização ---
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private var localizacaoUsuario: Location? = null

	/**
	 * Launcher para pedir permissão de localização
	 */
	private val locationPermissionRequest = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			// Permissão concedida, tenta pegar a localização e perguntar
			Log.d("RotaDetalheActivity", "Permissão de localização CONCEDIDA.")
			getUsersCurrentLocationAndAsk()
		} else {
			// Permissão negada, o app funciona sem otimizar (usará a base)
			Log.d("RotaDetalheActivity", "Permissão de localização NEGADA.")
			Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
			// Pergunta mesmo assim, mas sem a localização
			showOptimizeDialog(null)
		}
	}

	/**
	 * Launcher para a tela de Detalhe.
	 * Ao concluir, também remove a demanda do banco de dados local.
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

		// Inicializa o FusedLocationClient
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
				processarDadosCarregados(rotaLocal, demandasLocais, false) // Não otimiza no carreg. do cache
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
					// E inicia a checagem de otimização
					processarDadosCarregados(resposta.rota, resposta.demandas, true)

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
	 * Lógica centralizada para preencher as variáveis e INICIAR a checagem de localização.
	 */
	private fun processarDadosCarregados(rota: Rota, demandas: List<Demanda>, devePerguntarOtimizacao: Boolean) {
		rotaCompleta = rota
		listaDemandas = demandas.toMutableList()
		totalParadasInicial = listaDemandas.size
		supportActionBar?.title = rotaCompleta?.nome ?: "Detalhes da Rota"

		atualizarUI() // Atualiza a UI com a rota original

		// --- MODIFICADO: Inicia o fluxo de otimização ---
		// Só pergunta se a rota ainda tiver paradas e se for o 'load' da rede
		if (listaDemandas.isNotEmpty() && devePerguntarOtimizacao) {
			checkLocationPermissionAndAsk()
		}
	}

	// --- FUNÇÕES ADICIONADAS: Lógica de Localização e Otimização ---

	/**
	 * PASSO 1: Verifica se o app tem permissão.
	 */
	private fun checkLocationPermissionAndAsk() {
		when {
			ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.ACCESS_FINE_LOCATION
			) == PackageManager.PERMISSION_GRANTED -> {
				// Permissão já concedida. Pega a localização e pergunta.
				getUsersCurrentLocationAndAsk()
			}
			shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
				// (Opcional) Mostra um dialog explicando por que a permissão é necessária
				locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
			else -> {
				// Pede a permissão pela primeira vez
				locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
		}
	}

	/**
	 * PASSO 2: Pega a localização do usuário (somente se a permissão foi dada).
	 */
	@SuppressLint("MissingPermission") // <-- Suprime o aviso (já checamos a permissão)
	private fun getUsersCurrentLocationAndAsk() {
		// Checagem de segurança (embora já tenhamos checado antes)
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return
		}

		fusedLocationClient.lastLocation
			.addOnSuccessListener { location: Location? ->
				if (location != null) {
					Log.d("RotaDetalheActivity", "Localização encontrada: ${location.latitude}, ${location.longitude}")
					this.localizacaoUsuario = location
					// PASSO 3: Mostra o Dialog de pergunta
					showOptimizeDialog(location)
				} else {
					Log.w("RotaDetalheActivity", "Não foi possível obter a localização (pode estar nula).")
					// Chama o dialog mesmo com localização nula
					showOptimizeDialog(null)
				}
			}
			.addOnFailureListener {
				Log.e("RotaDetalheActivity", "Falha ao obter localização", it)
				// Chama o dialog mesmo com localização nula
				showOptimizeDialog(null)
			}
	}

	/**
	 * PASSO 3: Mostra o AlertDialog para o usuário.
	 */
	private fun showOptimizeDialog(userLocation: Location?) {
		if (isFinishing || isDestroyed) { // Evita crash se a activity fechar
			return
		}

		// Mensagem muda se o GPS falhou
		val message = if (userLocation != null) {
			"Deseja otimizar a rota a partir da sua localização atual?"
		} else {
			"Não foi possível obter sua localização. Deseja otimizar a rota a partir da base?"
		}

		val builder = AlertDialog.Builder(this)
		builder.setTitle("Otimizar Rota?")
		builder.setMessage(message)

		// Botão "Otimizar Rota"
		builder.setPositiveButton("Otimizar Rota") { dialog, which ->
			Log.d("RotaDetalheActivity", "Usuário escolheu OTIMIZAR.")
			// PASSO 4: Chama a função de otimização (passando a localização ou null)
			optimizeRouteFromLocation(userLocation)
		}

		// Botão "Manter Atual"
		builder.setNegativeButton("Manter Atual") { dialog, which ->
			Log.d("RotaDetalheActivity", "Usuário escolheu MANTER a rota.")
			dialog.dismiss()
		}

		builder.show()
	}

	/**
	 * PASSO 4: Onde a mágica da otimização acontece.
	 * ENVIA `demandaIds` E `userLocation` (opcional) PARA O BACKEND.
	 */
	private fun optimizeRouteFromLocation(userLocation: Location?) {
		Log.d("RotaDetalheActivity", "Iniciando otimização (usando backend)...")
		Toast.makeText(this, "Otimizando rota, aguarde...", Toast.LENGTH_SHORT).show()

		// 1. Coletar IDs das Demandas
		val demandaIds = listaDemandas.map { it.id }

		// Se não houver demandas, não faz nada
		if (demandaIds.isEmpty()) {
			Log.w("RotaDetalheActivity", "Nenhuma demanda para otimizar.")
			return
		}

		// 2. Chamar a API (com Volley)
		val url = "$API_BASE_URL/rotas/optimize"

		// Monta o corpo da requisição
		val requestBody = JSONObject()
		requestBody.put("demandaIds", JSONArray(demandaIds))

		// Adiciona a localização do usuário APENAS SE ELA EXISTIR
		if (userLocation != null) {
			val locationJson = JSONObject()
			locationJson.put("latitude", userLocation.latitude)
			locationJson.put("longitude", userLocation.longitude)
			requestBody.put("userLocation", locationJson)
			Log.d("RotaDetalheActivity", "Enviando localização do usuário para o backend.")
		} else {
			Log.d("RotaDetalheActivity", "Sem localização do usuário. Backend usará o padrão.")
		}

		val jsonObjectRequest = JsonObjectRequest(
			Request.Method.POST, url, requestBody,
			{ response ->
				// 3. Receber Resposta
				Log.d("RotaDetalheActivity", "Otimização recebida: $response")
				try {
					val optimizedDemandsJson = response.getJSONArray("optimizedDemands")

					// Ler o ponto de partida que o backend USOU (seja o do usuário ou a base)
					val startPointJson = response.optJSONObject("startPoint")
					if (startPointJson != null) {
						// Atualiza 'localizacaoUsuario' para centralizar o mapa corretamente
						val startLocation = Location("backend").apply { // O provedor "backend" é apenas um nome
							latitude = startPointJson.getDouble("lat")
							longitude = startPointJson.getDouble("lng")
						}
						this.localizacaoUsuario = startLocation // Atualiza para o foco do mapa
					}

					// 4. Remapear a Rota
					val gson = Gson()
					val sortedDemandaList = mutableListOf<Demanda>()

					for (i in 0 until optimizedDemandsJson.length()) {
						val demandaObj = optimizedDemandsJson.getJSONObject(i).toString()
						// Usamos o Gson para converter o objeto JSON de volta para a classe Demanda
						val demanda = gson.fromJson(demandaObj, Demanda::class.java)
						sortedDemandaList.add(demanda)
					}

					// 5. Atualizar a UI
					this.listaDemandas = sortedDemandaList
					// Reinicia o contador de paradas
					this.totalParadasInicial = this.listaDemandas.size

					atualizarUI() // Redesenha o mapa e o card com a nova rota
					Toast.makeText(this, "Rota otimizada!", Toast.LENGTH_SHORT).show()

					// Opcional: Atualizar o cache do banco com a nova ordem
					lifecycleScope.launch(Dispatchers.IO) {
						// Passamos a rotaCompleta (que não mudou) e a nova lista de demandas
						salvarDadosNoCache(this@RotaDetalheActivity.rotaCompleta!!, sortedDemandaList, this@RotaDetalheActivity.rotaId)
					}

				} catch (e: Exception) {
					Log.e("RotaDetalheActivity", "Erro ao processar JSON da otimização", e)
					Toast.makeText(this, "Falha ao ler otimização.", Toast.LENGTH_SHORT).show()
				}
			},
			{ error ->
				val statusCode = error.networkResponse?.statusCode
				Log.e("RotaDetalheActivity", "Erro de Rede (Volley) ao otimizar. Status: $statusCode", error)
				Toast.makeText(this, "Falha de rede ao otimizar.", Toast.LENGTH_SHORT).show()
			}
		)
		// Adiciona a requisição à fila do Volley
		Volley.newRequestQueue(this).add(jsonObjectRequest)
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
		// Se o número da parada for 0 ou negativo (após otimização), mostramos 1
		val displayParadaAtual = if (numeroParadaAtual <= 0) 1 else numeroParadaAtual
		tituloProximaParada.text = "Próxima Parada ($displayParadaAtual de $totalParadasInicial)"

		enderecoProximaParada.text = "${proximaDemanda?.logradouro ?: "End."} ${proximaDemanda?.numero ?: ""} - ${proximaDemanda?.bairro ?: ""}"
		descricaoProximaParada.text = proximaDemanda?.descricao ?: "Sem descrição."

		cardProximaVistoria.visibility = View.VISIBLE
		fabIniciarRota.visibility = View.VISIBLE
		btnIniciarVistoria.visibility = View.VISIBLE

		// 2. Preenche o Mapa
		val mapController = mapView.controller
		mapView.overlays.clear()

		// Adiciona um marcador para o PONTO DE PARTIDA (seja do usuário ou da base)
		// 'localizacaoUsuario' agora guarda o ponto de partida que o backend usou
		localizacaoUsuario?.let { loc ->
			val userPoint = GeoPoint(loc.latitude, loc.longitude)
			val userMarker = Marker(mapView)
			userMarker.position = userPoint
			userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
			userMarker.title = "Ponto de Partida"
			// (Você pode querer um ícone diferente para o usuário/base)
			// userMarker.icon = getDrawable(R.drawable.ic_user_location)
			mapView.overlays.add(userMarker)
		}

		// Adiciona marcadores para as demandas
		for ((index, demanda) in listaDemandas.withIndex()) {
			val coords = demanda.geom?.coordinates
			if (coords != null && coords.size == 2) {
				val point = GeoPoint(coords[1], coords[0]) // Lat, Lng
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
		val pontoFoco = if (localizacaoUsuario != null) {
			// Foca no ponto de partida (que agora é dinâmico)
			GeoPoint(localizacaoUsuario!!.latitude, localizacaoUsuario!!.longitude)
		} else if (proximaDemanda?.geom?.coordinates != null) {
			GeoPoint(proximaDemanda!!.geom!!.coordinates[1], proximaDemanda!!.geom!!.coordinates[0])
		} else {
			GeoPoint(-29.8608, -51.1789) // Fallback
		}

		mapController.setZoom(15.0)
		mapController.setCenter(pontoFoco)
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
				Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
			}
		} else {
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