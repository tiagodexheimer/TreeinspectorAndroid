package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FormularioDao {

	// Busca o JSON usando o nome correto da coluna 'tipoDemanda' e 'jsonEstrutura'
	@Query("SELECT jsonEstrutura FROM formularios_cache WHERE tipoDemanda = :tipo")
	suspend fun getFormularioJson(tipo: String): String?

	// Insere o objeto FormularioCache
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(formulario: FormularioCache)

	// Função de transação para facilitar a chamada no Repositório
	// Cria o objeto FormularioCache com os nomes corretos dos campos
	@Transaction
	suspend fun insertOrUpdate(tipo: String, json: String) {
		val formulario = FormularioCache(
			tipoDemanda = tipo,
			jsonEstrutura = json
		)
		insert(formulario)
	}
}