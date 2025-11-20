package com.dexheimer.treeinspectorandroid.presentation.vistoria

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.usecase.SaveResult
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormRendererFactory
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint // <--- HILT: Ponto de entrada
class VistoriaActivity : AppCompatActivity() {

	// NOVO: Injetar a Fábrica de Renderizadores (Hilt faz a mágica)
	@Inject
	lateinit var rendererFactory: FormRendererFactory

	// --- Injeção do ViewModel ---
	private val viewModel: VistoriaViewModel by viewModels()

	// --- UI Components ---
	private lateinit var dynamicFormContainer: LinearLayout
	private lateinit var btnSalvar: Button
	private lateinit var progressBar: ProgressBar
	private lateinit var toolbar: androidx.appcompat.widget.Toolbar

	private lateinit var txtTipoDemanda: TextView
	private lateinit var txtEndereco: TextView
	private lateinit var txtDescricao: TextView

	// --- Estado Local da View ---
	private var demandaAtual: Demanda? = null
	private val formViews = mutableMapOf<String, View>()
	private var fieldDefinitions = emptyList<FormField>()


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_vistoria)

		// 1. Inicializar UI e Toolbar
		setupUI()

		// 2. Recuperar Demanda
		demandaAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		// 3. Preencher Dados e Iniciar Busca do Formulário
		if (demandaAtual != null) {
			// CORREÇÃO: Usando tipoDemanda (camelCase)
			txtTipoDemanda.text = demandaAtual?.tipoDemanda ?: "Não informado"
			txtEndereco.text = "${demandaAtual?.logradouro ?: ""}, ${demandaAtual?.numero ?: ""}\n${demandaAtual?.bairro ?: ""}"
			txtDescricao.text = demandaAtual?.descricao ?: "Sem descrição."

			// Chama o ViewModel para buscar e renderizar
			demandaAtual?.tipoDemanda?.let { viewModel.buscarFormulario(it) }
		} else {
			Toast.makeText(this, "Erro crítico: Dados não encontrados.", Toast.LENGTH_LONG).show()
			finish()
			return
		}

		// 4. Ação do Botão Salvar
		btnSalvar.setOnClickListener {
			demandaAtual?.let { d ->
				val respostas = coletarRespostas()
				// CORREÇÃO CRÍTICA: Chama o ViewModel para salvar
				viewModel.salvarVistoria(d, respostas)
			}
		}

		// 5. Observar Estado do ViewModel
		observarViewModel()
	}

	private fun setupUI() {
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Realizar Vistoria"

		dynamicFormContainer = findViewById(R.id.dynamicFormContainer)
		btnSalvar = findViewById(R.id.btnSalvarVistoria)
		progressBar = findViewById(R.id.progressBarForm)

		txtTipoDemanda = findViewById(R.id.txtTipoDemanda)
		txtEndereco = findViewById(R.id.txtEndereco)
		txtDescricao = findViewById(R.id.txtDescricao)
	}

	private fun observarViewModel() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					// 1. Lógica de Loading
					showLoading(state.isLoading)
					btnSalvar.text = if (state.isLoading) "Salvando..." else "Concluir Vistoria"
					btnSalvar.isEnabled = !state.isLoading && state.formFields.isNotEmpty()

					// 2. Renderização do Formulário
					if (state.formFields.isNotEmpty() && fieldDefinitions != state.formFields) {
						fieldDefinitions = state.formFields
						renderDynamicForm(state.formFields)
					}

					// 3. Lógica de Erro
					if (state.error != null) {
						mostrarMensagemNoContainer(state.error)
					}

					// 4. Resultado Final
					state.saveResult?.let { result ->
						when (result) {
							is SaveResult.SuccessOnline -> {
								finalizarComSucesso("Vistoria enviada com sucesso!", "concluido")
							}
							is SaveResult.SuccessOffline -> {
								finalizarComSucesso("Salvo offline. Será sincronizado automaticamente.", "concluido_pendente")
							}
							is SaveResult.Failure -> {
								// Se falhar localmente, mantém o botão ativo
								Toast.makeText(this@VistoriaActivity, result.message, Toast.LENGTH_LONG).show()
								btnSalvar.isEnabled = true
							}
						}
					}
				}
			}
		}
	}

	// --- LÓGICA DE COLETA DE DADOS ---

// Em VistoriaActivity.kt

	private fun coletarRespostas(): Map<String, Any> {
		val respostas = HashMap<String, Any>()

		// O loop foi simplificado, a responsabilidade de coleta está nos Renderers.
		for (campo in fieldDefinitions) {
			val view = formViews[campo.name]
			val renderer = rendererFactory.getRenderer(campo.type)

			if (view != null && renderer != null) {
				val resposta = renderer.collectResponse(view, campo)
				if (resposta != null) {
					respostas[campo.name] = resposta
				}
			}
		}
		return respostas
	}

	// --- LÓGICA DE RENDERIZAÇÃO (Mantida na View) ---

// Em VistoriaActivity.kt

	private fun renderDynamicForm(campos: List<FormField>) {
		dynamicFormContainer.removeAllViews()
		formViews.clear()

		// O loop foi simplificado, a responsabilidade de renderizar está nos Renderers.
		for (campo in campos) {
			val renderer = rendererFactory.getRenderer(campo.type)
			if (renderer != null) {
				val view = renderer.render(this, campo, dynamicFormContainer)
				formViews[campo.name] = view
			}
		}
		// Remove a lógica de espaçador que estava no final do loop original
		// (A lógica de espaçamento foi movida para dentro de cada Renderer)
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

	// --- HELPERS E FINALIZAÇÃO ---

	private fun finalizarComSucesso(msg: String, novoStatus: String) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
		val resultIntent = Intent()
		resultIntent.putExtra("NOVO_STATUS", novoStatus)
		resultIntent.putExtra("DEMANDA_ID", demandaAtual?.id)
		setResult(Activity.RESULT_OK, resultIntent)
		finish()
	}

	private fun showLoading(isLoading: Boolean) {
		progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
	}

	private fun mostrarMensagemNoContainer(msg: String) {
		dynamicFormContainer.removeAllViews()
		val tv = TextView(this)
		tv.text = msg
		tv.setTextColor(Color.RED)
		tv.textSize = 16f
		tv.gravity = Gravity.CENTER
		tv.setPadding(0, 20, 0, 20)
		dynamicFormContainer.addView(tv)
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}