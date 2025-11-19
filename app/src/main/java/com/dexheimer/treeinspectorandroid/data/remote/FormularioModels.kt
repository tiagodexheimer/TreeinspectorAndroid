package com.dexheimer.treeinspectorandroid.data.remote

import com.google.gson.annotations.SerializedName

data class FormField(
	@SerializedName("id") val id: String,
	@SerializedName("name") val name: String,
	@SerializedName("type") val type: String,
	@SerializedName("label") val label: String,
	@SerializedName("rows") val rows: Int? = 1,
	@SerializedName("defaultValue") val defaultValue: Any? = null,
	@SerializedName("options") val options: List<FormOption>? = null
)

data class FormOption(
	@SerializedName("id") val id: String,
	@SerializedName("label") val label: String,
	@SerializedName("value") val value: String
)