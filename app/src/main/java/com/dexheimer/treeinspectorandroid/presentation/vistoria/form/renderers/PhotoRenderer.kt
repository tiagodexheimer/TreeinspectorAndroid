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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.content.Intent
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VisualizadorImagemActivity

class PhotoRenderer : FormFieldRenderer {

	override val supportedTypes: List<String> = listOf("photo", "image")

	override fun render(context: Context, field: FormField, container: ViewGroup, initialValue: Any?): View {
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

		// TextView oculto para guardar o caminho do arquivo (Path)
		val pathView = TextView(context).apply {
			visibility = View.GONE
			tag = "path_value" // Tag para coleta de resposta
		}

		// Layout para botões (Tirar/Alterar + Limpar)
		val buttonLayout = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}

		// Botão de Captura
		val button = Button(context).apply {
			text = "Tirar Foto"
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
			setOnClickListener {
				if (context is VistoriaActivity) {
					// Chama o método na Activity passando o nome do campo
					context.solicitarFoto(field.name)
				}
			}
		}

		// Botão de Limpar
		val btnClear = Button(context).apply {
			text = "Limpar"
			tag = "btn_clear"
			visibility = View.GONE
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
				marginStart = 8
			}
			setOnClickListener {
				pathView.text = ""
				imageView.visibility = View.GONE
				button.text = "Tirar Foto"
				visibility = View.GONE
			}
		}

		buttonLayout.addView(button)
		buttonLayout.addView(btnClear)

		// Restore state
		if (initialValue is String && initialValue.isNotEmpty()) {
			pathView.text = initialValue
			imageView.visibility = View.VISIBLE
			button.text = "Alterar Foto"
			btnClear.visibility = View.VISIBLE

			imageView.setOnClickListener {
				val intent = Intent(context, VisualizadorImagemActivity::class.java)
				intent.putExtra("IMAGE_PATH", initialValue)
				context.startActivity(intent)
			}

			if (context is VistoriaActivity) {
				context.carregarImagemNoImageView(initialValue, imageView)
			}
		}

		layout.addView(label)
		layout.addView(imageView)
		layout.addView(buttonLayout)
		layout.addView(pathView)

		container.addView(layout)
		return layout
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		val pathView = view.findViewWithTag<TextView>("path_value")
		val path = pathView?.text?.toString()
		return if (path.isNullOrEmpty()) null else path
	}

	companion object {
		fun updatePhoto(viewContainer: View, photoPath: String) {
			val context = viewContainer.context
			
			// 1. Atualiza o caminho oculto
			val pathView = viewContainer.findViewWithTag<TextView>("path_value") ?: return
			pathView.text = photoPath

			// 2. Atualiza o preview visual
			val imageView = viewContainer.findViewWithTag<ImageView>("preview_image") ?: return
			imageView.visibility = View.VISIBLE
			
			// 3. Atualiza os botões
			if (viewContainer is ViewGroup) {
				for (i in 0 until viewContainer.childCount) {
					val row = viewContainer.getChildAt(i)
					if (row is ViewGroup) {
						for (j in 0 until row.childCount) {
							val child = row.getChildAt(j)
							if (child is Button) {
								if (child.tag == "btn_clear") {
									child.visibility = View.VISIBLE
								} else {
									child.text = "Alterar Foto"
								}
							}
						}
					}
				}
			}

			// 4. Clique para zoom
			imageView.setOnClickListener {
				val intent = Intent(context, VisualizadorImagemActivity::class.java)
				intent.putExtra("IMAGE_PATH", photoPath)
				context.startActivity(intent)
			}

			// 4. Carrega o bitmap de forma assíncrona
			if (context is VistoriaActivity) {
				context.carregarImagemNoImageView(photoPath, imageView)
			}
		}
	}
}