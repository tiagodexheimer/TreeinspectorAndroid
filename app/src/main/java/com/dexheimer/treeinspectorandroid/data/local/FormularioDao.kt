package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FormularioDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun salvarFormulario(formulario: FormularioCache)

	@Query("SELECT jsonEstrutura FROM formularios_cache WHERE tipoDemanda = :tipo")
	suspend fun getFormularioJson(tipo: String): String?
}