package com.dexheimer.treeinspectorandroid

// Imports de Permissão e Localização

// Imports de Atividades e Resultados

// Imports de UI (Layout)

// Import para o 'edit' do SharedPreferences

// Imports de Rede (Volley e GSON)

// Imports do Google Maps

// Import da Biblioteca de Utilitários (para decodificar o polyline)
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.maps.android.PolyUtil

class RotaDetalheActivity : AppCompatActivity(), OnMapReadyCallback {

	// !! IMPORTANTE: Confirme se este é o IP da sua rede local (Wi-Fi) !!
	private val API_URL_BASE = "https://tree-inspector-v5.vercel.app/api/rotas"
	private val LOG_TAG = "RotaDetalheActivity"

	// Constantes para salvar o progresso
	private val ROUTE_PROGRESS_PREFS = "RouteProgressPrefs"
	private val PROGRESS_KEY_PREFIX = "progress_rota_"

	// Componentes de UI
	private lateinit var textViewNomeRota: TextView
	private lateinit var textViewResponsavel: TextView
	private lateinit var textViewStatus: TextView
	private lateinit var progressBarMap: ProgressBar
	private lateinit var mapFragmentContainer: View
	private lateinit var panelNavegacao: LinearLayout
	private lateinit var textProximaParadaContagem: TextView
	private lateinit var textProximaParadaEndereco: TextView
	private lateinit var textProximaParadaDetalhe: TextView
	private lateinit var buttonNavegar: Button
	private lateinit var buttonIniciarVistoria: Button

	// Mapa e Dados
	private var googleMap: GoogleMap? = null
	private var rotaDetalhe: RotaDetalhe? = null
	private var currentDemandIndex: Int = 0 // O índice da demanda atual

