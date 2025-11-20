package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DemandaListActivity : AppCompatActivity() {

	// ... (outros atributos permanecem iguais)
	private val viewModel: DemandasListViewModel by viewModels()
	private lateinit var demandasRecyclerView: RecyclerView
	private lateinit var demandaAdapter: DemandaAdapter
	private var rotaId: Int = -1

	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		// ... (código do launcher igual)
		if (result.resultCode == Activity.RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS")
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1
			if (novoStatus != null && demandaId != -1) {
				viewModel.atualizarStatusDemanda(demandaId, novoStatus)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_list)

		rotaId = intent.getIntExtra("ROTA_ID", -1)

		setupToolbar()
		setupRecyclerView()
		observarViewModel()

		// --- CORREÇÃO: Substitui o onBackPressed() ---
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				setResult(Activity.RESULT_OK)
				finish()
			}
		})
	}

	// ... (setupToolbar, setupRecyclerView, observarViewModel, abrirVistoria permanecem iguais)
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
					if (state.isLoading) supportActionBar?.subtitle = "Carregando..."
					if (state.error != null) Toast.makeText(this@DemandaListActivity, state.error, Toast.LENGTH_LONG).show()
					demandaAdapter.updateData(state.demandas)
					val pendentes = state.demandas.count { it.statusVistoria == "pendente" }
					supportActionBar?.subtitle = "$pendentes demandas pendentes"
				}
			}
		}
	}

	private fun abrirVistoria(demanda: Demanda) {
		val intent = Intent(this, VistoriaActivity::class.java).apply {
			putExtra("DEMANDA_EXTRA", demanda)
		}
		vistoriaLauncher.launch(intent)
	}

	override fun onSupportNavigateUp(): Boolean {
		setResult(Activity.RESULT_OK)
		finish()
		return true
	}

	// REMOVIDO: override fun onBackPressed() { ... }
}