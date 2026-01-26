package com.dexheimer.treeinspectorandroid.core.util

import androidx.room.TypeConverter
import com.dexheimer.treeinspectorandroid.domain.model.Anexo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AnexosTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromAnexosList(anexos: List<Anexo>?): String? {
        return anexos?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toAnexosList(anexosString: String?): List<Anexo>? {
        return anexosString?.let {
            val type = object : TypeToken<List<Anexo>>() {}.type
            gson.fromJson(it, type)
        }
    }
}
