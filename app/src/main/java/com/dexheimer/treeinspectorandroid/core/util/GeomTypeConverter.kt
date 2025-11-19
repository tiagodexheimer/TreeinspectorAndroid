package com.dexheimer.treeinspectorandroid.core.util

import androidx.room.TypeConverter
import com.google.gson.Gson

/**
 * Classe para o TypeConverter do Room.
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