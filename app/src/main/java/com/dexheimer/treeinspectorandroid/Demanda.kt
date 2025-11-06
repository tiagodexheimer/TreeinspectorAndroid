package com.dexheimer.treeinspectorandroid

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Classe para o TypeConverter do Room.
 * Isso ensina o Room a salvar o objeto 'Geom' como um texto (JSON) no banco de dados
 * e a convertê-lo de volta para um objeto 'Geom' ao ser lido.
 */
class GeomTypeConverter {
	private val gson = Gson()

	/**
	 * Converte um objeto Geom em uma String JSON
	 */
	@TypeConverter
	fun fromGeom(geom: Geom?): String? {
		return geom?.let { gson.toJson(it) }
	}

	/**
	 * Converte uma String JSON de volta para um objeto Geom
	 */
	@TypeConverter
	fun toGeom(geomString: String?): Geom? {
		return geomString?.let { gson.fromJson(it, Geom::class.java) }
	}
}


/**
 * Classe principal que representa uma Demanda (uma parada na rota).
 *
 * Agora está anotada como @Entity para ser usada pelo Room (banco de dados local).
 * Ela usa a classe 'Geom' (definida em Geom.kt)
 */
@Entity(tableName = "demandas") // <-- ANOTAÇÃO DO ROOM: Define o nome da tabela
@TypeConverters(GeomTypeConverter::class) // <-- ANOTAÇÃO DO ROOM: Diz para usar o converter acima
data class Demanda(

	@PrimaryKey // <-- ANOTAÇÃO DO ROOM: Define o 'id' como chave primária
	@SerializedName("id")
	val id: Int,

	@SerializedName("geom")
	val geom: Geom?, // <-- Room usará o GeomTypeConverter para este campo

	@SerializedName("logradouro")
	val logradouro: String?,

	@SerializedName("numero")
	val numero: String?,

	@SerializedName("bairro")
	val bairro: String?,

	@SerializedName("tipo_demanda")
	val tipo_demanda: String?,

	@SerializedName("descricao")
	val descricao: String?,

	// --- CAMPO ADICIONAL PARA O BANCO DE DADOS LOCAL ---
	/**
	 * Chave estrangeira para associar esta demanda a uma Rota no banco local.
	 * Este campo NÃO é preenchido pelo Gson (pois não vem da API neste nível),
	 * mas é usado para o banco de dados Room.
	 * Ele é definido manualmente na RotaDetalheActivity antes de salvar no banco.
	 */
	@ColumnInfo(name = "rota_id", index = true) // <-- Coluna para o ID da Rota
	var rotaId: Int = 0

) : Serializable

// ---- APAGUE QUALQUER OUTRA DEFINIÇÃO DE CLASSE DESTE ARQUIVO ----
// (Não deve haver 'data class Geom' ou 'data class RotaDetalheResponse' aqui)