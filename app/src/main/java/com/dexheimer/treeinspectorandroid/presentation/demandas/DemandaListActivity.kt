package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint // <--- ESSENCIAL: Permite a injeção do ViewModel
class DemandaListActivity : AppCompatActivity() {

	private val viewModel: DemandasListViewModel by viewModels() // Injeção do ViewModel

	private lateinit var demandasRecyclerView: RecyclerView
	private lateinit var demandaAdapter: DemandaAdapter
	private var rotaId: Int = -1

	// Launcher para pegar o resultado da VistoriaActivity
	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS")
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1

			if (novoStatus != null && demandaId != -1) {
				// CORREÇÃO: Chama o ViewModel para atualizar o status e reordenar a lista
				viewModel.atualizarStatusDemanda(demandaId, novoStatus)
			}
		} else {
			Log.d("DemandaListActivity", "Vistoria não finalizada (usuário voltou).")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_list)

		rotaId = intent.getIntExtra("ROTA_ID", -1)

		setupToolbar()
		setupRecyclerView()
		observarViewModel()
	}

	private fun setupToolbar() {
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Todas as Demandas da Rota"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	private fun setupRecyclerView() {
		demandasRecyclerView = findViewById(R.id.demandasRecyclerView)

		demandaAdapter = DemandaAdapter(emptyList()) { demandaClicada ->
			abrirVistoria(demandaClicada)
		}

		demandasRecyclerView.layoutManager = LinearLayoutManager(this)
		demandasRecyclerView.adapter = demandaAdapter
	}

	private fun observarViewModel() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					// 1. Erro ou Loading
					if (state.isLoading) {
						supportActionBar?.subtitle = "Carregando..."
					}
					if (state.error != null) {
						Toast.makeText(this@DemandaListActivity, state.error, Toast.LENGTH_LONG).show()
					}

					// 2. Dados
					demandaAdapter.updateData(state.demandas)

					// 3. Subtítulo (Calculado no ViewModel, mas aqui calculamos do objeto Domain)
					val pendentes = state.demandas.count { it.statusVistoria == "pendente" }
					supportActionBar?.subtitle = "$pendentes demandas pendentes"
				}
			}
		}
	}

	private fun abrirVistoria(demanda: Demanda) {
		val intent = Intent(this, DemandaDetalheActivity::class.java).apply {
			putExtra("DEMANDA_EXTRA", demanda)
		}
		vistoriaLauncher.launch(intent)
	}

	// Funções de navegação para voltar à RotaDetalheActivity
	override fun onSupportNavigateUp(): Boolean {
		setResult(Activity.RESULT_OK)
		finish()
		return true
	}

	override fun onBackPressed() {
		setResult(Activity.RESULT_OK)
		super.onBackPressed()
	}
}