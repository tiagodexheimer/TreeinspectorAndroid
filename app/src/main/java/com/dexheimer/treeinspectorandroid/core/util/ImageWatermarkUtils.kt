package com.dexheimer.treeinspectorandroid.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageWatermarkUtils {

    /**
     * Processa a imagem: redimensiona, rotaciona, aplica marca d'água e salva como WebP.
     * Tudo em um único passo eficiente para evitar OOM e lentidão.
     */
    fun compressAndWatermark(
        inputPath: String,
        outputPath: String,
        watermarkText: String,
        targetSize: Int = 1024
    ): Boolean {
        var bitmapOriginal: Bitmap? = null
        var resultBitmap: Bitmap? = null
        
        try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) return false

            // 1. Calcula inSampleSize para carregar imagem já reduzida
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputPath, options)
            
            val width = options.outWidth
            val height = options.outHeight
            var inSampleSize = 1
            if (width > targetSize || height > targetSize) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                while (halfWidth / inSampleSize >= targetSize && halfHeight / inSampleSize >= targetSize) {
                    inSampleSize *= 2
                }
            }

            // 2. Decodifica com inSampleSize
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            bitmapOriginal = BitmapFactory.decodeFile(inputPath, options) ?: return false

            // 3. Corrige rotação e redimensiona para tamanho exato
            val rotatedBitmap = rotateAndScale(bitmapOriginal, inputPath, targetSize)
            
            // 4. Prepara Canvas para Marca d'água
            resultBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            if (rotatedBitmap != bitmapOriginal) rotatedBitmap.recycle()
            
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = resultBitmap.height * 0.025f // 2.5% da altura
                isAntiAlias = true
                style = Paint.Style.FILL
                setShadowLayer(10f, 4f, 4f, Color.BLACK)
            }

            // 5. Desenha Marca d'água
            val linhas = watermarkText.split("\n")
            val lineHeight = paint.descent() - paint.ascent()
            val totalTextHeight = lineHeight * linhas.size
            val bottomMargin = 40f
            var y = resultBitmap.height - bottomMargin - totalTextHeight + lineHeight
            val x = 40f

            linhas.forEach { line ->
                canvas.drawText(line, x, y, paint)
                y += lineHeight
            }

            // 6. Salva como WebP
            val outFile = File(outputPath)
            val out = FileOutputStream(outFile)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                resultBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            } else {
                @Suppress("DEPRECATION")
                resultBitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }
            out.flush()
            out.close()

            return true

        } catch (e: Exception) {
            Log.e("Watermark", "Erro no processamento: ${e.message}", e)
            return false
        } finally {
            bitmapOriginal?.recycle()
            resultBitmap?.recycle()
        }
    }

    private fun rotateAndScale(bitmap: Bitmap, path: String, targetSize: Int): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        
        val matrix = Matrix()
        
        // Rotação
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        
        // Escalonamento para o tamanho alvo exato
        val currentWidth = bitmap.width
        val currentHeight = bitmap.height
        val maxDim = if (currentWidth > currentHeight) currentWidth else currentHeight
        
        if (maxDim > targetSize) {
            val scale = targetSize.toFloat() / maxDim
            matrix.postScale(scale, scale)
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, currentWidth, currentHeight, matrix, true)
    }

    /**
     * Mantido apenas para compatibilidade se houver chamadas antigas, 
     * mas o ideal é usar compressAndWatermark.
     */
    fun waterMarkImage(filePath: String, watermarkText: String): Boolean {
        return compressAndWatermark(filePath, filePath, watermarkText)
    }
}