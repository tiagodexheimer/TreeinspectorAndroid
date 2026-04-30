package com.dexheimer.treeinspectorandroid.presentation.vistoria

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VisualizadorImagemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Tela cheia
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_visualizador_imagem)

        val imageView = findViewById<ImageView>(R.id.imgFullRes)
        val btnFechar = findViewById<ImageButton>(R.id.btnFecharVisualizador)
        
        val imagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        btnFechar.setOnClickListener { finish() }

        if (imagePath.isNotEmpty()) {
            carregarImagem(imagePath, imageView)
        }
    }

    private fun carregarImagem(path: String, imageView: ImageView) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var finalPath = path
                val file = File(path)

                // Fallback para WebP se o arquivo original sumiu
                if (path.startsWith("/") && !file.exists()) {
                    val webpPath = path.substringBeforeLast(".") + ".webp"
                    if (File(webpPath).exists()) finalPath = webpPath
                }

                val bitmap = if (finalPath.startsWith("http")) {
                    val connection = java.net.URL(finalPath).openConnection()
                    connection.doInput = true
                    connection.connect()
                    BitmapFactory.decodeStream(connection.getInputStream())
                } else {
                    // Aqui carregamos com sampleSize menor (2) para manter mais detalhe que o preview
                    val options = BitmapFactory.Options().apply { inSampleSize = 1 } 
                    BitmapFactory.decodeFile(finalPath, options)
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("Visualizador", "Erro ao carregar imagem full", e)
            }
        }
    }
}
