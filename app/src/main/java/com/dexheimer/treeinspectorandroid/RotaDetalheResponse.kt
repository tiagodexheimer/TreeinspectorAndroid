package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Classe que representa a resposta JSON completa da API /api/rotas/[id].
 * (Esta era a classe que faltava e causava o erro 'Unresolved reference')
 */
data class RotaDetalheResponse(
	@SerializedName("rota")
	val rota: Rota, // Refere-se ao seu Rota.kt

	@SerializedName("demandas")
	val demandas: List<Demanda>, // Refere-se ao seu Demanda.kt

	@SerializedName("geometry")
	val geometry: String? // A polilinha da rota
) : Serializable