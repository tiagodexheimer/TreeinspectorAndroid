package com.dexheimer.treeinspectorandroid.domain.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Anexo(
        @SerializedName("url") val url: String,
        @SerializedName("nome") val nome: String,
        @SerializedName("type") val type: String? = null
) : Serializable
