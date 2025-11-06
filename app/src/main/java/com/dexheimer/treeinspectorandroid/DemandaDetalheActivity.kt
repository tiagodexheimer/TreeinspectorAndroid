package com.dexheimer.treeinspectorandroid

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DemandaDetalheActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_detalhe)

		// ... (Configuração da Toolbar)
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Detalhes da Vistoria"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		// Encontra os TextViews
		val idTextView: TextView = findViewById(R.id.detalheIdTextView)
		val enderecoTextView: TextView = findViewById(R.id.detalheEnderecoTextView)
		val tipoTextView: TextView = findViewById(R.id.detalheTipoTextView)
		val descricaoTextView: TextView = findViewById(R.id.detalheDescricaoTextView) // <-- Este

		// Pega o objeto Demanda
		val demanda: Demanda? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		// Preenche os dados na tela
		if (demanda != null) {
			idTextView.text = "Demanda #${demanda.id}"
			enderecoTextView.text = "${demanda.logradouro ?: "Endereço"} ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"
			tipoTextView.text = "Tipo: ${demanda.tipo_demanda ?: "N/D"}"

			// *** VERIFIQUE ESTA LINHA ***
			// Garante que a descrição está sendo exibida
			descricaoTextView.text = demanda.descricao ?: "Sem descrição."

			supportActionBar?.title = "Vistoria Demanda #${demanda.id}"
		} else {
			idTextView.text = "Erro ao carregar demanda"
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}
}