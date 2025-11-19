package com.dexheimer.treeinspectorandroid.presentation.vistoria

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.data.local.AppDatabase
import com.dexheimer.treeinspectorandroid.data.local.FormularioCache
import com.dexheimer.treeinspectorandroid.data.local.VistoriaPendente
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.data.remote.NetworkClient
import com.dexheimer.treeinspectorandroid.data.remote.VistoriaRequest
import com.dexheimer.treeinspectorandroid.data.worker.SyncVistoriasWorker
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VistoriaActivity : AppCompatActivity() {

	// --- UI Components ---
	private lateinit var dynamicFormContainer: LinearLayout
	private lateinit var btnSalvar: Button
	private lateinit var progressBar: ProgressBar

	private lateinit var txtTipoDemanda: TextView
	private lateinit var txtEndereco: TextView
	private lateinit var txtDescricao: TextView

	// --- Dados e Estado ---
	private var demandaAtual: Demanda? = null
	private val formViews = mutableMapOf<String, View>()
	private val fieldDefinitions = mutableListOf<FormField>()
	private lateinit var db: AppDatabase // Banco de dados local

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_vistoria)

		// 1. Inicializar Banco de Dados
		db = AppDatabase.Companion.getInstance(applicationContext)

		// 2. Configurar Toolbar
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Realizar Vistoria"

		// 3. Inicializar UI
		dynamicFormContainer = findViewById(R.id.dynamicFormContainer)
		btnSalvar = findViewById(R.id.btnSalvarVistoria)
		progressBar = findViewById(R.id.progressBarForm)

		txtTipoDemanda = findViewById(R.id.txtTipoDemanda)
		txtEndereco = findViewById(R.id.txtEndereco)
		txtDescricao = findViewById(R.id.txtDescricao)

		// 4. Recuperar Demanda
		demandaAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		// 5. Preencher Dados e Buscar Formulário
		if (demandaAtual != null) {
			txtTipoDemanda.text = demandaAtual?.tipoDemanda ?: "Não informado"
			txtEndereco.text = "${demandaAtual?.logradouro ?: ""}, ${demandaAtual?.numero ?: ""}\n${demandaAtual?.bairro ?: ""}"
			txtDescricao.text = demandaAtual?.descricao ?: "Sem descrição."

			buscarFormulario(demandaAtual!!.tipoDemanda ?: "")
		} else {
			Toast.makeText(this, "Erro crítico: Dados não encontrados.", Toast.LENGTH_LONG).show()
			finish()
		}

		// 6. Ação do Botão Salvar
		btnSalvar.setOnClickListener {
			salvarVistoria()
		}
	}

	// --- LÓGICA DE BUSCA (ONLINE + OFFLINE FALLBACK) ---

	private fun buscarFormulario(tipoDemanda: String) {
		showLoading(true)
		dynamicFormContainer.removeAllViews()

		lifecycleScope.launch {
			var campos: List<FormField>? = null

			// 1. Tenta buscar da API (Online)
			try {
				Log.d("VistoriaActivity", "Tentando buscar formulário online para: $tipoDemanda")
				val response = NetworkClient.api.getFormularioPorTipo(tipoDemanda)
				if (response.isSuccessful && response.body() != null) {
					campos = response.body()
					// Opcional: Atualizar o cache com a versão mais nova
					salvarFormularioNoCache(tipoDemanda, campos!!)
				}
			} catch (e: Exception) {
				Log.w("VistoriaActivity", "Sem conexão ou erro na API. Tentando cache local...")
			}

			// 2. Se falhou (null), busca do Banco Local (Offline)
			if (campos == null) {
				val jsonCache = withContext(Dispatchers.IO) {
					db.formularioDao().getFormularioJson(tipoDemanda)
				}

				if (jsonCache != null) {
					try {
						val listType = object : TypeToken<List<FormField>>() {}.type
						campos = Gson().fromJson(jsonCache, listType)
						Toast.makeText(this@VistoriaActivity, "Modo Offline: Formulário carregado.", Toast.LENGTH_SHORT).show()
					} catch (e: Exception) {
						Log.e("VistoriaActivity", "Erro ao ler cache do formulário", e)
					}
				}
			}

			// 3. Renderiza ou mostra erro
			if (campos != null && campos.isNotEmpty()) {
				fieldDefinitions.clear()
				fieldDefinitions.addAll(campos)
				renderDynamicForm(campos)
				btnSalvar.isEnabled = true
			} else {
				mostrarMensagemNoContainer("Formulário não disponível (Offline e sem cache).")
				btnSalvar.isEnabled = false
			}
			showLoading(false)
		}
	}

	private suspend fun salvarFormularioNoCache(tipo: String, campos: List<FormField>) {
		withContext(Dispatchers.IO) {
			try {
				val json = Gson().toJson(campos)
				db.formularioDao().salvarFormulario(FormularioCache(tipo, json))
			} catch (e: Exception) {
				Log.e("VistoriaActivity", "Erro ao salvar cache do formulário", e)
			}
		}
	}

	// --- RENDERIZAÇÃO DINÂMICA ---

	private fun renderDynamicForm(campos: List<FormField>) {
		formViews.clear()

		for (campo in campos) {
			when (campo.type) {
				"textarea", "text" -> renderTextField(campo)
				"radio" -> renderRadioGroup(campo)
				"select" -> renderSpinner(campo)
				"switch" -> renderSwitch(campo)
				"checkbox" -> renderCheckbox(campo)
				else -> Log.w("VistoriaActivity", "Tipo desconhecido: ${campo.type}")
			}

			// Espaçamento
			val spacer = View(this)
			spacer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 48)
			dynamicFormContainer.addView(spacer)
		}
	}

	private fun renderTextField(campo: FormField) {
		val textInputLayout = TextInputLayout(this)
		textInputLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		textInputLayout.hint = campo.label
		textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE

		val editText = TextInputEditText(textInputLayout.context)
		editText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

		if (campo.type == "textarea") {
			editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
			editText.minLines = campo.rows ?: 3
			editText.gravity = Gravity.TOP or Gravity.START
		} else {
			editText.inputType = InputType.TYPE_CLASS_TEXT
		}

		textInputLayout.addView(editText)
		dynamicFormContainer.addView(textInputLayout)
		formViews[campo.name] = editText
	}

	private fun renderRadioGroup(campo: FormField) {
		val labelView = TextView(this)
		labelView.text = campo.label
		labelView.textSize = 16f
		labelView.setTextColor(Color.BLACK)
		labelView.typeface = Typeface.DEFAULT_BOLD
		labelView.setPadding(0, 0, 0, 16)
		dynamicFormContainer.addView(labelView)

		val radioGroup = RadioGroup(this)
		radioGroup.orientation = RadioGroup.VERTICAL

		campo.options?.forEach { option ->
			val radioButton = RadioButton(this)
			radioButton.text = option.label
			radioButton.tag = option.value
			radioButton.id = View.generateViewId()
			radioGroup.addView(radioButton)
		}

		dynamicFormContainer.addView(radioGroup)
		formViews[campo.name] = radioGroup
	}

	private fun renderSpinner(campo: FormField) {
		val labelView = TextView(this)
		labelView.text = campo.label
		labelView.textSize = 16f
		labelView.setTextColor(Color.BLACK)
		labelView.typeface = Typeface.DEFAULT_BOLD
		labelView.setPadding(0, 0, 0, 8)
		dynamicFormContainer.addView(labelView)

		val spinner = Spinner(this)
		spinner.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

		val opcoesLista = mutableListOf("Selecione...")
		val valoresLista = mutableListOf("")

		campo.options?.forEach { option ->
			opcoesLista.add(option.label)
			valoresLista.add(option.value)
		}

		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoesLista)
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		spinner.adapter = adapter
		spinner.tag = valoresLista

		dynamicFormContainer.addView(spinner)
		formViews[campo.name] = spinner
	}

	private fun renderSwitch(campo: FormField) {
		val switchView = SwitchMaterial(this)
		switchView.text = campo.label
		switchView.textSize = 16f
		switchView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

		if (campo.defaultValue.toString() == "true") {
			switchView.isChecked = true
		}

		dynamicFormContainer.addView(switchView)
		formViews[campo.name] = switchView
	}

	private fun renderCheckbox(campo: FormField) {
		val checkBox = CheckBox(this)
		checkBox.text = campo.label
		checkBox.textSize = 16f
		checkBox.setTextColor(Color.BLACK)
		checkBox.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

		if (campo.defaultValue.toString() == "true") {
			checkBox.isChecked = true
		}

		dynamicFormContainer.addView(checkBox)
		formViews[campo.name] = checkBox
	}

	// --- LÓGICA DE SALVAR (ONLINE + OFFLINE QUEUE) ---

	private fun salvarVistoria() {
		// 1. Coletar Respostas
		val respostas = HashMap<String, Any>()

		for (campo in fieldDefinitions) {
			val view = formViews[campo.name]
			if (view != null) {
				when (campo.type) {
					"textarea", "text" -> {
						respostas[campo.name] = (view as EditText).text.toString()
					}
					"radio" -> {
						val rg = view as RadioGroup
						val selectedId = rg.checkedRadioButtonId
						if (selectedId != -1) {
							val rb = rg.findViewById<RadioButton>(selectedId)
							respostas[campo.name] = rb.tag.toString()
						} else {
							respostas[campo.name] = ""
						}
					}
					"select" -> {
						val spinner = view as Spinner
						val position = spinner.selectedItemPosition
						val valores = spinner.tag as List<String>
						if (position > 0 && position < valores.size) {
							respostas[campo.name] = valores[position]
						} else {
							respostas[campo.name] = ""
						}
					}
					"switch", "checkbox" -> {
						respostas[campo.name] = (view as CompoundButton).isChecked
					}
				}
			}
		}

		if (demandaAtual == null) return

		showLoading(true)
		btnSalvar.isEnabled = false
		btnSalvar.text = "Salvando..."

		lifecycleScope.launch {
			var salvouOnline = false

			// 2. Tentar Enviar Online Primeiro
			try {
				val request = VistoriaRequest(demandaAtual!!.id, respostas)
				val response = NetworkClient.api.salvarVistoria(request)
				if (response.isSuccessful) {
					salvouOnline = true
				}
			} catch (e: Exception) {
				Log.w("VistoriaActivity", "Falha envio online: ${e.message}")
			}

			if (salvouOnline) {
				finalizarComSucesso("Vistoria enviada com sucesso!", "concluido")
			} else {
				// 3. Salvar na Fila Offline se falhar
				salvarLocalmenteParaSincronizar(respostas)
			}
		}
	}

	private suspend fun salvarLocalmenteParaSincronizar(respostas: Map<String, Any>) {
		try {
			val gson = Gson()
			val jsonRespostas = gson.toJson(respostas)

			val vistoriaPendente = VistoriaPendente(
				demandaId = demandaAtual!!.id,
				jsonRespostas = jsonRespostas
			)

			withContext(Dispatchers.IO) {
				// Salva na fila
				db.vistoriaDao().adicionarFila(vistoriaPendente)

				// Atualiza status local para usuário não fazer de novo
				// Usamos um status especial para indicar que falta sync
				db.demandaDao().updateStatus(demandaAtual!!.id, "concluido_pendente")
			}

			// Agenda o Worker para rodar quando tiver internet
			agendarSincronizacao()

			finalizarComSucesso("Salvo offline. Será sincronizado automaticamente.", "concluido_pendente")

		} catch (e: Exception) {
			Log.e("VistoriaActivity", "Erro crítico ao salvar localmente", e)
			Toast.makeText(this, "Erro ao salvar dados. Tente novamente.", Toast.LENGTH_LONG).show()
			resetarBotoes()
		}
	}

	private fun agendarSincronizacao() {
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val syncRequest = OneTimeWorkRequestBuilder<SyncVistoriasWorker>()
			.setConstraints(constraints)
			.build()

		WorkManager.getInstance(applicationContext).enqueue(syncRequest)
	}

	private fun finalizarComSucesso(msg: String, novoStatus: String) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
		val resultIntent = Intent()
		resultIntent.putExtra("NOVO_STATUS", novoStatus)
		resultIntent.putExtra("DEMANDA_ID", demandaAtual?.id)
		setResult(RESULT_OK, resultIntent)
		finish()
	}

	// --- HELPERS ---

	private fun showLoading(isLoading: Boolean) {
		progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
	}

	private fun resetarBotoes() {
		showLoading(false)
		btnSalvar.isEnabled = true
		btnSalvar.text = "Concluir Vistoria"
	}

	private fun mostrarMensagemNoContainer(msg: String) {
		val tv = TextView(this)
		tv.text = msg
		tv.setTextColor(Color.RED)
		tv.textSize = 16f
		tv.gravity = Gravity.CENTER
		tv.setPadding(0, 20, 0, 20)
		dynamicFormContainer.addView(tv)
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressedDispatcher.onBackPressed()
		return true
	}
}