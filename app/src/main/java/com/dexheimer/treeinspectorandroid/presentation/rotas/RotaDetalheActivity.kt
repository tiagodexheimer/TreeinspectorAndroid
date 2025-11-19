package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.presentation.demandas.DemandaDetalheActivity
import com.dexheimer.treeinspectorandroid.presentation.demandas.DemandaListActivity
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.data.local.AppDatabase
import com.dexheimer.treeinspectorandroid.data.local.Demanda
import com.dexheimer.treeinspectorandroid.data.local.FormularioCache
import com.dexheimer.treeinspectorandroid.data.local.Rota
import com.dexheimer.treeinspectorandroid.data.remote.NetworkClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

	// --- Variáveis de Estado ---
	private var demandasPendentes: MutableList<Demanda> = mutableListOf()
	private var demandasCarregadas: MutableList<Demanda> = mutableListOf()
	private var rotaCompleta: Rota? = null
	private var proximaDemanda: Demanda? = null
	private var totalParadasInicial: Int = 0
	private var geometry: String? = null

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

	// --- Banco de Dados e Location ---
	private lateinit var db: AppDatabase
	private var rotaId: Int = -1
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private var localizacaoUsuario: Location? = null

	// Launcher para Permissão de Localização
	private val locationPermissionRequest = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			getUsersCurrentLocationAndAsk()
		} else {
			Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
		}
	}

	// Launcher para o retorno da Vistoria
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS") ?: return@registerForActivityResult
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1

			if (demandaId != -1 && demandaId == proximaDemanda?.id) {
				atualizarStatusDemanda(demandaId, novoStatus)
			}
		}
	}

	// Launcher para o retorno da Lista
	private val demandaListLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == RESULT_OK) {
			// Recarrega dados pois algo pode ter mudado na lista
			recuperarDadosLocais()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
		setContentView(R.layout.activity_rota_detalhe)

		db = AppDatabase.getInstance(applicationContext)
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

		// Inicializa UI
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

		// Listeners
		fabIniciarRota.setOnClickListener { iniciarRotaGoogleMaps() }
		btnIniciarVistoria.setOnClickListener { abrirDetalheProximaDemanda() }
		btnVerTodasDemandas.setOnClickListener { abrirListaDeDemandas() }

		// Carrega Dados
		rotaId = intent.getIntExtra("ROTA_ID", -1)
		if (rotaId != -1) {
			carregarDadosIniciais(rotaId)
		} else {
			Toast.makeText(this, "Rota inválida", Toast.LENGTH_SHORT).show()
			finish()
		}
	}

	private fun setupToolbarEMapa() {
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Detalhes da Rota"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.setBuiltInZoomControls(true)
		mapView.setMultiTouchControls(true)
	}

	private fun carregarDadosIniciais(rotaId: Int) {
		lifecycleScope.launch {
			// 1. Tenta Cache Local Primeiro
			val rotaLocal = carregarRotaDoCache(rotaId)
			val demandasLocais = carregarDemandasDoCache(rotaId)

			if (rotaLocal != null && demandasLocais.isNotEmpty()) {
				processarDadosCarregados(rotaLocal, demandasLocais)
			}

			// 2. Tenta Rede (Background) para atualizar e baixar formulários
			fetchRotaDetalhesNetwork(rotaId)
		}
	}

	private fun recuperarDadosLocais() {
		lifecycleScope.launch {
			val demandasLocais = carregarDemandasDoCache(rotaId)
			val rotaLocal = carregarRotaDoCache(rotaId)
			if (rotaLocal != null) {
				processarDadosCarregados(rotaLocal, demandasLocais)
			}
		}
	}

	// --- NETWORKING (RETROFIT) ---
	private suspend fun fetchRotaDetalhesNetwork(rotaId: Int) {
		try {
			// Chamada via Retrofit (Autenticada automaticamente pelo NetworkClient)
			val response = NetworkClient.api.getRotaDetalhes(rotaId)

			if (response.isSuccessful && response.body() != null) {
				val resposta = response.body()!!
				this.geometry = resposta.geometry

				// Mesclar status locais (para não perder vistorias offline pendentes de sync)
				val demandasLocais = withContext(Dispatchers.IO) {
					db.demandaDao().getDemandasDaRota(rotaId)
				}
				val mapaStatusLocal = demandasLocais.associateBy({ it.id }, { it.status_vistoria })

				val demandasApi = resposta.demandas.map { demanda ->
					val statusLocal = mapaStatusLocal[demanda.id]
					// Se localmente já está concluído (ou pendente de envio), mantém o local
					if (statusLocal != null && (statusLocal == "concluido" || statusLocal == "concluido_pendente")) {
						demanda.status_vistoria = statusLocal
					} else {
						// Senão, usa o que veio da API (que deve ser "pendente" ou "concluido" no servidor)
						demanda.status_vistoria = "pendente"
					}
					demanda
				}

				processarDadosCarregados(resposta.rota, demandasApi)

				withContext(Dispatchers.IO) {
					// 1. Salva Rota e Demandas no Cache
					salvarDadosNoCache(resposta.rota, demandasApi, rotaId)

					// 2. --- CACHE DE FORMULÁRIOS PARA USO OFFLINE ---
					val tiposUnicos = demandasApi.mapNotNull { it.tipo_demanda }.distinct()
					val daoForm = db.formularioDao()
					val gson = Gson()

					for (tipo in tiposUnicos) {
						try {
							val formResponse = NetworkClient.api.getFormularioPorTipo(tipo)
							if (formResponse.isSuccessful && formResponse.body() != null) {
								val campos = formResponse.body()!!
								val jsonEstrutura = gson.toJson(campos)
								daoForm.salvarFormulario(FormularioCache(tipo, jsonEstrutura))
								Log.d("RotaDetalhe", "Formulário cacheado: $tipo")
							}
						} catch (e: Exception) {
							Log.e("RotaDetalhe", "Erro ao cachear formulário $tipo", e)
						}
					}
				}
			} else {
				if (response.code() == 401) {
					Toast.makeText(this@RotaDetalheActivity, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
				} else {
					Log.e("RotaDetalhe", "Erro na API: ${response.code()}")
				}
			}
		} catch (e: Exception) {
			Log.e("RotaDetalhe", "Erro de rede: ${e.message}")
			if (demandasCarregadas.isEmpty()) {
				Toast.makeText(this, "Sem conexão e sem dados locais.", Toast.LENGTH_SHORT).show()
			}
		}
	}

	// --- PROCESSAMENTO E UI ---
	private fun processarDadosCarregados(rota: Rota, demandas: List<Demanda>) {
		rotaCompleta = rota
		demandasCarregadas = demandas.toMutableList()
		filtrarDemandasPendentes()
		supportActionBar?.title = rota.nome
	}

	private fun filtrarDemandasPendentes() {
		// Filtra tudo que NÃO está concluído (inclui "pendente")
		// Se quiser mostrar na rota apenas o que falta fazer:
		demandasPendentes = demandasCarregadas.filter {
			it.status_vistoria.equals("pendente", ignoreCase = true)
		}.toMutableList()

		// Ordena pela ordem original
		demandasPendentes.sortBy { demanda ->
			demandasCarregadas.indexOfFirst { it.id == demanda.id }
		}

		totalParadasInicial = demandasPendentes.size
		atualizarUI()
	}

	private fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				db.demandaDao().updateStatus(demandaId, novoStatus)
			}
			demandasCarregadas.find { it.id == demandaId }?.status_vistoria = novoStatus
			filtrarDemandasPendentes()
		}
	}

	private fun atualizarUI() {
		if (demandasPendentes.isEmpty()) {
			tituloProximaParada.text = "Rota Concluída"
			enderecoProximaParada.text = "Todas as vistorias foram realizadas."
			descricaoProximaParada.visibility = View.GONE
			btnIniciarVistoria.visibility = View.GONE
			fabIniciarRota.visibility = View.GONE
			cardProximaVistoria.visibility = View.VISIBLE
		} else {
			proximaDemanda = demandasPendentes[0]
			tituloProximaParada.text = "Próxima Parada"

			val end = StringBuilder()
			proximaDemanda?.logradouro?.let { end.append(it) }
			proximaDemanda?.numero?.let { end.append(", $it") }
			proximaDemanda?.bairro?.let { end.append(" - $it") }

			enderecoProximaParada.text = end.toString()
			descricaoProximaParada.text = proximaDemanda?.descricao ?: ""
			descricaoProximaParada.visibility = View.VISIBLE

			cardProximaVistoria.visibility = View.VISIBLE
			fabIniciarRota.visibility = View.VISIBLE
			btnIniciarVistoria.visibility = View.VISIBLE
		}
		atualizarMapa()
	}

	private fun atualizarMapa() {
		mapView.overlays.clear()

		for ((index, demanda) in demandasPendentes.withIndex()) {
			if (demanda.lat != null && demanda.lng != null) {
				val point = GeoPoint(demanda.lat!!, demanda.lng!!)
				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				marker.title = "Parada ${index + 1}"

				val iconRes = if(index == 0) R.drawable.ic_marker_green else R.drawable.ic_marker_blue
				marker.icon = ContextCompat.getDrawable(this, iconRes)

				mapView.overlays.add(marker)
			}
		}

		// Centraliza
		if (proximaDemanda?.lat != null && proximaDemanda?.lng != null) {
			val foco = GeoPoint(proximaDemanda!!.lat!!, proximaDemanda!!.lng!!)
			mapView.controller.setZoom(18.0)
			mapView.controller.setCenter(foco)
		} else if (localizacaoUsuario != null) {
			val foco = GeoPoint(localizacaoUsuario!!.latitude, localizacaoUsuario!!.longitude)
			mapView.controller.setZoom(15.0)
			mapView.controller.setCenter(foco)
		}

		mapView.invalidate()
	}

	// --- CACHE (ROOM) ---
	private suspend fun carregarRotaDoCache(id: Int) = withContext(Dispatchers.IO) {
		db.rotaDao().getRotaById(id)
	}

	private suspend fun carregarDemandasDoCache(id: Int) = withContext(Dispatchers.IO) {
		db.demandaDao().getDemandasDaRota(id)
	}

	private suspend fun salvarDadosNoCache(rota: Rota, demandas: List<Demanda>, rId: Int) {
		withContext(Dispatchers.IO) {
			db.rotaDao().insertRota(rota)
			db.demandaDao().clearDemandasDaRota(rId)
			demandas.forEach { it.rotaId = rId }
			db.demandaDao().insertAll(demandas)
		}
	}

	// --- AÇÕES E PERMISSÕES ---

	private fun checkLocationPermissionAndAsk() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			getUsersCurrentLocationAndAsk()
		} else {
			locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
		}
	}

	@SuppressLint("MissingPermission")
	private fun getUsersCurrentLocationAndAsk() {
		fusedLocationClient.lastLocation.addOnSuccessListener { location ->
			this.localizacaoUsuario = location
			Toast.makeText(this, "Localização atualizada", Toast.LENGTH_SHORT).show()
		}
	}

	private fun iniciarRotaGoogleMaps() {
		val lat = proximaDemanda?.lat
		val lng = proximaDemanda?.lng
		if (lat != null && lng != null) {
			val uri = Uri.parse("google.navigation:q=$lat,$lng")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			intent.setPackage("com.google.android.apps.maps")
			if (intent.resolveActivity(packageManager) != null) {
				startActivity(intent)
			} else {
				Toast.makeText(this, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
			}
		} else {
			Toast.makeText(this, "Coordenadas não disponíveis", Toast.LENGTH_SHORT).show()
		}
	}

	private fun abrirDetalheProximaDemanda() {
		if (proximaDemanda == null) return
		val intent = Intent(this, DemandaDetalheActivity::class.java)
		intent.putExtra("DEMANDA_EXTRA", proximaDemanda)
		vistoriaLauncher.launch(intent)
	}

	private fun abrirListaDeDemandas() {
		val intent = Intent(this, DemandaListActivity::class.java)
		intent.putExtra("ROTA_ID", rotaId)
		demandaListLauncher.launch(intent)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.rota_detalhe_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> { finish(); true }
			R.id.action_optimize -> {
				Toast.makeText(this, "Otimização em breve", Toast.LENGTH_SHORT).show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	// Ciclo de vida do mapa & Refresh ao voltar para a tela
	override fun onResume() {
		super.onResume()
		mapView.onResume()
		// Recarrega dados para atualizar status se voltou de outra tela ou background
		if (rotaId != -1) recuperarDadosLocais()
	}

	override fun onPause() { super.onPause(); mapView.onPause() }
}