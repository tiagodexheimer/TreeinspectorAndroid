package com.dexheimer.treeinspectorandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dexheimer.treeinspectorandroid.core.util.GeomTypeConverter

// Certifique-se que 'FormularioCache::class' está na lista de entities
@Database(
	entities = [RotaEntity::class, DemandaEntity::class, FormularioCache::class, VistoriaPendente::class],
	version = 4, // Se der erro de migração, aumente para 5
	exportSchema = false
)
@TypeConverters(GeomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

	abstract fun rotaDao(): RotaDao
	abstract fun demandaDao(): DemandaDao
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
					.fallbackToDestructiveMigration()
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
}