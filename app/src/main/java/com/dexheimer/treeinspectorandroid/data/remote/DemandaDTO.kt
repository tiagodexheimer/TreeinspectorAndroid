package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.core.util.Geom
import com.dexheimer.treeinspectorandroid.domain.model.Anexo
import com.google.gson.annotations.SerializedName

data class DemandaDTO(
        @SerializedName("id") val id: Int,
        @SerializedName("lat") val lat: Double?,
        @SerializedName("lng") val lng: Double?,
        @SerializedName("status_nome") val statusNome: String?,
        @SerializedName("status_cor") val statusCor: String?,
        @SerializedName("protocolo") val protocolo: String?,
        @SerializedName("nome_solicitante") val nomeSolicitante: String?,
        @SerializedName("telefone_solicitante") val telefoneSolicitante: String?,
        @SerializedName("email_solicitante") val emailSolicitante: String?,
        @SerializedName("prazo") val prazo: String?,
        @SerializedName("created_at") val createdAt: String?,
        @SerializedName("updated_at") val updatedAt: String?,
        @SerializedName("cep") val cep: String?,
        @SerializedName("logradouro") val logradouro: String?,
        @SerializedName("numero") val numero: String?,
        @SerializedName("complemento") val complemento: String?,
        @SerializedName("bairro") val bairro: String?,
        @SerializedName("cidade") val cidade: String?,
        @SerializedName("uf") val uf: String?,
        @SerializedName("tipo_demanda") val tipoDemanda: String?,
        @SerializedName("descricao") val descricao: String?,
        @SerializedName("geom") val geom: Geom?,
        @SerializedName("anexos") val anexos: List<Anexo>? // [NOVO]
)
