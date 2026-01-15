package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

// Lida com campos 'radio'
class RadioGroupRenderer @Inject constructor() : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("radio")

	override fun render(context: Context, field: FormField, container: ViewGroup, initialValue: Any?): View {
		val labelView = TextView(context).apply {
			text = field.label
			textSize = 16f
			setTextColor(Color.BLACK)
			typeface = Typeface.DEFAULT_BOLD
			setPadding(0, 0, 0, 16)
		}
		container.addView(labelView)

		val radioGroup = RadioGroup(context).apply {
			orientation = RadioGroup.VERTICAL
		}

		val selectedValue = initialValue as? String

		field.options?.forEach { option ->
			val radioButton = RadioButton(context).apply {
				text = option.label
				tag = option.value
				id = View.generateViewId()
				setTextColor(Color.BLACK)
				isChecked = option.value == selectedValue
			}
			radioGroup.addView(radioButton)
		}

		container.addView(radioGroup)
		addSpacer(context, container)
		return radioGroup
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		val rg = view as RadioGroup
		val selectedId = rg.checkedRadioButtonId
		return if (selectedId != -1) {
			val rb = rg.findViewById<RadioButton>(selectedId)
			rb.tag.toString()
		} else {
			""
		}
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