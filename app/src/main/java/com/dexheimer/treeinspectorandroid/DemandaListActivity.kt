package com.dexheimer.treeinspectorandroid

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DemandaListActivity : AppCompatActivity() {

	private lateinit var db: AppDatabase
	private lateinit var demandasRecyclerView: RecyclerView
	private lateinit var demandaAdapter: DemandaAdapter
	private var rotaId: Int = -1
	private var demandasCarregadas: MutableList<Demanda> = mutableListOf()

	private val vistoriaLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val novoStatus = result.data?.getStringExtra("NOVO_STATUS") ?: return@registerForActivityResult
			val demandaId = result.data?.getIntExtra("DEMANDA_ID", -1) ?: -1
			if (demandaId != -1) {
				atualizarStatusDemanda(demandaId, novoStatus)
			}
		} else {
			Log.d("DemandaListActivity", "Vistoria não finalizada (usuário voltou).")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_demanda_list)

		db = AppDatabase.getInstance(applicationContext)
		rotaId = intent.getIntExtra("ROTA_ID", -1)

		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.title = "Todas as Demandas da Rota"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		// O DemandaAdapter receberá a lista de Demandas, que agora inclui os novos campos
		// (lat, lng, status_cor, etc.). O Adapter (não fornecido) deve ser capaz de usá-los.
		demandaAdapter = DemandaAdapter(emptyList()) { demandaClicada ->
			val intent = android.content.Intent(this, DemandaDetalheActivity::class.java).apply {
				// O objeto Demanda contém todos os novos dados, pois o Room o carregou completo
				putExtra("DEMANDA_EXTRA", demandaClicada)
			}
			vistoriaLauncher.launch(intent)
		}
		demandasRecyclerView.layoutManager = LinearLayoutManager(this)
		demandasRecyclerView.adapter = demandaAdapter

		carregarDemandasDoCache()
	}

	private fun carregarDemandasDoCache() {
		if (rotaId == -1) return
		lifecycleScope.launch {
			demandasCarregadas = withContext(Dispatchers.IO) {
				// Esta função retorna o objeto Demanda completo, com todos os novos campos
				db.demandaDao().getDemandasDaRota(rotaId)
			}.toMutableList()

			demandasCarregadas.sortBy { it.status_vistoria != "pendente" }
			demandaAdapter.updateData(demandasCarregadas)

			val pendentes = demandasCarregadas.count { it.status_vistoria == "pendente" }
			supportActionBar?.subtitle = "$pendentes demandas pendentes"
		}
	}

	private fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				db.demandaDao().updateStatus(demandaId, novoStatus)
			}
			demandasCarregadas.find { it.id == demandaId }?.status_vistoria = novoStatus
			demandasCarregadas.sortBy { it.status_vistoria != "pendente" }
			demandaAdapter.updateData(demandasCarregadas)
			val pendentes = demandasCarregadas.count { it.status_vistoria == "pendente" }
			supportActionBar?.subtitle = "$pendentes demandas pendentes"
		}
	}

	// Funções de navegação (Manter inalteradas)
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
