package com.dexheimer.treeinspectorandroid

import android.app.Activity
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class VistoriaActivity : AppCompatActivity() {

	// Referências de UI
	private lateinit var dynamicFormContainer: LinearLayout
	private lateinit var btnSalvar: Button
	private lateinit var progressBar: ProgressBar

	// Referências dos campos de texto estáticos
	private lateinit var txtTipoDemanda: TextView
	private lateinit var txtEndereco: TextView
	private lateinit var txtDescricao: TextView

	// Dados e Controle
	private var demandaAtual: Demanda? = null
	private val formViews = mutableMapOf<String, View>()
	private val fieldDefinitions = mutableListOf<FormField>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_vistoria)

		// 1. Configurar a Toolbar
		val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = "Realizar Vistoria"

		// 2. Inicializar Componentes de UI
		dynamicFormContainer = findViewById(R.id.dynamicFormContainer)
		btnSalvar = findViewById(R.id.btnSalvarVistoria)
		progressBar = findViewById(R.id.progressBarForm)

		txtTipoDemanda = findViewById(R.id.txtTipoDemanda)
		txtEndereco = findViewById(R.id.txtEndereco)
		txtDescricao = findViewById(R.id.txtDescricao)

		// 3. Recuperar a Demanda passada via Intent
		demandaAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		// 4. Preencher Dados e Iniciar Busca
		if (demandaAtual != null) {
			txtTipoDemanda.text = demandaAtual?.tipo_demanda ?: "Não informado"
			txtEndereco.text = "${demandaAtual?.logradouro ?: ""}, ${demandaAtual?.numero ?: ""}\n${demandaAtual?.bairro ?: ""}"
			txtDescricao.text = demandaAtual?.descricao ?: "Sem descrição."

			// Inicia a busca do formulário
			buscarFormulario(demandaAtual!!.tipo_demanda ?: "")
		} else {
			Toast.makeText(this, "Erro crítico: Dados da demanda não encontrados.", Toast.LENGTH_LONG).show()
			finish()
		}

		// 5. Configurar Botão de Salvar
		btnSalvar.setOnClickListener {
			salvarVistoria()
		}
	}

	// --- LÓGICA DE BUSCA DO FORMULÁRIO ---

	private fun buscarFormulario(tipoDemanda: String) {
		showLoading(true)
		dynamicFormContainer.removeAllViews()

		lifecycleScope.launch {
			try {
				Log.d("VistoriaActivity", "Buscando formulário para: $tipoDemanda")
				val response = NetworkClient.api.getFormularioPorTipo(tipoDemanda)

				if (response.isSuccessful && response.body() != null) {
					val campos = response.body()!!

					fieldDefinitions.clear()
					fieldDefinitions.addAll(campos)

					if (campos.isEmpty()) {
						mostrarMensagemNoContainer("Nenhum formulário configurado para este tipo de demanda ($tipoDemanda).")
						btnSalvar.isEnabled = false
					} else {
						renderDynamicForm(campos)
						btnSalvar.isEnabled = true
					}
				} else {
					Log.e("VistoriaActivity", "Erro API: ${response.code()}")
					mostrarMensagemNoContainer("Erro ao carregar formulário. Código: ${response.code()}")
				}
			} catch (e: Exception) {
				Log.e("VistoriaActivity", "Erro de Rede", e)
				mostrarMensagemNoContainer("Falha na conexão. Verifique sua internet.")
			} finally {
				showLoading(false)
			}
		}
	}

	// --- LÓGICA DE RENDERIZAÇÃO DINÂMICA ---

	private fun renderDynamicForm(campos: List<FormField>) {
		formViews.clear()

		for (campo in campos) {
			when (campo.type) {
				"textarea", "text" -> renderTextField(campo)
				"radio" -> renderRadioGroup(campo)      // Apenas Radio
				"select" -> renderSpinner(campo)        // Novo Spinner para Select
				"switch" -> renderSwitch(campo)         // Apenas Switch
				"checkbox" -> renderCheckbox(campo)     // Novo Checkbox
				else -> Log.w("VistoriaActivity", "Tipo de campo desconhecido: ${campo.type}")
			}

			// Espaçamento entre campos
			val spacer = View(this)
			spacer.layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				48
			)
			dynamicFormContainer.addView(spacer)
		}
	}

	private fun renderTextField(campo: FormField) {
		val textInputLayout = TextInputLayout(this)
		textInputLayout.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)
		textInputLayout.hint = campo.label
		textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE

		val editText = TextInputEditText(textInputLayout.context)
		editText.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

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

	// --- NOVO: RENDERIZAÇÃO DO SPINNER (LISTA SUSPENSA) ---
	private fun renderSpinner(campo: FormField) {
		val labelView = TextView(this)
		labelView.text = campo.label
		labelView.textSize = 16f
		labelView.setTextColor(Color.BLACK)
		labelView.typeface = Typeface.DEFAULT_BOLD
		labelView.setPadding(0, 0, 0, 8)
		dynamicFormContainer.addView(labelView)

		val spinner = Spinner(this)
		spinner.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

		val opcoesLista = mutableListOf("Selecione...")
		val valoresLista = mutableListOf("")

		campo.options?.forEach { option ->
			opcoesLista.add(option.label)
			valoresLista.add(option.value)
		}

		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoesLista)
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		spinner.adapter = adapter
		spinner.tag = valoresLista // Guarda os valores técnicos na tag

		dynamicFormContainer.addView(spinner)
		formViews[campo.name] = spinner
	}

	private fun renderSwitch(campo: FormField) {
		val switchView = SwitchMaterial(this)
		switchView.text = campo.label
		switchView.textSize = 16f
		switchView.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

		val defValue = campo.defaultValue.toString()
		if (defValue == "true") {
			switchView.isChecked = true
		}

		dynamicFormContainer.addView(switchView)
		formViews[campo.name] = switchView
	}

	// --- NOVO: RENDERIZAÇÃO DO CHECKBOX ---
	private fun renderCheckbox(campo: FormField) {
		val checkBox = CheckBox(this)
		checkBox.text = campo.label
		checkBox.textSize = 16f
		checkBox.setTextColor(Color.BLACK)
		checkBox.layoutParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		)

		val defValue = campo.defaultValue.toString()
		if (defValue == "true") {
			checkBox.isChecked = true
		}

		dynamicFormContainer.addView(checkBox)
		formViews[campo.name] = checkBox
	}

	// --- LÓGICA DE SALVAR ---

	private fun salvarVistoria() {
		val respostas = HashMap<String, Any>()

		for (campo in fieldDefinitions) {
			val view = formViews[campo.name]
			if (view != null) {
				when (campo.type) {
					"textarea", "text" -> {
						val text = (view as EditText).text.toString()
						respostas[campo.name] = text
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
						// Lógica para recuperar valor do Spinner
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
		btnSalvar.text = "Enviando..."

		lifecycleScope.launch {
			try {
				val request = VistoriaRequest(
					demandaId = demandaAtual!!.id,
					respostas = respostas
				)

				Log.d("VistoriaActivity", "Enviando vistoria: $request")

				val response = NetworkClient.api.salvarVistoria(request)

				if (response.isSuccessful) {
					Toast.makeText(this@VistoriaActivity, "Vistoria salva com sucesso!", Toast.LENGTH_LONG).show()

					val resultIntent = android.content.Intent()
					resultIntent.putExtra("NOVO_STATUS", "concluido")
					resultIntent.putExtra("DEMANDA_ID", demandaAtual?.id)
					setResult(Activity.RESULT_OK, resultIntent)

					finish()
				} else {
					Log.e("VistoriaActivity", "Erro API ao salvar: ${response.code()}")
					Toast.makeText(this@VistoriaActivity, "Erro ao salvar: ${response.code()}", Toast.LENGTH_LONG).show()
					resetarBotoes()
				}
			} catch (e: Exception) {
				Log.e("VistoriaActivity", "Erro de Rede ao salvar", e)
				Toast.makeText(this@VistoriaActivity, "Erro de conexão. Tente novamente.", Toast.LENGTH_LONG).show()
				resetarBotoes()
			}
		}
	}

	// --- HELPERS UI ---

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