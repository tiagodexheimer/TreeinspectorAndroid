package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.LocationManager
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.presentation.demandas.DemandaListActivity
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject

@AndroidEntryPoint
class RotaDetalheActivity : AppCompatActivity() {

	@Inject
	lateinit var okHttpClient: OkHttpClient

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

	// Overlays
	private var locationOverlay: MyLocationNewOverlay? = null
	private var routeLine: Polyline? = null
	private var rotaJob: Job? = null // Para controlar o cancelamento de requisições antigas

	// Estado Local
	private var proximaDemanda: Demanda? = null
	private var rotaId: Int = -1

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

	private val demandaListLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			viewModel.carregarDados()
		}
	}

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
			permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
		) {
			setupLocationOverlay()
		} else {
			Toast.makeText(this, "Permissão de localização necessária para mostrar a rota.", Toast.LENGTH_LONG).show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

		// Configura User-Agent global para o OSMDroid (Evita bloqueios de tiles)
		Configuration.getInstance().userAgentValue = packageName

		setContentView(R.layout.activity_rota_detalhe)

		rotaId = intent.getIntExtra("ROTA_ID", -1)

		setupUI()
		checkLocationPermissions()
		observarViewModel()
	}

	private fun setupUI() {
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		mapView = findViewById(R.id.mapView)
		mapView.setTileSource(TileSourceFactory.MAPNIK)
		mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
		mapView.setMultiTouchControls(true)

		fabIniciarRota = findViewById(R.id.fabIniciarRota)
		cardProximaVistoria = findViewById(R.id.proximaVistoriaCard)
		tituloProximaParada = findViewById(R.id.tituloProximaParada)
		enderecoProximaParada = findViewById(R.id.enderecoProximaParada)
		descricaoProximaParada = findViewById(R.id.descricaoProximaParada)
		btnIniciarVistoria = findViewById(R.id.btnIniciarVistoria)
		btnVerTodasDemandas = findViewById(R.id.btnVerTodasDemandas)

		fabIniciarRota.setOnClickListener { iniciarRotaGoogleMaps() }
		btnIniciarVistoria.setOnClickListener { abrirVistoriaProximaDemanda() }
		btnVerTodasDemandas.setOnClickListener { abrirListaDeDemandas() }
	}

	private fun setupLocationOverlay() {
		val provider = GpsMyLocationProvider(this)
		provider.addLocationSource(LocationManager.GPS_PROVIDER)
		provider.addLocationSource(LocationManager.NETWORK_PROVIDER)

		locationOverlay = MyLocationNewOverlay(provider, mapView)
		locationOverlay?.enableMyLocation()

		// Ícone de navegação
		val bitmapNavegacao = getBitmapFromVectorDrawable(R.drawable.ic_navigation)
		if (bitmapNavegacao != null) {
			locationOverlay?.setPersonIcon(bitmapNavegacao)
			locationOverlay?.setDirectionIcon(bitmapNavegacao)
		}

		mapView.overlays.add(locationOverlay)

		// Quando tiver a primeira localização, atualiza o mapa para desenhar a linha
		locationOverlay?.runOnFirstFix {
			runOnUiThread {
				viewModel.uiState.value.let { state ->
					atualizarMapa(state.demandasPendentes)
				}
			}
		}
		mapView.invalidate()
	}

	private fun checkLocationPermissions() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
			ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
		) {
			setupLocationOverlay()
		} else {
			requestPermissionLauncher.launch(
				arrayOf(
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACCESS_COARSE_LOCATION
				)
			)
		}
	}

	private fun observarViewModel() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					if (state.isLoading) tituloProximaParada.text = "Carregando..."
					if (state.error != null) Toast.makeText(this@RotaDetalheActivity, state.error, Toast.LENGTH_LONG).show()
					state.rota?.let { supportActionBar?.title = it.nome }

					atualizarCardProxima(state.demandasPendentes)
					atualizarMapa(state.demandasPendentes)
				}
			}
		}
	}

	private fun atualizarCardProxima(pendentes: List<Demanda>) {
		if (pendentes.isEmpty()) {
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
		// 1. Cancelar busca de rota anterior se houver (evita piscar ou linhas erradas)
		rotaJob?.cancel()

		// 2. Limpeza de overlays (mantendo localização)
		val overlaysParaManter = mapView.overlays.filter { it is MyLocationNewOverlay }
		mapView.overlays.clear()
		mapView.overlays.addAll(overlaysParaManter)

		// 3. Adiciona Marcadores
		demandas.forEachIndexed { index, demanda ->
			if (demanda.lat != null && demanda.lng != null) {
				val point = GeoPoint(demanda.lat, demanda.lng)
				val marker = Marker(mapView)
				marker.position = point
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
				marker.title = "Parada ${index + 1}"

				val iconRes = if (index == 0) R.drawable.ic_marker_red else R.drawable.ic_marker_blue
				marker.icon = ContextCompat.getDrawable(this, iconRes)
				mapView.overlays.add(marker)
			}
		}

		// 4. Lógica da Linha (Rota)
		val userLocation = locationOverlay?.myLocation
		val nextPoint = if (proximaDemanda?.lat != null && proximaDemanda?.lng != null) {
			GeoPoint(proximaDemanda!!.lat!!, proximaDemanda!!.lng!!)
		} else null

		if (userLocation != null && nextPoint != null) {

			// Cria uma linha temporária (reta) enquanto baixa a detalhada
			val linhaReta = Polyline().apply {
				addPoint(userLocation)
				addPoint(nextPoint)
				outlinePaint.color = Color.GRAY
				outlinePaint.strokeWidth = 5f
				// Opcional: fazer pontilhada se quiser indicar que é temporária
			}
			mapView.overlays.add(linhaReta)

			// Inicia busca da rota real em background
			rotaJob = lifecycleScope.launch {
				val pontosRota = fetchRoutePoints(userLocation, nextPoint)

				// Remove a linha reta temporária
				mapView.overlays.remove(linhaReta)

				if (pontosRota.isNotEmpty()) {
					// SUCESSO: Desenha rota detalhada
					routeLine = Polyline().apply {
						setPoints(pontosRota)
						outlinePaint.color = Color.parseColor("#4285F4") // Azul
						outlinePaint.strokeWidth = 15f
						outlinePaint.strokeCap = Paint.Cap.ROUND
						outlinePaint.isAntiAlias = true
					}
					mapView.overlays.add(routeLine)
				} else {
					// FALHA: Desenha a linha reta novamente (Fallback) mas com a cor oficial
					val linhaFallback = Polyline().apply {
						addPoint(userLocation)
						addPoint(nextPoint)
						outlinePaint.color = Color.parseColor("#4285F4")
						outlinePaint.strokeWidth = 10f
					}
					mapView.overlays.add(linhaFallback)
				}
				mapView.invalidate()
			}

			// Enquadramento
			val points = arrayListOf(userLocation, nextPoint)
			val boundingBox = BoundingBox.fromGeoPoints(points)
			mapView.zoomToBoundingBox(boundingBox, true, 200)

		} else if (nextPoint != null) {
			mapView.controller.setZoom(18.0)
			mapView.controller.setCenter(nextPoint)
		} else if (userLocation != null) {
			mapView.controller.setZoom(18.0)
			mapView.controller.animateTo(userLocation)
		}

		mapView.invalidate()
	}

	// --- LÓGICA DE ROTEAMENTO (OSRM) ---
	private suspend fun fetchRoutePoints(start: GeoPoint, end: GeoPoint): List<GeoPoint> = withContext(Dispatchers.IO) {
		val points = mutableListOf<GeoPoint>()
		try {
			// URL da API pública do OSRM
			val url = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"

			val request = Request.Builder()
				.url(url)
				.header("User-Agent", "TreeInspectorAndroid/1.0") // IMPORTANTE: Header adicionado
				.build()

			val response = okHttpClient.newCall(request).execute()

			if (response.isSuccessful) {
				val jsonResponse = response.body?.string()
				if (jsonResponse != null) {
					val jsonObject = JSONObject(jsonResponse)
					val routes = jsonObject.optJSONArray("routes")
					if (routes != null && routes.length() > 0) {
						val geometry = routes.getJSONObject(0).getJSONObject("geometry")
						val coordinates = geometry.getJSONArray("coordinates")

						for (i in 0 until coordinates.length()) {
							val coord = coordinates.getJSONArray(i)
							// OSRM retorna [lon, lat], GeoPoint quer [lat, lon]
							val lon = coord.getDouble(0)
							val lat = coord.getDouble(1)
							points.add(GeoPoint(lat, lon))
						}
					}
				}
			} else {
				Log.e("RotaDetalhe", "Erro API OSRM: ${response.code} - ${response.message}")
			}
		} catch (e: Exception) {
			Log.e("RotaDetalhe", "Exceção na rota OSRM", e)
		}
		return@withContext points
	}

	private fun iniciarRotaGoogleMaps() {
		val lat = proximaDemanda?.lat
		val lng = proximaDemanda?.lng

		// 1. Muda status IMEDIATAMENTE
		proximaDemanda?.let {
			viewModel.iniciarAtendimento(it.id)

			// Opcional: Atualizar o texto do card manualmente para feedback instantâneo
			// enquanto o Maps carrega
			tituloProximaParada.text = "Em Rota - ${it.tipoDemanda}"
		}

		// 2. Abre Maps
		if (lat != null && lng != null) {
			val uri = Uri.parse("google.navigation:q=$lat,$lng")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			intent.setPackage("com.google.android.apps.maps")
			try {
				startActivity(intent)
			} catch (e: Exception) {
				// Fallback se não tiver Google Maps
				startActivity(Intent(Intent.ACTION_VIEW, uri))
			}
		} else {
			Toast.makeText(this, "Coordenadas indisponíveis", Toast.LENGTH_SHORT).show()
		}
	}

	private fun abrirVistoriaProximaDemanda() {
		proximaDemanda?.let { demanda ->
			// [CORRIGIDO] Chama a função no ViewModel
			viewModel.iniciarAtendimento(demanda.id)

			// Abre a tela de Vistoria normalmente
			val intent = Intent(this, VistoriaActivity::class.java)
			intent.putExtra("DEMANDA_EXTRA", demanda)
			vistoriaLauncher.launch(intent)
		}
	}


	private fun abrirListaDeDemandas() {
		val intent = Intent(this, DemandaListActivity::class.java)
		intent.putExtra("ROTA_ID", rotaId)
		demandaListLauncher.launch(intent)
	}

	private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
		val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null
		val bitmap = Bitmap.createBitmap(
			drawable.intrinsicWidth,
			drawable.intrinsicHeight,
			Bitmap.Config.ARGB_8888
		)
		val canvas = Canvas(bitmap)
		drawable.setBounds(0, 0, canvas.width, canvas.height)
		drawable.draw(canvas)
		return bitmap
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.rota_detalhe_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> { finish(); true }
			R.id.action_optimize -> {
				// CORREÇÃO: Obter localização do usuário para a otimização
				val userLocation = locationOverlay?.myLocation
				val userLat = userLocation?.latitude
				val userLng = userLocation?.longitude

				viewModel.otimizarRota(userLat, userLng) // <--- Chamada agora passa Lat/Lng
				Toast.makeText(this, "Rota otimizada", Toast.LENGTH_SHORT).show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onResume() {
		super.onResume()
		mapView.onResume()
		locationOverlay?.enableMyLocation()
	}

	override fun onPause() {
		super.onPause()
		locationOverlay?.disableMyLocation()
		mapView.onPause()
	}
}