package com.dexheimer.treeinspectorandroid.presentation.vistoria.form

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.dexheimer.treeinspectorandroid.data.remote.FormField

/**
 * Contrato para qualquer componente de UI que renderiza um FormField.
 * SRP: Cada implementação é responsável apenas por um tipo de campo.
 */
interface FormFieldRenderer {
	// A lista de tipos que este Renderer pode processar
	val supportedTypes: List<String>

	/**
	 * Renderiza o campo e o anexa ao container.
	 * @return A View principal do campo renderizado para ser rastreada.
	 */
	fun render(context: Context, field: FormField, container: ViewGroup, initialValue: Any? = null): View

	/**
	 * Coleta a resposta do usuário a partir da View do campo.
	 * @return O valor da resposta (String, Boolean, etc.).
	 */
	fun collectResponse(view: View, field: FormField): Any?
}