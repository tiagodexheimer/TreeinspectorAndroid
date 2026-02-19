package com.dexheimer.treeinspectorandroid.domain.model

/**
 * Parâmetros de domínio para criação de uma demanda. Isola a camada de domínio de DTOs da camada de
 * dados.
 */
data class CreateDemandaParams(
        val nome_solicitante: String,
        val telefone_solicitante: String? = null,
        val email_solicitante: String? = null,
        val cep: String,
        val logradouro: String? = null,
        val numero: String,
        val complemento: String? = null,
        val bairro: String? = null,
        val cidade: String? = null,
        val uf: String? = null,
        val tipo_demanda: String,
        val descricao: String,
        val coordinates: List<Double>? = null,
        val anexos: List<String>? = null
)
