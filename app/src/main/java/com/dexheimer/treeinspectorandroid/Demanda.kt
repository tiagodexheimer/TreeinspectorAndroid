package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Classe principal que representa uma Demanda (uma parada na rota).
 * Ela usa a classe 'Geom' (definida em Geom.kt)
 */
data class Demanda(
	@SerializedName("id")
	val id: Int,

	@SerializedName("geom")
	val geom: Geom?, // <-- CORRETO: Usa a classe do Geom.kt

	@SerializedName("logradouro")
	val logradouro: String?,

	@SerializedName("numero")
	val numero: String?,

	@SerializedName("bairro")
	val bairro: String?,

	@SerializedName("tipo_demanda")
	val tipo_demanda: String?,

	@SerializedName("descricao")
	val descricao: String?

) : Serializable

// ---- APAGUE QUALQUER OUTRA DEFINIÇÃO DE CLASSE DESTE ARQUIVO ----
// (Não deve haver 'data class Geom' ou 'data class RotaDetalheResponse' aqui)