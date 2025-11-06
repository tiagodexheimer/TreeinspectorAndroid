package com.dexheimer.treeinspectorandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultNavigationApiExtensions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.view.MapboxNavigationView
import kotlin.collections.isNotEmpty

class NavegacaoActivity : AppCompatActivity() {

	private lateinit var navigationView: MapboxNavigationView
	private lateinit var mapboxNavigation: MapboxNavigation

	private var destinationPoint: Point? = null

	// Observador da localização do usuário
	private val locationObserver = object : LocationObserver {
		override fun onNewRawLocation(rawLocation: android.location.Location) {
			// Não precisamos fazer nada aqui por enquanto
		}
		override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
			// Opcional: Pegar a localização atualizada
		}
	}

	// Observador das rotas
	private val routesObserver = object : RoutesObserver {
		override fun onRoutesChanged(routes: List<NavigationRoute>) {
			if (routes.isNotEmpty()) {
				// Rota calculada com sucesso, inicia a navegação
				mapboxNavigation.startTripSession()
				Log.d("NavegacaoActivity", "Rota encontrada. Iniciando sessão de viagem.")
			} else {
				Log.d("NavegacaoActivity", "Nenhuma rota encontrada.")
				Toast.makeText(this@NavegacaoActivity, "Não foi possível encontrar uma rota", Toast.LENGTH_SHORT).show()
				finish()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_navegacao)
		navigationView = findViewById(R.id.navigationView)

		// 1. Pega as coordenadas do destino
		val lat = intent.getDoubleExtra("DEST_LAT", 0.0)
		val lng = intent.getDoubleExtra("DEST_LNG", 0.0)

		if (lat == 0.0 || lng == 0.0) {
			Log.e("NavegacaoActivity", "Coordenadas de destino inválidas.")
			finish()
			return
		}
		destinationPoint = Point.fromLngLat(lng, lat)

		// 2. Inicializa o Mapbox Navigation
		mapboxNavigation = MapboxNavigationApp.mapboxNavigation(
			NavigationOptions.Builder(this)
				.accessToken(getString(R.string.mapbox_access_token)) // Pega o token do Manifesto
				.build()
		)

		// 3. Registra os observadores
		mapboxNavigation.registerRoutesObserver(routesObserver)
		mapboxNavigation.registerLocationObserver(locationObserver)

		// 4. Configura a UI de Navegação (NavigationView)
		navigationView.api.routeReplayEnabled(false) // Desabilita simulação
		navigationView.api.options.applyDefaultNavigationApiExtensions()
		navigationView.api.options.applyLanguageAndVoiceUnitOptions(this)

		// 5. Configura o Estilo do Mapa (Dia/Noite)
		navigationView.mapboxMap.loadStyle(NavigationStyles.NAVIGATION_DAY_STYLE) {
			// Assim que o mapa estiver carregado, busca a rota
			fetchRoute()
		}
	}

	/**
	 * Busca a rota usando a localização atual (simulada ou real) e o destino.
	 */
	private fun fetchRoute() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
			ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_SHORT).show()
			finish()
			return
		}

		// Pede ao Mapbox para calcular a rota
		mapboxNavigation.requestRoutes(
			RouteOptions.builder()
				.applyDefaultNavigationApiExtensions()
				.coordinatesList(listOf(
					// Ponto de Origem: Mapbox pega o GPS automaticamente
					null,
					// Ponto de Destino:
					destinationPoint
				))
				.bearingsList(listOf(
					Bearing.builder().angle(0.0).degrees(45.0).build(), // Bearing para origem (GPS)
					null // Bearing para destino
				))
				.layersList(listOf(mapboxNavigation.getRouteProfileLayers(null)))
				.build(),
			object : NavigationRouterCallback {
				override fun onCanceled() {
					Log.d("NavegacaoActivity", "Cálculo da rota cancelado.")
				}
				override fun onFailure(error: RouterFailure) {
					Log.e("NavegacaoActivity", "Falha ao calcular rota: ${error.message}")
					Toast.makeText(this@NavegacaoActivity, "Falha ao calcular rota", Toast.LENGTH_SHORT).show()
				}
				override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
					// O 'routesObserver' será notificado
					Log.d("NavegacaoActivity", "Rotas recebidas: ${routes.size}")
				}
			}
		)
	}

	/**
	 * Limpa os observadores quando a Activity é destruída
	 */
	override fun onDestroy() {
		super.onDestroy()
		mapboxNavigation.unregisterRoutesObserver(routesObserver)
		mapboxNavigation.unregisterLocationObserver(locationObserver)
		mapboxNavigation.onDestroy()
	}
}