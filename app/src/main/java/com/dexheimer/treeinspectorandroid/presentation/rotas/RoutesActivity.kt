package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.presentation.login.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RoutesActivity : AppCompatActivity() {

    private val viewModel: RotasViewModel by viewModels()
    @Inject lateinit var sessionManager: SessionManager
    private lateinit var adapter: RotasAdapter

    // Views (se não usar ViewBinding)
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var syncMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routes)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_logout) // Ícone de Logout

        swipeRefresh = findViewById(R.id.swipeRefresh)
        val recyclerView =
                findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerRotas)

        adapter = RotasAdapter { rotaId ->
            val intent = Intent(this, RotaDetalheActivity::class.java)
            intent.putExtra("ROTA_ID", rotaId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // LÓGICA DO PULL-TO-REFRESH
        swipeRefresh.setOnRefreshListener { viewModel.forcarSincronizacaoCompleta() }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading do Swipe
                    swipeRefresh.isRefreshing = state.isLoading

                    // Lista
                    adapter.submitList(state.rotas)

                    // Erro
                    if (state.error != null) {
                        Toast.makeText(this@RoutesActivity, state.error, Toast.LENGTH_LONG).show()
                    }

                    // Ícone de Sincronização
                    updateSyncIcon(state.pendingUploads)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.routes_menu, menu)
        syncMenuItem = menu?.findItem(R.id.action_sync_status)

        // Força atualização visual inicial do ícone
        viewModel.uiState.value.let { updateSyncIcon(it.pendingUploads) }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Logout real
                sessionManager.clearSession()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            R.id.action_sync_status -> {
                val pendentes = viewModel.uiState.value.pendingUploads
                if (pendentes > 0) {
                    Toast.makeText(
                                    this,
                                    "Existem $pendentes vistorias aguardando envio. Arraste para baixo para sincronizar.",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                } else {
                    Toast.makeText(this, "Tudo sincronizado!", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateSyncIcon(pendingCount: Int) {
        syncMenuItem?.let { item ->
            if (pendingCount > 0) {
                item.icon =
                        ContextCompat.getDrawable(
                                this,
                                R.drawable.ic_cloud_upload
                        ) // Ícone de "Falta subir" (Amarelo/Aviso)
                // Se quiser mudar a cor via código:
                // item.icon?.setTint(Color.YELLOW)
            } else {
                item.icon =
                        ContextCompat.getDrawable(
                                this,
                                R.drawable.ic_cloud_done
                        ) // Ícone de "Ok" (Verde/Branco)
            }
        }
    }
}
