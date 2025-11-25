package com.dexheimer.treeinspectorandroid.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageWatermarkUtils {

	/**
	 * Adiciona o texto fornecido (com quebras de linha) na imagem.
	 */
	fun waterMarkImage(filePath: String, watermarkText: String): Boolean {
		try {
			val file = File(filePath)
			if (!file.exists()) return false

			val bitmapOriginal = BitmapFactory.decodeFile(filePath) ?: return false
			val rotatedBitmap = rotateBitmapIfRequired(bitmapOriginal, filePath)

			val resultBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
			val canvas = Canvas(resultBitmap)

			// Configuração do Pincel
			val paint = Paint().apply {
				color = Color.WHITE
				textSize = resultBitmap.height * 0.035f // 3.5% da altura
				isAntiAlias = true
				style = Paint.Style.FILL
				setShadowLayer(10f, 4f, 4f, Color.BLACK) // Sombra forte para ler em qualquer fundo
			}

			// Posicionamento (Canto Inferior Esquerdo)
			val x = 40f
			var y = resultBitmap.height - 150f

			// Desenha cada linha do texto
			watermarkText.split("\n").forEach { line ->
				canvas.drawText(line, x, y, paint)
				y += paint.descent() - paint.ascent() // Move para linha de baixo
			}

			val out = FileOutputStream(file)
			resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
			out.flush()
			out.close()

			bitmapOriginal.recycle()
			if (rotatedBitmap != bitmapOriginal) rotatedBitmap.recycle()
			resultBitmap.recycle()

			return true

		} catch (e: Exception) {
			e.printStackTrace()
			return false
		}
	}

	private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
		val ei = ExifInterface(path)
		val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
		return when (orientation) {
			ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
			ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
			ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
			else -> bitmap
		}
	}

	private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
		val matrix = android.graphics.Matrix()
		matrix.postRotate(angle)
		return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
	}
}