	// Launcher para a Permissão de Localização
	private val locationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			Log.d(LOG_TAG, "Permissão de localização concedida.")
			ativarLocalizacaoNoMapa()
		} else {
			Log.w(LOG_TAG, "Permissão de localização negada.")
			Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
		}
	}

	// Launcher para a VistoriaActivity (que salva o progresso)
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {

			// --- LÓGICA DE SALVAMENTO ---
			Log.d(LOG_TAG, "Vistoria do índice $currentDemandIndex finalizada. Salvando progresso...")

			val prefs = getSharedPreferences(ROUTE_PROGRESS_PREFS, MODE_PRIVATE)
			val progressKey = "$PROGRESS_KEY_PREFIX${rotaDetalhe!!.rota.id}"

			// Salva o índice que ACABOU de ser completado
			prefs.edit {
				putInt(progressKey, currentDemandIndex)
				apply() // Salva em background
			}
			// ---------------------------

			// Avança para a próxima demanda
			currentDemandIndex++
			prepararProximaDemanda()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_rota_detalhe)

		// Pega os dados passados (ID e Nome)
		val rotaId = intent.getIntExtra("ROTA_ID", -1)
		val rotaNome = intent.getStringExtra("ROTA_NOME") ?: "Detalhes da Rota"
		title = rotaNome

		bindViews()
		textViewNomeRota.text = rotaNome

		if (rotaId == -1) {
			Toast.makeText(this, "Erro: ID da Rota inválido", Toast.LENGTH_LONG).show()
			finish()
			return
		}

		setupClickListeners()

		// Inicia o mapa
		val mapFragment = supportFragmentManager
			.findFragmentById(R.id.mapFragment) as SupportMapFragment
		mapFragment.getMapAsync(this) // Isso chama 'onMapReady'

		// Busca os dados da rota no backend
		fetchDetalhesRota(rotaId)
	}

	// ===================================================================
	// --- FUNÇÃO OBRIGATÓRIA (que corrige o erro 'onMapReady') ---
	// ===================================================================
	override fun onMapReady(map: GoogleMap) {
		googleMap = map
		Log.d(LOG_TAG, "Mapa pronto.")

		// 1. Pede permissão para mostrar o "ponto azul"
		checarPermissaoLocalizacao()

		// 2. Se os dados da rota já chegaram, desenha a rota no mapa
		rotaDetalhe?.let { desenharMapa(it.demandas, it.geometry) }
	}
	// ===================================================================


	// Função para vincular os componentes da UI (findView...)
	private fun bindViews() {
		textViewNomeRota = findViewById(R.id.textViewNomeRota)
		textViewResponsavel = findViewById(R.id.textViewResponsavel)
		textViewStatus = findViewById(R.id.textViewStatus)
		progressBarMap = findViewById(R.id.progressBarMap)
		mapFragmentContainer = findViewById(R.id.mapFragment)
		panelNavegacao = findViewById(R.id.panelNavegacao)
		textProximaParadaContagem = findViewById(R.id.textProximaParadaContagem)
		textProximaParadaEndereco = findViewById(R.id.textProximaParadaEndereco)
		textProximaParadaDetalhe = findViewById(R.id.textProximaParadaDetalhe)
		buttonNavegar = findViewById(R.id.buttonNavegar)
		buttonIniciarVistoria = findViewById(R.id.buttonIniciarVistoria)
	}

	// Função para configurar os cliques dos botões
	private fun setupClickListeners() {
		buttonNavegar.setOnClickListener {
			navegarParaDemandaAtual()
		}
		buttonIniciarVistoria.setOnClickListener {
			iniciarVistoriaDemandaAtual()
		}
	}

	// Função para checar se o app TEM permissão de localização
	private fun checarPermissaoLocalizacao() {
		when {
			ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.ACCESS_FINE_LOCATION
			) == PackageManager.PERMISSION_GRANTED -> {
				// Permissão já concedida, ativa o "ponto azul"
				ativarLocalizacaoNoMapa()
			}
			shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
				// Pedimos direto (poderia ter um popup de explicação)
				locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
			else -> {
				// Pede a permissão
				locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
		}
	}

	// Função para ativar o "ponto azul"
	private fun ativarLocalizacaoNoMapa() {
		if (googleMap == null) return
		try {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) {

				googleMap?.isMyLocationEnabled = true // O "Ponto Azul"
				googleMap?.uiSettings?.isMyLocationButtonEnabled = true // O botão de centralizar
			}
		} catch (e: SecurityException) {
			Log.e(LOG_TAG, "Erro de segurança ao ativar localização: ${e.message}")
		}
	}

	// Função para buscar os dados da Rota (com carregamento de progresso)
	private fun fetchDetalhesRota(id: Int) {
		val url = "$API_URL_BASE/$id"
		progressBarMap.visibility = View.VISIBLE
		mapFragmentContainer.visibility = View.INVISIBLE

		val queue = Volley.newRequestQueue(this)
		val jsonObjectRequest = JsonObjectRequest(
			Request.Method.GET, url, null,
			{ response ->
				Log.d(LOG_TAG, "JSON Detalhes Recebido.")
				try {
					val gson = Gson()
					this.rotaDetalhe = gson.fromJson(response.toString(), RotaDetalhe::class.java)

					textViewResponsavel.text = "Responsável: ${rotaDetalhe!!.rota.responsavel ?: "N/D"}"
					textViewStatus.text = "Status: ${rotaDetalhe!!.rota.status ?: "N/D"}"

					googleMap?.let { desenharMapa(rotaDetalhe!!.demandas, rotaDetalhe!!.geometry) }

					progressBarMap.visibility = View.GONE
					mapFragmentContainer.visibility = View.VISIBLE

					if (rotaDetalhe!!.demandas.isNotEmpty()) {

						// --- LÓGICA DE CARREGAMENTO DE PROGRESSO ---
						val prefs = getSharedPreferences(ROUTE_PROGRESS_PREFS, MODE_PRIVATE)
						val progressKey = "$PROGRESS_KEY_PREFIX${rotaDetalhe!!.rota.id}"

						val lastCompletedIndex = prefs.getInt(progressKey, -1) // -1 se não houver
						currentDemandIndex = lastCompletedIndex + 1 // O próximo a fazer

						Log.d(LOG_TAG, "Progresso carregado. Último salvo: $lastCompletedIndex. Iniciando em: $currentDemandIndex")

						prepararProximaDemanda() // Prepara o painel
						panelNavegacao.visibility = View.VISIBLE // Mostra o painel
					}

				} catch (e: Exception) {
					// Erro de GSON (JSON incompatível) ou outro
					Log.e(LOG_TAG, "Erro no GSON (parsing): ${e.message}", e)
					showError()
				}
			},
			{ error ->
				// Erro de Rede (Servidor 500, sem conexão, etc)
				Log.e(LOG_TAG, "Erro de Rede (Volley): ${error.message}", error)
				showError()
			}
		)
		queue.add(jsonObjectRequest)
	}

	// Função que desenha a ROTA OSRM e os MARCADORES
	private fun desenharMapa(demandas: List<Demanda>, geometry: String?) {
		val map = googleMap ?: return

		// 1. Coletar os pontos dos MARCADORES (alfinetes)
		val pontosDosMarcadores = demandas.mapNotNull { demanda ->
			demanda.geom?.coordinates?.let { coords ->
				if (coords.size >= 2) LatLng(coords[1], coords[0]) else null // [lon, lat] -> LatLng(lat, lon)
			}
		}

		if (pontosDosMarcadores.isEmpty()) {
			Toast.makeText(this, "Esta rota não possui pontos no mapa.", Toast.LENGTH_SHORT).show()
			return
		}

		val boundsBuilder = LatLngBounds.Builder()

		// 2. Desenhar os MARCADORES (alfinetes)
		pontosDosMarcadores.forEachIndexed { index, latLng ->
			map.addMarker(
				MarkerOptions()
					.position(latLng)
					.title("Ponto ${index + 1}")
			)
			boundsBuilder.include(latLng) // Adiciona marcador ao zoom
		}

		// 3. Desenhar a ROTA (a linha azul das ruas)
		if (geometry != null && geometry.isNotEmpty()) {
			try {
				// Decodifica o 'geometry' (polyline) vindo do OSRM
				val pontosDaRota: List<LatLng> = PolyUtil.decode(geometry)

				if (pontosDaRota.isNotEmpty()) {
					val polylineOptions = PolylineOptions()
						.color(Color.BLUE)
						.width(12f) // Linha grossa
						.addAll(pontosDaRota)
					map.addPolyline(polylineOptions)
				}
			} catch (e: Exception) {
				Log.e(LOG_TAG, "Falha ao decodificar polyline: ${e.message}")
			}
		} else {
			Log.w(LOG_TAG, "Geometria OSRM não encontrada. Mostrando apenas marcadores.")
		}

		// 4. Mover a Câmera (Zoom automático)
		val bounds = boundsBuilder.build()
		val padding = 100 // pixels
		try {
			map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
		} catch (e: IllegalStateException) {
			Log.e(LOG_TAG, "Erro ao mover câmera (mapa não pronto): ${e.message}")
			map.setOnMapLoadedCallback {
				map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
			}
		}
	}

	// Função que atualiza o painel inferior com a PRÓXIMA demanda
	private fun prepararProximaDemanda() {
		val demandas = rotaDetalhe?.demandas
		if (demandas == null || demandas.isEmpty()) return

		if (currentDemandIndex < demandas.size) {
			// Ainda há demandas
			val proximaDemanda = demandas[currentDemandIndex]

			textProximaParadaContagem.text =
				"PRÓXIMA PARADA (Ponto ${currentDemandIndex + 1} de ${demandas.size})"

			textProximaParadaEndereco.text =
				"${proximaDemanda.logradouro ?: "Endereço não informado"}, ${proximaDemanda.numero ?: ""}"

			textProximaParadaDetalhe.text =
				"Tipo: ${proximaDemanda.tipo_demanda ?: "N/D"} | Bairro: ${proximaDemanda.bairro ?: "N/D"}"

			panelNavegacao.visibility = View.VISIBLE

		} else {
			// Rota Concluída!
			panelNavegacao.visibility = View.GONE
			Toast.makeText(this, "Rota Concluída!", Toast.LENGTH_LONG).show()

			// --- LÓGICA DE LIMPEZA DO PROGRESSO ---
			Log.d(LOG_TAG, "Rota concluída. Limpando progresso salvo.")
			val prefs = getSharedPreferences(ROUTE_PROGRESS_PREFS, MODE_PRIVATE)
			val progressKey = "$PROGRESS_KEY_PREFIX${rotaDetalhe!!.rota.id}"
			prefs.edit {
				remove(progressKey)
				apply()
			}
			// -------------------------------------
		}
	}

	// Função que chama o app Google Maps (externo)
	private fun navegarParaDemandaAtual() {
		val demanda = rotaDetalhe?.demandas?.getOrNull(currentDemandIndex) ?: return

		val latLng = demanda.geom?.coordinates?.let { coords ->
			if (coords.size >= 2) LatLng(coords[1], coords[0]) else null
		}

		if (latLng != null) {
			val gmmIntentUri = Uri.parse("google.navigation:q=${latLng.latitude},${latLng.longitude}")
			val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
			mapIntent.setPackage("com.google.android.apps.maps")

			if (mapIntent.resolveActivity(packageManager) != null) {
				startActivity(mapIntent)
			} else {
				Toast.makeText(this, "Google Maps não instalado.", Toast.LENGTH_SHORT).show()
			}
		} else {
			Toast.makeText(this, "Coordenadas inválidas para esta demanda.", Toast.LENGTH_SHORT).show()
		}
	}

	// Função que chama a tela de Vistoria
	private fun iniciarVistoriaDemandaAtual() {
		val demanda = rotaDetalhe?.demandas?.getOrNull(currentDemandIndex) ?: return
		val intent = Intent(this, VistoriaActivity::class.java)
		intent.putExtra("DEMANDA_EXTRA", demanda)
		vistoriaLauncher.launch(intent)
	}

	// Função para mostrar erro genérico (Falha ao carregar...)
	private fun showError() {
		progressBarMap.visibility = View.GONE
		// Só mostra o erro se o painel de navegação não estiver visível
		// (evita cobrir os dados caso o mapa falhe mas o resto não)
		if(panelNavegacao.visibility != View.VISIBLE) {
			Toast.makeText(this, "Falha ao carregar detalhes da rota", Toast.LENGTH_LONG).show()
		}
	}

} // <-- FIM DA CLASSE RotaDetalheActivity