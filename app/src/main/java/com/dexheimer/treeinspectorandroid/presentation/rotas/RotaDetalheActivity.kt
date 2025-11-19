package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.presentation.demandas.DemandaDetalheActivity
import com.dexheimer.treeinspectorandroid.presentation.demandas.DemandaListActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint // Injeção do Hilt
class RotaDetalheActivity : AppCompatActivity() {

	private val viewModel: RotaDetalheViewModel by viewModels()

	// UI
	private lateinit var toolbar: Toolbar
	private lateinit var mapView: MapView
	private lateinit var fabIniciarRota: FloatingActionButton
	private lateinit var cardProximaVistoria: View
	private lateinit var tituloProximaParada: TextView
	private lateinit var enderecoProximaParada: TextView
	private lateinit var descricaoProximaParada: TextView
	private lateinit var btnIniciarVistoria: Button
	private lateinit var btnVerTodasDemandas: Button

	// Estado Local (apenas para controle de mapa/navegação)
	private var proximaDemanda: Demanda? = null
	private var rotaId: Int = -1

	// Launcher Vistoria
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS")
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1
			if (novoStatus != null && demandaId != -1) {
				viewModel.atualizarStatusDemanda(demandaId, novoStatus)
			}
		}
	}

	// Launcher Lista
	private val demandaListLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			viewModel.carregarDados() // Recarrega se voltou da lista
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
		setContentView(R.layout.activity_rota_detalhe)

		rotaId = intent.getIntExtra("ROTA_ID", -1)

		setupUI()
		observarViewModel()
	}

	private fun setupUI() {
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		mapView = findViewById(R.id.mapView)
		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.setBuiltInZoomControls(true)
		mapView.setMultiTouchControls(true)

		fabIniciarRota = findViewById(R.id.fabIniciarRota)
		cardProximaVistoria = findViewById(R.id.proximaVistoriaCard)
		tituloProximaParada = findViewById(R.id.tituloProximaParada)
		enderecoProximaParada = findViewById(R.id.enderecoProximaParada)
		descricaoProximaParada = findViewById(R.id.descricaoProximaParada)
		btnIniciarVistoria = findViewById(R.id.btnIniciarVistoria)
		btnVerTodasDemandas = findViewById(R.id.btnVerTodasDemandas)

		fabIniciarRota.setOnClickListener { iniciarRotaGoogleMaps() }
		btnIniciarVistoria.setOnClickListener { abrirDetalheProximaDemanda() }
		btnVerTodasDemandas.setOnClickListener { abrirListaDeDemandas() }
	}

	private fun observarViewModel() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					if (state.isLoading) {
						// Mostrar loading...
						tituloProximaParada.text = "Carregando..."
					}

					if (state.error != null) {
						Toast.makeText(this@RotaDetalheActivity, state.error, Toast.LENGTH_LONG).show()
					}

					state.rota?.let {
						supportActionBar?.title = it.nome
					}

					atualizarCardProxima(state.demandasPendentes)
					atualizarMapa(state.demandasPendentes)
				}
			}
		}
	}

	private fun atualizarCardProxima(pendentes: List<Demanda>) {
		if (pendentes.isEmpty()) {
			// Rota Concluída
			tituloProximaParada.text = "Rota Concluída"
			enderecoProximaParada.text = "Todas as vistorias realizadas."
			descricaoProximaParada.visibility = View.GONE
			btnIniciarVistoria.visibility = View.GONE
			fabIniciarRota.visibility = View.GONE
			proximaDemanda = null
		} else {
			proximaDemanda = pendentes.first()
			tituloProximaParada.text = "Próxima Parada"

			val end = StringBuilder()
			proximaDemanda?.logradouro?.let { end.append(it) }
			proximaDemanda?.numero?.let { end.append(", $it") }
			enderecoProximaParada.text = end.toString()

			descricaoProximaParada.text = proximaDemanda?.descricao ?: ""
			descricaoProximaParada.visibility = View.VISIBLE
			btnIniciarVistoria.visibility = View.VISIBLE
			fabIniciarRota.visibility = View.VISIBLE
		}
	}

	private fun atualizarMapa(demandas: List<Demanda>) {
		mapView.overlays.clear()

		demandas.forEachIndexed { index, demanda ->
			if (demanda.lat != null && demanda.lng != null) {
				val point = GeoPoint(demanda.lat, demanda.lng)
				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				marker.title = "Parada ${index + 1}"

				// Ícone verde para a próxima, azul para as outras
				val iconRes = if(index == 0) R.drawable.ic_marker_green else R.drawable.ic_marker_blue
				marker.icon = ContextCompat.getDrawable(this, iconRes)

				mapView.overlays.add(marker)
			}
		}

		// Centralizar mapa na próxima demanda
		if (proximaDemanda?.lat != null && proximaDemanda?.lng != null) {
			val foco = GeoPoint(proximaDemanda!!.lat!!, proximaDemanda!!.lng!!)
			mapView.controller.setZoom(18.0)
			mapView.controller.setCenter(foco)
		}
		mapView.invalidate()
	}

	// --- Navegação e Ações ---

	private fun iniciarRotaGoogleMaps() {
		val lat = proximaDemanda?.lat
		val lng = proximaDemanda?.lng
		if (lat != null && lng != null) {
			val uri = Uri.parse("google.navigation:q=$lat,$lng")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			intent.setPackage("com.google.android.apps.maps")
			startActivity(intent)
		} else {
			Toast.makeText(this, "Coordenadas indisponíveis", Toast.LENGTH_SHORT).show()
		}
	}

	private fun abrirDetalheProximaDemanda() {
		proximaDemanda?.let { demanda ->
			val intent = Intent(this, DemandaDetalheActivity::class.java)
			intent.putExtra("DEMANDA_EXTRA", demanda)
			vistoriaLauncher.launch(intent)
		}
	}

	private fun abrirListaDeDemandas() {
		val intent = Intent(this, DemandaListActivity::class.java)
		intent.putExtra("ROTA_ID", rotaId)
		demandaListLauncher.launch(intent)
	}

	// Menus e Ciclo de Vida
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.rota_detalhe_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> { finish(); true }
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onResume() {
		super.onResume()
		mapView.onResume()
		// Refresh dos dados caso algo tenha mudado
		if(rotaId != -1) viewModel.carregarDados()
	}

	override fun onPause() {
		super.onPause()
		mapView.onPause()
	}
}