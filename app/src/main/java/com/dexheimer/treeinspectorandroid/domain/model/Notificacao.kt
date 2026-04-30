package com.dexheimer.treeinspectorandroid.domain.model

import java.io.Serializable

data class Notificacao(
    val id: Int,
    val numeroProcesso: String,
    val numeroNotificacao: String?,
    val descricao: String?,
    val dataEmissao: String,
    val prazoDias: Int,
    val vencimento: String,
    val status: String,
    val fotos: List<Anexo>?
) : Serializable
