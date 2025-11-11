package com.dexheimer.treeinspectorandroid

import android.app.Activity
import android.content.Intent // <-- IMPORT CORRETO
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DemandaDetalheActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_detalhe)

		// --- Configuração da Toolbar ---
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Detalhes da Vistoria"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		// --- Encontra as Views ---
		val idTextView: TextView = findViewById(R.id.detalheIdTextView)
		val enderecoTextView: TextView = findViewById(R.id.detalheEnderecoTextView)
		val tipoTextView: TextView = findViewById(R.id.detalheTipoTextView)
		val descricaoTextView: TextView = findViewById(R.id.detalheDescricaoTextView)
		val btnFinalizarVistoria: Button = findViewById(R.id.btnFinalizarVistoria)

		// --- Pega o objeto Demanda ---
		val demanda: Demanda? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		// --- Preenche os dados na tela ---
		if (demanda != null) {
			idTextView.text = "Demanda #${demanda.id}"
			enderecoTextView.text = "${demanda.logradouro ?: "Endereço"} ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"
			tipoTextView.text = "Tipo: ${demanda.tipo_demanda ?: "N/D"}"
			descricaoTextView.text = demanda.descricao ?: "Sem descrição."

			supportActionBar?.title = "Vistoria Demanda #${demanda.id}"


			// --- LÓGICA DO BOTÃO (MODIFICADO) ---
			// 1. Define o texto do botão com base no status ATUAL
			if (demanda.status_vistoria.equals("concluida", ignoreCase = true)) {
				btnFinalizarVistoria.text = "Marcar como PENDENTE (Refazer Vistoria)"
			} else {
				btnFinalizarVistoria.text = "Finalizar Vistoria (Marcar CONCLUÍDA)"
			}

			// 2. Configura o clique para ALTERNAR o status
			btnFinalizarVistoria.setOnClickListener {
				// Define qual será o NOVO status
				val novoStatus = if (demanda.status_vistoria.equals("concluida", ignoreCase = true)) {
					"pendente"
				} else {
					"concluida"
				}

				// 3. Devolve o novo status e o ID para a RotaDetalheActivity
				val resultIntent = Intent()
				resultIntent.putExtra("NOVO_STATUS", novoStatus)
				resultIntent.putExtra("DEMANDA_ID", demanda.id)

				// Envia os dados de volta e finaliza
				setResult(Activity.RESULT_OK, resultIntent)
				finish()
			}
			// --- FIM DA LÓGICA DO BOTÃO ---

		} else {
			idTextView.text = "Erro ao carregar demanda"
			btnFinalizarVistoria.isEnabled = false // Desabilita o botão se der erro
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		// Se o usuário clicar em "Voltar" na toolbar, o resultado
		// (RESULT_CANCELED) será enviado, e a RotaDetalheActivity saberá
		// que NÃO deve alterar o status.
		setResult(Activity.RESULT_CANCELED)
		onBackPressedDispatcher.onBackPressed()
		return true
	}
} // <-- ESTA CHAVE FINAL ESTAVA EM FALTA