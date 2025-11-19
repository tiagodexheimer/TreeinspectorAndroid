package com.dexheimer.treeinspectorandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dexheimer.treeinspectorandroid.core.util.GeomTypeConverter

// 1. AUMENTADO A VERSÃO PARA 3 para forçar o Room a reconhecer as mudanças
@Database(
	entities = [Rota::class, Demanda::class, FormularioCache::class, VistoriaPendente::class], // <--- ADICIONE AS NOVAS ENTIDADES
	version = 2, // <--- INCREMENTE A VERSÃO
	exportSchema = false
)
@TypeConverters(GeomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

	abstract fun rotaDao(): RotaDao
	abstract fun demandaDao(): DemandaDao

	// NOVOS DAOS
	abstract fun formularioDao(): FormularioDao
	abstract fun vistoriaDao(): VistoriaDao

	companion object {
		@Volatile
		private var INSTANCE: AppDatabase? = null

		fun getInstance(context: Context): AppDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					AppDatabase::class.java,
					"tree_inspector_db"
				)
					.fallbackToDestructiveMigration() // <--- Útil para desenvolvimento, limpa o banco se mudar versão
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
}