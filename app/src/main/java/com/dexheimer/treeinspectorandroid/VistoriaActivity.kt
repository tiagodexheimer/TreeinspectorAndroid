package com.dexheimer.treeinspectorandroid

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VistoriaActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_vistoria)

		val textViewTipoDemanda = findViewById<TextView>(R.id.textViewTipoDemanda)
		val textViewEndereco = findViewById<TextView>(R.id.textViewEndereco)
		val textViewBairro = findViewById<TextView>(R.id.textViewBairro)
		val buttonFinalizarVistoria = findViewById<Button>(R.id.buttonFinalizarVistoria)

		// Pegar o objeto Demanda que foi passado
		val demanda = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		if (demanda != null) {
			title = "Vistoria: ${demanda.tipo_demanda}"
			textViewTipoDemanda.text = "Tipo: ${demanda.tipo_demanda ?: "N/D"}"
			textViewEndereco.text = "Endereço: ${demanda.logradouro ?: ""}, ${demanda.numero ?: ""}"
			textViewBairro.text = "Bairro: ${demanda.bairro ?: "N/D"}"
		} else {
			title = "Erro"
			textViewEndereco.text = "Erro ao carregar demanda."
		}

		// Configura o botão de finalizar
		buttonFinalizarVistoria.setOnClickListener {
			// Informa à Activity anterior que a vistoria foi concluída
			setResult(Activity.RESULT_OK)
			// Fecha esta tela e volta para a tela do mapa
			finish()
		}
	}
}