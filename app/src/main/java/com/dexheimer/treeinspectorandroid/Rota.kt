package com.dexheimer.treeinspectorandroid

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "rotas")
data class Rota(
	@PrimaryKey
	@SerializedName("id")
	val id: Int,

	@SerializedName("nome")
	val nome: String,

	@SerializedName("responsavel")
	val responsavel: String?,

	@SerializedName("status")
	val status: String?,

	@SerializedName("data_rota")
	val data_rota: String?,

	// --- CAMPO ADICIONADO ---
	// Este campo vem da API (ambas) e será salvo no Room
	@SerializedName("created_at")
	val created_at: String?,

	// --- CAMPO ADICIONADO ---
	// Este campo vem APENAS da API de lista (api/rotas/route.ts)
	// Usamos @Ignore para que o Room não tente salvá-lo no banco
	@Ignore
	@SerializedName("total_demandas")
	val total_demandas: Int? = 0

) : Serializable {
	// Construtor secundário para o Room (que não conhece campos @Ignore)
	// Isso evita erros de compilação do Room
	constructor(
		id: Int,
		nome: String,
		responsavel: String?,
		status: String?,
		data_rota: String?,
		created_at: String?
	) : this(id, nome, responsavel, status, data_rota, created_at, 0)
}