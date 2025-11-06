package com.dexheimer.treeinspectorandroid

// 1. IMPORTS ADICIONADOS
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
// (Remova as importações não utilizadas de activity e core.view)

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// (enableEdgeToEdge() removido - CORRETO)
		setContentView(R.layout.activity_main)

		// Configuração da Toolbar (CORRETO)
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Tree Inspector"

		// 2. LÓGICA DO BOTÃO ADICIONADA
		// Encontra o botão que acabamos de adicionar no XML
		val btnVerRotas: Button = findViewById(R.id.btnVerRotas)

		// Define o que acontece ao clicar no botão
		btnVerRotas.setOnClickListener {
			// Cria uma intenção para abrir a RoutesActivity
			val intent = Intent(this, RoutesActivity::class.java)
			startActivity(intent)
		}

		// (Listener de Insets removido - CORRETO)
	}
}