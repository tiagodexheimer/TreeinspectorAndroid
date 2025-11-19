package com.dexheimer.treeinspectorandroid.core.util

import com.google.gson.annotations.SerializedName
import java.io.Serializable // <-- ESSENCIAL: Garanta que este import exista

data class Geom(
	@SerializedName("coordinates")
	val coordinates: List<Double>
) : Serializable // <-- ESSENCIAL: Deve implementar Serializable