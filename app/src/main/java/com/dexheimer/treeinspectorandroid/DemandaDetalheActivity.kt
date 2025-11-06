package com.dexheimer.treeinspectorandroid

import android.app.Activity // <-- IMPORTAR
import android.os.Build
import android.os.Bundle
import android.widget.Button // <-- IMPORTAR
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
		val descricaoTextView: TextView = findViewById(R.id.detalheDescricaoTextView)

		// --- ENCONTRAR O NOVO BOTÃO ---
		val btnFinalizarVistoria: Button = findViewById(R.id.btnFinalizarVistoria)

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
			descricaoTextView.text = demanda.descricao ?: "Sem descrição."

			supportActionBar?.title = "Vistoria Demanda #${demanda.id}"
		} else {
			idTextView.text = "Erro ao carregar demanda"
			btnFinalizarVistoria.isEnabled = false // Desabilita o botão se der erro
		}

		// --- CONFIGURAR O CLIQUE DO BOTÃO ---
		btnFinalizarVistoria.setOnClickListener {
			// Sinaliza para a Activity anterior que a vistoria foi concluída
			setResult(Activity.RESULT_OK)
			// Fecha esta tela
			finish()
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		// Se o usuário clicar em "Voltar" na toolbar, o resultado padrão
		// (RESULT_CANCELED) será enviado, e a RotaDetalheActivity saberá
		// que não deve remover o item da lista.
		onBackPressedDispatcher.onBackPressed()
		return true
	}
}