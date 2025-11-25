package com.dexheimer.treeinspectorandroid.presentation.vistoria

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.core.util.ImageWatermarkUtils
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.usecase.SaveResult
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormRendererFactory
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.MultiPhotoRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VistoriaActivity : AppCompatActivity() {

	@Inject
	lateinit var rendererFactory: FormRendererFactory

	private val viewModel: VistoriaViewModel by viewModels()

	// --- UI Components ---
	private lateinit var dynamicFormContainer: LinearLayout
	private lateinit var btnSalvar: Button
	private lateinit var progressBar: ProgressBar
	private lateinit var toolbar: androidx.appcompat.widget.Toolbar
	private lateinit var txtTipoDemanda: TextView
	private lateinit var txtEndereco: TextView
	private lateinit var txtDescricao: TextView
	private lateinit var btnFixedCamera: Button
	private lateinit var btnFixedGallery: Button
	private lateinit var containerFotosEstaticas: LinearLayout
	private lateinit var txtSemFotos: TextView

	// --- Estado Local ---
	private var demandaAtual: Demanda? = null
	private val formViews = mutableMapOf<String, View>()
	private var fieldDefinitions = emptyList<FormField>()
	private val fotosEstaticasBase64 = mutableListOf<String>()

	// --- Controle de Imagens, GPS e Permissões ---
	private var currentPhotoField: String? = null
	private var currentPhotoUri: Uri? = null
	private var currentPhotoPath: String? = null
	private var pendingFieldForPermission: String? = null
	private var capturedLocation: Location? = null

	// 1. Permissões (Câmera e Localização)
	private val requestPermissionsLauncher = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
		val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

		if (cameraGranted) {
			// Tenta abrir a câmera mesmo se a localização for negada (vai sem GPS)
			pendingFieldForPermission?.let { abrirCameraSegura(it, locationGranted) }
		} else {
			Toast.makeText(this, "Permissão de câmera é obrigatória.", Toast.LENGTH_LONG).show()
		}
	}

	// 2. Launcher Câmera
	private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
		if (success && currentPhotoField != null && currentPhotoPath != null) {
			// Processa a foto para adicionar a marca d'água completa
			processarFotoComDadosCompletos(currentPhotoPath!!, capturedLocation)
		} else {
			Toast.makeText(this, "Foto cancelada.", Toast.LENGTH_SHORT).show()
		}
	}

	// 3. Launcher Galeria
	private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		if (uri != null && currentPhotoField != null) {
			val localFile = copiarUriParaArquivo(uri)
			if (localFile != null) {
				adicionarFotoNaTela(currentPhotoField!!, localFile.absolutePath)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_vistoria)
		setupUI()

		demandaAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
		}

		if (demandaAtual != null) {
			preencherCabecalho()
			demandaAtual?.tipoDemanda?.let { viewModel.buscarFormulario(it) }
		} else {
			finish()
			return
		}

		btnSalvar.setOnClickListener {
			demandaAtual?.let { d ->
				val respostas = coletarRespostas()
				viewModel.salvarVistoria(d, respostas)
			}
		}
		observarViewModel()
	}

	// ------------------------------------------------------------------------
	// LÓGICA DE MARCA D'ÁGUA COMPLETA (GPS + ENDEREÇO + DATA)
	// ------------------------------------------------------------------------

	private fun processarFotoComDadosCompletos(path: String, location: Location?) {
		showLoading(true)

		lifecycleScope.launch(Dispatchers.IO) {
			// 1. Data e Hora
			val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
			val dateStr = dateFormat.format(Date())

			// 2. Coordenadas GPS
			val gpsStr = if (location != null) {
				"Lat: ${String.format("%.6f", location.latitude)} | Lon: ${String.format("%.6f", location.longitude)}"
			} else {
				"GPS: Não capturado"
			}

			// 3. Endereço (Tenta buscar se tiver GPS)
			var addressStr = ""
			if (location != null) {
				val enderecoEncontrado = getAddressString(location)
				if (enderecoEncontrado != null) {
					addressStr = "\n$enderecoEncontrado"
				}
			}

			// 4. Monta o texto final (Data + GPS + Endereço)
			val finalText = "$dateStr\n$gpsStr$addressStr"

			// 5. Aplica na imagem
			val sucesso = ImageWatermarkUtils.waterMarkImage(path, finalText)

			withContext(Dispatchers.Main) {
				showLoading(false)
				if (!sucesso) {
					Toast.makeText(this@VistoriaActivity, "Aviso: Falha ao gravar marca d'água.", Toast.LENGTH_SHORT).show()
				}
				adicionarFotoNaTela(currentPhotoField!!, path)
			}
		}
	}

	/**
	 * Retorna apenas a string do endereço legível, ou null se falhar.
	 */
	private fun getAddressString(location: Location): String? {
		return try {
			val geocoder = Geocoder(this, Locale.getDefault())
			val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

			if (!addresses.isNullOrEmpty()) {
				val address = addresses[0]
				val rua = address.thoroughfare ?: ""
				val num = address.subThoroughfare ?: ""
				val bairro = address.subLocality ?: address.locality ?: ""

				if (rua.isNotEmpty()) "$rua, $num - $bairro" else null
			} else {
				null
			}
		} catch (e: Exception) {
			null // Falha de rede ou serviço
		}
	}

	// ------------------------------------------------------------------------
	// FLUXO DE CÂMERA E PERMISSÕES
	// ------------------------------------------------------------------------

	fun solicitarFoto(fieldName: String) {
		val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
		val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

		if (hasCamera) {
			// Se já tem câmera, abre (tenta pegar localização se tiver permissão)
			abrirCameraSegura(fieldName, hasLocation)
		} else {
			pendingFieldForPermission = fieldName
			// Pede tudo de uma vez
			requestPermissionsLauncher.launch(arrayOf(
				Manifest.permission.CAMERA,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION
			))
		}
	}

	fun solicitarGaleria(fieldName: String) {
		currentPhotoField = fieldName
		pickImageLauncher.launch("image/*")
	}

	private fun abrirCameraSegura(fieldName: String, hasLocationPermission: Boolean) {
		currentPhotoField = fieldName

		// Tenta capturar GPS AGORA, antes de abrir a câmera
		capturedLocation = if (hasLocationPermission) {
			obterLocalizacaoImediata()
		} else {
			null
		}

		val photoFile = criarArquivoImagem()
		if (photoFile != null) {
			currentPhotoPath = photoFile.absolutePath
			val photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
			currentPhotoUri = photoUri
			takePictureLauncher.launch(photoUri)
		}
	}

	private fun obterLocalizacaoImediata(): Location? {
		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		return try {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				// Tenta GPS preciso primeiro, depois Rede
				locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
					?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
			} else {
				null
			}
		} catch (e: Exception) {
			null
		}
	}

	// ------------------------------------------------------------------------
	// MÉTODOS PADRÃO DA ACTIVITY (UI, ViewModel, Helpers)
	// ------------------------------------------------------------------------

	private fun setupUI() {
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		dynamicFormContainer = findViewById(R.id.dynamicFormContainer)
		btnSalvar = findViewById(R.id.btnSalvarVistoria)
		progressBar = findViewById(R.id.progressBarForm)
		txtTipoDemanda = findViewById(R.id.txtTipoDemanda)
		txtEndereco = findViewById(R.id.txtEndereco)
		txtDescricao = findViewById(R.id.txtDescricao)
		btnFixedCamera = findViewById(R.id.btnFixedCamera)
		btnFixedGallery = findViewById(R.id.btnFixedGallery)
		containerFotosEstaticas = findViewById(R.id.containerFotosEstaticas)
		txtSemFotos = findViewById(R.id.txtSemFotos)

		btnFixedCamera.setOnClickListener { solicitarFoto("FIELD_FIXED_PHOTOS") }
		btnFixedGallery.setOnClickListener { solicitarGaleria("FIELD_FIXED_PHOTOS") }
	}

	private fun preencherCabecalho() {
		txtTipoDemanda.text = demandaAtual?.tipoDemanda ?: ""
		txtEndereco.text = "${demandaAtual?.logradouro}, ${demandaAtual?.numero}"
		txtDescricao.text = demandaAtual?.descricao
	}

	private fun adicionarFotoNaTela(fieldName: String, path: String) {
		if (fieldName == "FIELD_FIXED_PHOTOS") {
			val base64Img = fileToBase64(path)
			if (base64Img != null) {
				fotosEstaticasBase64.add(base64Img)
				txtSemFotos.visibility = View.GONE
				val imageView = ImageView(this).apply {
					layoutParams = LinearLayout.LayoutParams(250, 250).apply { setMargins(0, 0, 16, 0) }
					scaleType = ImageView.ScaleType.CENTER_CROP
					setImageBitmap(BitmapFactory.decodeFile(path))
					background = getDrawable(R.drawable.ic_launcher_background)
				}
				containerFotosEstaticas.addView(imageView)
			}
		} else {
			val viewContainer = formViews[fieldName]
			viewContainer?.let { MultiPhotoRenderer.addPhotoToView(it, path) }
		}
	}

	private fun observarViewModel() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { state ->
					if (!progressBar.isIndeterminate) showLoading(state.isLoading)
					val temConteudo = state.formFields.isNotEmpty() || fotosEstaticasBase64.isNotEmpty()
					btnSalvar.isEnabled = !state.isLoading && temConteudo
					if (state.formFields.isNotEmpty() && fieldDefinitions != state.formFields) {
						fieldDefinitions = state.formFields
						renderDynamicForm(state.formFields)
					}
					state.saveResult?.let { tratarResultadoSalvamento(it) }
				}
			}
		}
	}

	private fun renderDynamicForm(campos: List<FormField>) {
		dynamicFormContainer.removeAllViews()
		formViews.clear()
		for (campo in campos) {
			val renderer = rendererFactory.getRenderer(campo.type)
			if (renderer != null) {
				val view = renderer.render(this, campo, dynamicFormContainer)
				formViews[campo.name] = view
			}
		}
	}

	private fun coletarRespostas(): Map<String, Any> {
		val respostas = HashMap<String, Any>()
		for (campo in fieldDefinitions) {
			val view = formViews[campo.name]
			val renderer = rendererFactory.getRenderer(campo.type)
			if (view != null && renderer != null) {
				val resp = renderer.collectResponse(view, campo)
				if (resp != null) respostas[campo.name] = resp
			}
		}
		if (fotosEstaticasBase64.isNotEmpty()) respostas["fotos_evidencia"] = fotosEstaticasBase64
		return respostas
	}

	private fun criarArquivoImagem(): File? {
		val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
		val storageDir: File? = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
		return try { File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir) } catch (e: Exception) { null }
	}

	private fun copyingUriParaArquivo(uri: Uri): File? {
		return try {
			val inputStream = contentResolver.openInputStream(uri)
			val file = criarArquivoImagem()
			val outputStream = FileOutputStream(file)
			inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
			file
		} catch (e: Exception) { null }
	}
	private fun copiarUriParaArquivo(uri: Uri) = copyingUriParaArquivo(uri)

	private fun fileToBase64(filePath: String): String? {
		return try {
			val bytes = File(filePath).readBytes()
			val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
			"data:image/jpeg;base64,$base64"
		} catch (e: Exception) { null }
	}

	private fun showLoading(isLoading: Boolean) {
		progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
	}

	private fun tratarResultadoSalvamento(result: SaveResult) {
		when (result) {
			is SaveResult.SuccessOnline -> finalizarComSucesso("Vistoria enviada!", "concluido")
			is SaveResult.SuccessOffline -> finalizarComSucesso("Salvo offline.", "concluido_pendente")
			is SaveResult.Failure -> {
				Toast.makeText(this, "Erro: ${result.message}", Toast.LENGTH_LONG).show()
				btnSalvar.isEnabled = true
			}
		}
	}

	private fun finalizarComSucesso(msg: String, novoStatus: String) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
		val resultIntent = Intent().apply {
			putExtra("NOVO_STATUS", novoStatus)
			putExtra("DEMANDA_ID", demandaAtual?.id)
		}
		setResult(Activity.RESULT_OK, resultIntent)
		finish()
	}

	override fun onSupportNavigateUp(): Boolean { finish(); return true }
}