package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.google.android.material.switchmaterial.SwitchMaterial
import javax.inject.Inject

// Lida com campos 'switch' e 'checkbox'
class SwitchCheckboxRenderer @Inject constructor() : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("switch", "checkbox")

	override fun render(context: Context, field: FormField, container: ViewGroup): View {
		val view = if (field.type == "switch") {
			SwitchMaterial(context).apply {
				text = field.label
				textSize = 16f
			}
		} else { // checkbox
			CheckBox(context).apply {
				text = field.label
				textSize = 16f
				setTextColor(Color.BLACK)
			}
		}

		view.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

		if (field.defaultValue.toString().equals("true", ignoreCase = true)) {
			(view as CompoundButton).isChecked = true
		}

		container.addView(view)
		addSpacer(context, container)
		return view
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		return (view as CompoundButton).isChecked
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