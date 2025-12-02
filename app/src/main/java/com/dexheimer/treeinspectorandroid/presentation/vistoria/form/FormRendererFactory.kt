package com.dexheimer.treeinspectorandroid.presentation.vistoria.form

import javax.inject.Inject

/**
 * Factory que mapeia o tipo do FormField para o Renderer correspondente.
 * OCP: Adicionar um novo renderer exige apenas adicionar uma entrada no mapa, sem mudar a Activity.
 */
class FormRendererFactory @Inject constructor(
	// O Hilt injeta um Set de todos os FormFieldRenderer's criados.
	private val renderers: Set<@JvmSuppressWildcards FormFieldRenderer>

) {
	// Mapa para busca rápida: "text" -> TextFieldRenderer
	private val rendererMap: Map<String, FormFieldRenderer> = buildMap {
		renderers.forEach { renderer ->
			renderer.supportedTypes.forEach { type ->
				put(type, renderer)
			}
		}
	}

	fun getRenderer(type: String): FormFieldRenderer? {
		return rendererMap[type]
	}
}