package com.dexheimer.treeinspectorandroid

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Classe GeomTypeConverter: Manter como está (dentro deste arquivo)

@Entity(tableName = "demandas")
@TypeConverters(GeomTypeConverter::class)
data class Demanda(

	@PrimaryKey
	@SerializedName("id")
	val id: Int,

	// --- NOVOS CAMPOS DA API (Coordenadas, Status, Protocolo, Solicitante) ---
	@SerializedName("lat")
	val lat: Double?,
	@SerializedName("lng")
	val lng: Double?,

	@SerializedName("id_status")
	val id_status: Int?,
	@SerializedName("status_nome")
	val status_nome: String?,
	@SerializedName("status_cor")
	val status_cor: String?,

	@SerializedName("protocolo")
	val protocolo: String?,
	@SerializedName("nome_solicitante")
	val nome_solicitante: String?,
	@SerializedName("telefone_solicitante")
	val telefone_solicitante: String?,
	@SerializedName("email_solicitante")
	val email_solicitante: String?,
	@SerializedName("prazo")
	val prazo: String?,
	@SerializedName("created_at")
	val created_at: String?,
	@SerializedName("updated_at")
	val updated_at: String?,

	// --- CAMPOS DE ENDEREÇO ATUALIZADOS ---
	@SerializedName("cep")
	val cep: String?,
	@SerializedName("logradouro")
	val logradouro: String?,

	@SerializedName("numero")
	val numero: String?,

	@SerializedName("complemento")
	val complemento: String?,
	@SerializedName("bairro")
	val bairro: String?,
	@SerializedName("cidade")
	val cidade: String?,
	@SerializedName("uf")
	val uf: String?,

	@SerializedName("tipo_demanda")
	val tipo_demanda: String?,

	@SerializedName("descricao")
	val descricao: String?,

	// --- CAMPO DE GEOMETRIA ORIGINAL ---
	@SerializedName("geom")
	val geom: Geom?,

	// --- CAMPOS LOCAIS DO ROOM ---
	@ColumnInfo(name = "status_vistoria", defaultValue = "pendente")
	var status_vistoria: String = "pendente",

	@ColumnInfo(name = "rota_id", index = true)
	var rotaId: Int = 0

) : Serializable