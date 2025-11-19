package com.dexheimer.treeinspectorandroid.domain.model

import com.dexheimer.treeinspectorandroid.core.util.Geom // Certifique-se de que o Geom foi movido para core/util
import java.io.Serializable

data class Demanda(
	val id: Int,
	val lat: Double?,
	val lng: Double?,
	val statusNome: String?, // Renomeei para camelCase (padrão Kotlin)
	val statusCor: String?,
	val protocolo: String?,
	val nomeSolicitante: String?,
	val telefoneSolicitante: String?,
	val emailSolicitante: String?,
	val prazo: String?,
	val dataCriacao: String?,
	val dataAtualizacao: String?,
	val cep: String?,
	val logradouro: String?,
	val numero: String?,
	val complemento: String?,
	val bairro: String?,
	val cidade: String?,
	val uf: String?,
	val tipoDemanda: String?,
	val descricao: String?,
	val geom: Geom?, // Objeto de Geometria

	// Campos de controle local (App)
	val statusVistoria: String = "pendente",
	val rotaId: Int = 0
) : Serializable