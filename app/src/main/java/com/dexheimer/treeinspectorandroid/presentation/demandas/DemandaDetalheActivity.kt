package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity

class DemandaDetalheActivity : AppCompatActivity() {

	private lateinit var demanda: Demanda

	// Launcher para pegar o resultado da VistoriaActivity
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == RESULT_OK) {
			setResult(RESULT_OK, result.data)
			finish()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_detalhe)

		// Toolbar
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Detalhes da Demanda"

		// Recuperar objeto Demanda
		val demandaExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		if (demandaExtra != null) {
			demanda = demandaExtra
			preencherDados()
		} else {
			Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
			finish()
			return
		}

		// Botão de Ação (Corrigido para usar o ID do XML: btnFinalizarVistoria)
		val btnAcao: Button = findViewById(R.id.btnFinalizarVistoria)
		btnAcao.text = "Realizar Vistoria" // Ajustando o texto programaticamente para fazer sentido
		btnAcao.setOnClickListener {
			abrirTelaDeVistoria()
		}
	}

	private fun preencherDados() {
		// Tipo (detalheTipoTextView)
		// CORREÇÃO: tipo_demanda -> tipoDemanda
		findViewById<TextView>(R.id.detalheTipoTextView).text = "Tipo: ${demanda.tipoDemanda ?: "-"}"

		// Protocolo/ID (detalheIdTextView)
		findViewById<TextView>(R.id.detalheIdTextView).text = "Protocolo: ${demanda.protocolo ?: "-"}"

		// Endereço (detalheEnderecoTextView)
		findViewById<TextView>(R.id.detalheEnderecoTextView).text =
			"${demanda.logradouro ?: ""}, ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"

		// Descrição (detalheDescricaoTextView)
		findViewById<TextView>(R.id.detalheDescricaoTextView).text = demanda.descricao ?: "-"
	}

	private fun abrirTelaDeVistoria() {
		val intent = Intent(this, VistoriaActivity::class.java)
		intent.putExtra("DEMANDA_EXTRA", demanda)
		vistoriaLauncher.launch(intent)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			finish()
			return true
		}
		return super.onOptionsItemSelected(item)
	}
}