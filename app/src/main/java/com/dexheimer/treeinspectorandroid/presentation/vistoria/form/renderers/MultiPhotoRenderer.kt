package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setMargins
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import java.io.File

class MultiPhotoRenderer : FormFieldRenderer {

	// Tipos suportados no JSON
	override val supportedTypes: List<String> = listOf("photo", "photos", "image", "gallery")

	override fun render(context: Context, field: FormField, container: ViewGroup): View {
		// Container Principal do Campo
		val masterLayout = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { setMargins(0, 24, 0, 24) }
			background = context.getDrawable(android.R.drawable.dialog_holo_light_frame) // Opcional: borda leve
			setPadding(16, 16, 16, 16)
		}

		// 1. Label (Título do Campo)
		val label = TextView(context).apply {
			text = field.label
			textSize = 16f
			setTextColor(Color.BLACK)
			typeface = Typeface.DEFAULT_BOLD
			setPadding(0, 0, 0, 16)
		}
		masterLayout.addView(label)

		// 2. Container de Botões (Horizontal)
		val buttonsLayout = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			gravity = Gravity.CENTER_HORIZONTAL
		}

		// Botão Câmera
		val btnCamera = Button(context).apply {
			text = "📸 Câmera"
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
				marginEnd = 8
			}
			setOnClickListener {
				if (context is VistoriaActivity) {
					context.solicitarFoto(field.name)
				}
			}
		}

		// Botão Galeria
		val btnGaleria = Button(context).apply {
			text = "🖼️ Galeria"
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
				marginStart = 8
			}
			setOnClickListener {
				if (context is VistoriaActivity) {
					context.solicitarGaleria(field.name)
				}
			}
		}

		buttonsLayout.addView(btnCamera)
		buttonsLayout.addView(btnGaleria)
		masterLayout.addView(buttonsLayout)

		// 3. Scroll Horizontal para as Fotos (Miniaturas)
		val photosContainer = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			tag = "photos_container" // Tag importante para encontrar depois
		}

		val scrollView = HorizontalScrollView(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				300 // Altura fixa para as miniaturas (ajuste conforme necessário)
			).apply { topMargin = 16 }
			addView(photosContainer)
		}
		masterLayout.addView(scrollView)

		// 4. TextView oculto para armazenar os caminhos (Lógica de dados)
		val hiddenPathsView = TextView(context).apply {
			visibility = View.GONE
			tag = "paths_list"
			text = ""
		}
		masterLayout.addView(hiddenPathsView)

		container.addView(masterLayout)
		return masterLayout
	}

	override fun collectResponse(view: View, field: FormField): Any? {
		val hiddenView = view.findViewWithTag<TextView>("paths_list")
		val pathsString = hiddenView?.text?.toString() ?: ""

		return if (pathsString.isNotEmpty()) {
			// Retorna lista de strings para o JSON final
			pathsString.split("|").filter { it.isNotEmpty() }
		} else {
			null
		}
	}

	companion object {
		// Atualiza a UI adicionando a miniatura
		fun addPhotoToView(viewContainer: View, photoPath: String) {
			val context = viewContainer.context

			// 1. Atualiza dados ocultos
			val hiddenView = viewContainer.findViewWithTag<TextView>("paths_list")
			val currentPaths = hiddenView.text.toString()
			val newPaths = if (currentPaths.isEmpty()) photoPath else "$currentPaths|$photoPath"
			hiddenView.text = newPaths

			// 2. Adiciona miniatura visual
			val photosContainer = viewContainer.findViewWithTag<LinearLayout>("photos_container")

			val imageView = ImageView(context).apply {
				layoutParams = LinearLayout.LayoutParams(250, 250).apply { // Tamanho quadrado
					setMargins(0, 0, 16, 0)
				}
				scaleType = ImageView.ScaleType.CENTER_CROP
				background = context.getDrawable(android.R.drawable.screen_background_light_transparent)

				// Carrega a imagem (em produção, use Glide/Coil para performance)
				try {
					setImageBitmap(BitmapFactory.decodeFile(photoPath))
				} catch (e: Exception) {
					// Fallback se falhar
					setBackgroundColor(Color.LTGRAY)
				}
			}

			photosContainer.addView(imageView)
		}
	}
}