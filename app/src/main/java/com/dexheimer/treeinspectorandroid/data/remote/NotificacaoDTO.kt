package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.domain.model.Anexo
import com.google.gson.annotations.SerializedName

data class NotificacaoDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("numero_processo") val numeroProcesso: String,
    @SerializedName("numero_notificacao") val numeroNotificacao: String?,
    @SerializedName("descricao") val descricao: String?,
    @SerializedName("data_emissao") val dataEmissao: String,
    @SerializedName("prazo_dias") val prazoDias: Int,
    @SerializedName("vencimento") val vencimento: String,
    @SerializedName("status") val status: String,
    @SerializedName("fotos") val fotos: List<Anexo>?
)
