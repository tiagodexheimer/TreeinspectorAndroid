package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dexheimer.treeinspectorandroid.core.util.Geom
import com.dexheimer.treeinspectorandroid.core.util.GeomTypeConverter // Se tiver movido o converter

@Entity(tableName = "demandas")
@TypeConverters(GeomTypeConverter::class)
data class DemandaEntity(
	@PrimaryKey
	val id: Int,
	val lat: Double?,
	val lng: Double?,

	@ColumnInfo(name = "id_status") val idStatus: Int?,
	@ColumnInfo(name = "status_nome") val statusNome: String?,
	@ColumnInfo(name = "status_cor") val statusCor: String?,

	val protocolo: String?,
	@ColumnInfo(name = "nome_solicitante") val nomeSolicitante: String?,
	@ColumnInfo(name = "telefone_solicitante") val telefoneSolicitante: String?,
	@ColumnInfo(name = "email_solicitante") val emailSolicitante: String?,
	val prazo: String?,
	@ColumnInfo(name = "created_at") val createdAt: String?,
	@ColumnInfo(name = "updated_at") val updatedAt: String?,

	val cep: String?,
	val logradouro: String?,
	val numero: String?,
	val complemento: String?,
	val bairro: String?,
	val cidade: String?,
	val uf: String?,

	@ColumnInfo(name = "tipo_demanda") val tipoDemanda: String?,
	val descricao: String?,
	val geom: Geom?,

	// Campos Locais
	@ColumnInfo(name = "status_vistoria", defaultValue = "pendente")
	val statusVistoria: String = "pendente",

	@ColumnInfo(name = "rota_id", index = true)
	val rotaId: Int = 0
)