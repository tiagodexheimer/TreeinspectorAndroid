package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import com.dexheimer.treeinspectorandroid.presentation.vistoria.VistoriaActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DemandaDetalheActivity : AppCompatActivity() {

    @Inject lateinit var repository: DemandaRepository

    private lateinit var demanda: Demanda

    // Launcher para pegar o resultado da VistoriaActivity
    private val vistoriaLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        val demandaExtra =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
                }

        if (demandaExtra != null) {
            demanda = demandaExtra
            preencherDados()
            buscarNotificacoes()
        } else {
            Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Botão de Ação (Corrigido para usar o ID do XML: btnFinalizarVistoria)
        val btnAcao: Button = findViewById(R.id.btnFinalizarVistoria)
        btnAcao.text = "Realizar Vistoria" // Ajustando o texto programaticamente para fazer sentido
        btnAcao.setOnClickListener { abrirTelaDeVistoria() }
    }

    private fun preencherDados() {
        // Tipo (detalheTipoTextView)
        // CORREÇÃO: tipo_demanda -> tipoDemanda
        findViewById<TextView>(R.id.detalheTipoTextView).text =
                "Tipo: ${demanda.tipoDemanda ?: "-"}"

        // Protocolo/ID (detalheIdTextView)
        findViewById<TextView>(R.id.detalheIdTextView).text =
                "Protocolo: ${demanda.protocolo ?: "-"}"

        // Endereço (detalheEnderecoTextView)
        findViewById<TextView>(R.id.detalheEnderecoTextView).text =
                "${demanda.logradouro ?: ""}, ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"

        // Descrição (detalheDescricaoTextView)
        findViewById<TextView>(R.id.detalheDescricaoTextView).text = demanda.descricao ?: "-"

        // Anexos
        val attachmentsLayout = findViewById<android.widget.LinearLayout>(R.id.attachmentsLayout)
        attachmentsLayout.removeAllViews()

        demanda.anexos?.forEach { anexo ->
            val btn = Button(this)
            btn.text = "📎 ${anexo.nome}"
            btn.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse(anexo.url)
                startActivity(intent)
            }
            attachmentsLayout.addView(btn)
        }
    }

    private fun buscarNotificacoes() {
        Log.d("DemandaDetalhe", "Buscando notificações para demanda: ${demanda.id}")
        val attachmentsLayout = findViewById<android.widget.LinearLayout>(R.id.attachmentsLayout)

        lifecycleScope.launch {
            try {
                val notificacoes = repository.getNotificacoesByDemanda(demanda.id)
                Log.d("DemandaDetalhe", "Notificações encontradas: ${notificacoes.size}")
                notificacoes.forEach { notificacao ->
                    notificacao.fotos?.forEach { anexo ->
                        val btn =
                                Button(
                                                this@DemandaDetalheActivity,
                                                null,
                                                com.google
                                                        .android
                                                        .material
                                                        .R
                                                        .style
                                                        .Widget_MaterialComponents_Button_OutlinedButton
                                        )
                                        .apply {
                                            text = "📋 [Notif] ${anexo.nome}"
                                            isAllCaps = false
                                            setOnClickListener {
                                                try {
                                                    val intent =
                                                            Intent(
                                                                    Intent.ACTION_VIEW,
                                                                    android.net.Uri.parse(anexo.url)
                                                            )
                                                    startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                                    context,
                                                                    "Não foi possível abrir o anexo.",
                                                                    Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                }
                                            }
                                            layoutParams =
                                                    android.widget.LinearLayout.LayoutParams(
                                                                    android.widget.LinearLayout
                                                                            .LayoutParams
                                                                            .MATCH_PARENT,
                                                                    android.widget.LinearLayout
                                                                            .LayoutParams
                                                                            .WRAP_CONTENT
                                                            )
                                                            .apply { setMargins(0, 0, 0, 8) }
                                        }
                        attachmentsLayout.addView(btn)
                    }
                }
            } catch (e: Exception) {
                Log.e("DemandaDetalhe", "Erro ao buscar notificações", e)
            }
        }
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
