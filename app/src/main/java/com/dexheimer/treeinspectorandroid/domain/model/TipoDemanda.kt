package com.dexheimer.treeinspectorandroid.domain.model

import com.google.gson.annotations.SerializedName

data class TipoDemanda(
    val id: Int,
    val nome: String,
    @SerializedName("is_custom")
    val isCustom: Boolean,
    @SerializedName("is_default_global")
    val isDefaultGlobal: Boolean
)
