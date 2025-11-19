package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import javax.inject.Inject

// Lida com campos 'text' e 'textarea'
class TextFieldRenderer @Inject constructor() : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("textarea", "text")

	override fun render(context: Context, field: FormField, container: ViewGroup): View {
		val textInputLayout = TextInputLayout(context)
		textInputLayout.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		textInputLayout.hint = field.label
		textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE

		val editText = TextInputEditText(textInputLayout.context)
		editText.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

		if (field.type == "textarea") {
			editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
			editText.minLines = field.rows ?: 3
			editText.gravity = Gravity.TOP or Gravity.START
		} else {
			editText.inputType = InputType.TYPE_CLASS_TEXT
		}

		textInputLayout.addView(editText)
		container.addView(textInputLayout)
		addSpacer(context, container)
		return editText
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		return (view as EditText).text.toString()
	}
	// Adicione esta função a todos os arquivos TextFieldRenderer.kt, RadioGroupRenderer.kt, etc.
	private fun addSpacer(context: Context, container: ViewGroup) {
		val spacer = View(context)
		spacer.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			(context.resources.displayMetrics.density * 16).toInt() // Exemplo: 16dp
		)
		container.addView(spacer)
	}
}