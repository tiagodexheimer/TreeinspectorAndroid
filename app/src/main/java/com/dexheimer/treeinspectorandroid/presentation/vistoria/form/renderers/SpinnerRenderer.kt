package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

// Lida com campos 'select'
class SpinnerRenderer @Inject constructor() : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("select")

	override fun render(context: Context, field: FormField, container: ViewGroup): View {
		val labelView = TextView(context).apply {
			text = field.label
			textSize = 16f
			setTextColor(Color.BLACK)
			typeface = Typeface.DEFAULT_BOLD
			setPadding(0, 0, 0, 8)
		}
		container.addView(labelView)

		val spinner = Spinner(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}

		val opcoesLista = mutableListOf("Selecione...")
		val valoresLista = mutableListOf("")

		field.options?.forEach { option ->
			opcoesLista.add(option.label)
			valoresLista.add(option.value)
		}

		val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, opcoesLista) {
			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				val v = super.getView(position, convertView, parent)
				(v as? TextView)?.setTextColor(Color.BLACK)
				return v
			}
			override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
				val v = super.getDropDownView(position, convertView, parent)
				(v as? TextView)?.setTextColor(Color.BLACK)
				return v
			}
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		spinner.adapter = adapter
		spinner.tag = valoresLista // Usamos a tag para guardar os valores reais

		container.addView(spinner)
		addSpacer(context, container)
		return spinner
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		val spinner = view as Spinner
		val position = spinner.selectedItemPosition
		@Suppress("UNCHECKED_CAST")
		val valores = spinner.tag as? List<String> ?: emptyList()

		return if (position > 0 && position < valores.size) {
			valores[position]
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