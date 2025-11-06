package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName
import java.io.Serializable // IMPORTANTE

// Implementamos Serializable para passar o objeto entre Activities
data class Demanda(
	@SerializedName("id")
	val id: Int,

	@SerializedName("geom")
	val geom: Geom?,

	// ---- NOVOS CAMPOS ADICIONADOS ----
	@SerializedName("logradouro")
	val logradouro: String?,

	@SerializedName("numero")
	val numero: String?,

	@SerializedName("bairro")
	val bairro: String?,

	@SerializedName("tipo_demanda")
	val tipo_demanda: String?

	// Nota: O GSON irá ignorar os outros campos (status_nome, etc.)
	// que não definimos aqui.

) : Serializable // IMPORTANTE