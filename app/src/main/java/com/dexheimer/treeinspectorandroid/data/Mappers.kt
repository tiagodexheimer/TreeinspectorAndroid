package com.dexheimer.treeinspectorandroid.data

import com.dexheimer.treeinspectorandroid.data.local.DemandaEntity
import com.dexheimer.treeinspectorandroid.data.local.RotaEntity
import com.dexheimer.treeinspectorandroid.data.remote.DemandaDTO
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota

// --- DTO -> ENTITY (Da API para o Banco de Dados) ---

fun DemandaDTO.toEntity(rotaId: Int): DemandaEntity {
	return DemandaEntity(
		id = this.id,
		lat = this.lat,
		lng = this.lng,
		idStatus = null,
		statusNome = this.statusNome,
		statusCor = this.statusCor,
		protocolo = this.protocolo,
		nomeSolicitante = this.nomeSolicitante,
		telefoneSolicitante = this.telefoneSolicitante,
		emailSolicitante = this.emailSolicitante,
		prazo = this.prazo,
		createdAt = this.createdAt,
		updatedAt = this.updatedAt,
		cep = this.cep,
		logradouro = this.logradouro,
		numero = this.numero,
		complemento = this.complemento,
		bairro = this.bairro,
		cidade = this.cidade,
		uf = this.uf,
		tipoDemanda = this.tipoDemanda,
		descricao = this.descricao,
		geom = this.geom,
		rotaId = rotaId,
		statusVistoria = "pendente" // Padrão ao baixar da API
	)
}

// --- ENTITY -> DOMAIN (Do Banco de Dados para a Tela) ---

fun DemandaEntity.toDomain(): Demanda {
	return Demanda(
		id = this.id,
		lat = this.lat,
		lng = this.lng,
		statusNome = this.statusNome,
		statusCor = this.statusCor,
		protocolo = this.protocolo,
		nomeSolicitante = this.nomeSolicitante,
		telefoneSolicitante = this.telefoneSolicitante,
		emailSolicitante = this.emailSolicitante,
		prazo = this.prazo,
		dataCriacao = this.createdAt,
		dataAtualizacao = this.updatedAt,
		cep = this.cep,
		logradouro = this.logradouro,
		numero = this.numero,
		complemento = this.complemento,
		bairro = this.bairro,
		cidade = this.cidade,
		uf = this.uf,
		tipoDemanda = this.tipoDemanda,
		descricao = this.descricao,
		geom = this.geom,
		statusVistoria = this.statusVistoria,
		rotaId = this.rotaId
	)
}

fun RotaEntity.toDomain(): Rota {
	return Rota(
		id = this.id,
		nome = this.nome,
		responsavel = this.responsavel,
		status = this.status,
		dataRota = this.dataRota,
		dataCriacao = this.createdAt,
		totalDemandas = this.totalDemandas ?: 0 // <--- Adicione esta linha
	)
}

fun Rota.toEntity(): RotaEntity {
	return RotaEntity(
		id = this.id,
		nome = this.nome,
		responsavel = this.responsavel,
		status = this.status,
		dataRota = this.dataRota,
		createdAt = this.dataCriacao,
		totalDemandas = this.totalDemandas // <--- Adicione esta linha
	)
}

fun Demanda.toEntity(rotaId: Int): DemandaEntity {
	return DemandaEntity(
		id = this.id,
		lat = this.lat,
		lng = this.lng,
		idStatus = null,
		statusNome = this.statusNome,
		statusCor = this.statusCor,
		protocolo = this.protocolo,
		nomeSolicitante = this.nomeSolicitante,
		telefoneSolicitante = this.telefoneSolicitante,
		emailSolicitante = this.emailSolicitante,
		prazo = this.prazo,
		createdAt = this.dataCriacao,
		updatedAt = this.dataAtualizacao,
		cep = this.cep,
		logradouro = this.logradouro,
		numero = this.numero,
		complemento = this.complemento,
		bairro = this.bairro,
		cidade = this.cidade,
		uf = this.uf,
		tipoDemanda = this.tipoDemanda,
		descricao = this.descricao,
		geom = this.geom,
		statusVistoria = this.statusVistoria,
		rotaId = rotaId
	)
}