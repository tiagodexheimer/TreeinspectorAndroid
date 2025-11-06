package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName
import java.io.Serializable // <-- 1. ADICIONE ESTE IMPORT

// Esta classe representa o objeto "geom"
//                              <-- 2. ADICIONE ': Serializable' AQUI
data class Geom(
	@SerializedName("coordinates")
	val coordinates: List<Double>
) : Serializable