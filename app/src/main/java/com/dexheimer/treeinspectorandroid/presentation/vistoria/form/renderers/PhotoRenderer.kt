package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import java.io.File

class PhotoRenderer : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("photo", "image")

	override fun render(context: Context, field: FormField, container: ViewGroup): View {
		val layout = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { setMargins(0, 16, 0, 16) }
		}

		// Label
		val label = TextView(context).apply {
			text = field.label
			textSize = 16f
			setPadding(0, 0, 0, 8)
		}

		// Preview da Imagem
		val imageView = ImageView(context).apply {
			id = View.generateViewId()
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				500 // Altura fixa ou ajustar conforme necessidade
			).apply { setMargins(0, 8, 0, 8) }
			scaleType = ImageView.ScaleType.CENTER_CROP
			visibility = View.GONE
			tag = "preview_image" // Tag para encontrar depois
		}

		// Botão de Captura
		val button = Button(context).apply {
			text = "Tirar Foto"
			setOnClickListener {
				if (context is VistoriaActivity) {
					// Chama o método na Activity passando o nome do campo
					context.solicitarFoto(field.name)
				}
			}
		}

		// TextView oculto para guardar o caminho do arquivo (Path)
		val pathView = TextView(context).apply {
			visibility = View.GONE
			tag = "path_value" // Tag para coleta de resposta
		}

		layout.addView(label)
		layout.addView(imageView)
		layout.addView(button)
		layout.addView(pathView)

		container.addView(layout)
		return layout
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		val pathView = view.findViewWithTag<TextView>("path_value")
		val path = pathView?.text?.toString()
		return if (path.isNullOrEmpty()) null else path
	}
}