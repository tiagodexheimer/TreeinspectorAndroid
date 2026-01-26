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
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class VistoriaActivity : AppCompatActivity() {

    @Inject lateinit var rendererFactory: FormRendererFactory

    private val viewModel: VistoriaViewModel by viewModels()

    // --- UI Components ---
    private lateinit var dynamicFormContainer: LinearLayout
    private lateinit var btnSalvar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private lateinit var txtTipoDemanda: TextView
    private lateinit var txtEndereco: TextView
    private lateinit var txtDescricao: TextView
    private lateinit var btnFixedCamera: Button
    private lateinit var btnFixedGallery: Button
    private lateinit var containerFotosEstaticas: LinearLayout
    private lateinit var txtSemFotos: TextView
    private lateinit var sectionAnexosExistentes: LinearLayout
    private lateinit var containerAnexosExistentes: LinearLayout

    // --- Estado Local ---
    private var demandaAtual: Demanda? = null
    private val formViews = mutableMapOf<String, View>()
    private var fieldDefinitions = emptyList<FormField>()
    private val fotosEstaticasFilePaths = mutableListOf<String>()
    private var lastDraft: Map<String, Any>? = null

    private var isSaving = false

    // --- Controle de Imagens, GPS e Permissões ---
    private var currentPhotoField: String? = null
    private var currentPhotoPath: String? = null
    private var pendingFieldForPermission: String? = null
    private var capturedLocation: Location? = null

    // --- Constantes para salvar estado ---
    private val STATE_PHOTO_FIELD = "photo_field_key"
    private val STATE_PHOTO_PATH = "photo_path_key"
    private val STATE_FIXED_PATHS = "fixed_paths_key"
    private val STATE_LAST_LOCATION = "last_location_key"

    // 1. Permissões
    private val requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
                val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

                if (cameraGranted) {
                    pendingFieldForPermission?.let { abrirCameraSegura(it, locationGranted) }
                } else {
                    mostrarDialogoPermissaoNecessaria()
                }
            }

    // 2. Câmera
    private val takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && currentPhotoField != null && currentPhotoPath != null) {
                    processarFotoComDadosCompletos(currentPhotoPath!!, capturedLocation)
                } else {
                    currentPhotoPath = null
                    Toast.makeText(this, "Foto cancelada.", Toast.LENGTH_SHORT).show()
                }
            }

    // 3. Galeria
    private val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null && currentPhotoField != null) {
                    val localFile = copiarUriParaArquivo(uri)
                    if (localFile != null) {
                        processarFotoComDadosCompletos(localFile.absolutePath, null)
                    } else {
                        Toast.makeText(
                                        this,
                                        "Erro ao processar imagem da galeria.",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vistoria)

        setupUI()
        setupToolbar()

        savedInstanceState?.let { bundle ->
            currentPhotoField = bundle.getString(STATE_PHOTO_FIELD)
            currentPhotoPath = bundle.getString(STATE_PHOTO_PATH)
            bundle.getStringArrayList(STATE_FIXED_PATHS)?.let { savedPaths ->
                fotosEstaticasFilePaths.clear()
                fotosEstaticasFilePaths.addAll(savedPaths)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                capturedLocation = bundle.getParcelable(STATE_LAST_LOCATION, Location::class.java)
            } else {
                @Suppress("DEPRECATION")
                capturedLocation = bundle.getParcelable(STATE_LAST_LOCATION) as? Location
            }
        }

        demandaAtual =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra("DEMANDA_EXTRA", Demanda::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("DEMANDA_EXTRA") as? Demanda
                }

        val extraId = intent.getIntExtra("DEMANDA_ID", -1)

        if (demandaAtual != null) {
            preencherCabecalho()
            viewModel.carregarRascunho(demandaAtual!!.id)
            demandaAtual?.tipoDemanda?.let { viewModel.buscarFormulario(it) }
        } else if (extraId != -1) {
            Toast.makeText(this, "Erro: Dados da demanda não encontrados.", Toast.LENGTH_LONG)
                    .show()
            finish()
            return
        } else {
            finish()
            return
        }

        if (fotosEstaticasFilePaths.isNotEmpty()) {
            containerFotosEstaticas.removeAllViews()
            txtSemFotos.visibility = View.GONE
            fotosEstaticasFilePaths.forEach { path -> adicionarFotoViewEstatica(path) }
        }

        btnSalvar.setOnClickListener { salvarVistoria() }

        observarViewModel()
    }

    override fun onPause() {
        super.onPause()
        salvarRascunhoLocal()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PHOTO_FIELD, currentPhotoField)
        outState.putString(STATE_PHOTO_PATH, currentPhotoPath)
        outState.putStringArrayList(STATE_FIXED_PATHS, ArrayList(fotosEstaticasFilePaths))
        capturedLocation?.let { outState.putParcelable(STATE_LAST_LOCATION, it) }
    }

    private fun setupUI() {
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
        sectionAnexosExistentes = findViewById(R.id.sectionAnexosExistentes)
        containerAnexosExistentes = findViewById(R.id.containerAnexosExistentes)

        btnFixedCamera.setOnClickListener { solicitarFoto("FIELD_FIXED_PHOTOS") }
        btnFixedGallery.setOnClickListener { solicitarGaleria("FIELD_FIXED_PHOTOS") }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Vistoria ${demandaAtual?.protocolo ?: ""}"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun preencherCabecalho() {
        txtTipoDemanda.text = demandaAtual?.tipoDemanda ?: "Tipo não informado"
        txtEndereco.text = "${demandaAtual?.logradouro ?: ""}, ${demandaAtual?.numero ?: ""}"
        txtDescricao.text = demandaAtual?.descricao ?: "Sem descrição"
        preencherAnexosExistentes()
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!progressBar.isIndeterminate) {
                        showLoading(state.isLoading)
                    }

                    // Verify if form needs rendering (fields loaded OR draft loaded/changed)
                    if (state.formFields.isNotEmpty() &&
                                    (fieldDefinitions != state.formFields ||
                                            lastDraft != state.draft)
                    ) {
                        fieldDefinitions = state.formFields
                        lastDraft = state.draft

                        // Create a combined draft map (ViewModel draft + invalid/local changes if
                        // needed)
                        // For now, just use ViewModel draft
                        renderDynamicForm(state.formFields, state.draft)

                        // Restore static photos if present in draft
                        if (state.draft != null) {
                            val savedPhotos = state.draft["fotos_evidencia"] as? List<String>
                            if (savedPhotos != null) {
                                fotosEstaticasFilePaths.clear()
                                fotosEstaticasFilePaths.addAll(savedPhotos)
                                containerFotosEstaticas.removeAllViews()
                                txtSemFotos.visibility =
                                        if (fotosEstaticasFilePaths.isEmpty()) View.VISIBLE
                                        else View.GONE
                                fotosEstaticasFilePaths.forEach { adicionarFotoViewEstatica(it) }
                            }
                        }
                    }

                    val temFormulario =
                            state.formFields.isNotEmpty() || fotosEstaticasFilePaths.isNotEmpty()
                    btnSalvar.isEnabled = !state.isLoading && !isSaving && temFormulario

                    state.saveResult?.let { tratarResultadoSalvamento(it) }
                }
            }
        }
    }

    private fun renderDynamicForm(campos: List<FormField>, draft: Map<String, Any>? = null) {
        dynamicFormContainer.removeAllViews()
        formViews.clear()

        for (campo in campos) {
            try {
                val renderer = rendererFactory.getRenderer(campo.type)
                if (renderer != null) {
                    val initialValue = draft?.get(campo.name)
                    val view = renderer.render(this, campo, dynamicFormContainer, initialValue)
                    formViews[campo.name] = view
                    view.tag = campo.name
                }
            } catch (e: Exception) {
                Log.e("VistoriaActivity", "Erro ao renderizar campo ${campo.name}: ${e.message}", e)
            }
        }
    }

    private fun salvarVistoria() {
        if (isSaving || demandaAtual == null) return

        isSaving = true
        btnSalvar.isEnabled = false
        showLoading(true)

        val d = demandaAtual!!

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val respostas = coletarRespostas()
                withContext(Dispatchers.Main) { viewModel.salvarVistoria(d, respostas) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    showLoading(false)
                    Toast.makeText(
                                    this@VistoriaActivity,
                                    "Erro ao processar dados: ${e.message}",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
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
                if (resp != null) {
                    respostas[campo.name] = resp
                }
            }
        }

        if (fotosEstaticasFilePaths.isNotEmpty()) {
            respostas["fotos_evidencia"] = ArrayList(fotosEstaticasFilePaths)
        }

        return respostas
    }

    private fun processarFotoComDadosCompletos(path: String, location: Location?) {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dateStr =
                        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val gpsStr =
                        if (location != null) {
                            "Lat: ${String.format("%.5f", location.latitude)} | Lon: ${String.format("%.5f", location.longitude)}"
                        } else {
                            "GPS: Indisponível"
                        }

                var addressStr = ""
                if (location != null) {
                    val end = getAddressString(location)
                    if (end != null) addressStr = "\n$end"
                }

                val textoFinal = "$dateStr\n$gpsStr$addressStr"
                val sucesso = ImageWatermarkUtils.waterMarkImage(path, textoFinal)

                withContext(Dispatchers.Main) {
                    if (!sucesso) Log.w("VistoriaActivity", "Falha ao gravar marca d'água")
                    adicionarFotoNaTela(currentPhotoField ?: "", path)
                }
            } catch (e: Exception) {
                Log.e("VistoriaActivity", "Erro ao processar foto: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                                    this@VistoriaActivity,
                                    "Erro ao processar foto.",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            } finally {
                withContext(Dispatchers.Main) { showLoading(false) }
            }
        }
    }

    private fun getAddressString(location: Location): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val a = addresses[0]

                // Dados básicos
                val rua = a.thoroughfare ?: ""
                val numero = a.subThoroughfare ?: ""
                val bairro = a.subLocality ?: ""

                // [NOVO] Cidade e Estado
                val cidade = a.locality ?: a.subAdminArea ?: "" // Tenta cidade, senão microrregião
                val estado = a.adminArea ?: "" // Estado (UF)

                // Monta a string: "Rua X, 123 - Bairro\nCidade - UF"
                val linha1 = if (rua.isNotEmpty()) "$rua, $numero" else ""
                val linha2 = if (bairro.isNotEmpty()) " - $bairro" else ""
                val linha3 = if (cidade.isNotEmpty()) "\n$cidade - $estado" else ""

                "$linha1$linha2$linha3"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun adicionarFotoNaTela(fieldName: String, path: String) {
        if (fieldName.isEmpty()) return

        if (fieldName == "FIELD_FIXED_PHOTOS") {
            fotosEstaticasFilePaths.add(path)
            txtSemFotos.visibility = View.GONE
            adicionarFotoViewEstatica(path)
        } else {
            val viewContainer = formViews[fieldName]
            viewContainer?.let { MultiPhotoRenderer.addPhotoToView(it, path) }
        }
    }

    private fun adicionarFotoViewEstatica(path: String) {
        val imageView =
                ImageView(this).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(250, 250).apply { setMargins(0, 0, 16, 0) }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val bmOptions = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bitmap = BitmapFactory.decodeFile(path, bmOptions)
                    setImageBitmap(bitmap)
                    background =
                            ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
                }
        containerFotosEstaticas.addView(imageView)
    }

    fun solicitarFoto(fieldName: String) {
        val hasCamera =
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
        val hasLocation =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED

        if (hasCamera) {
            abrirCameraSegura(fieldName, hasLocation)
        } else {
            pendingFieldForPermission = fieldName
            requestPermissionsLauncher.launch(
                    arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
            )
        }
    }

    fun solicitarGaleria(fieldName: String) {
        currentPhotoField = fieldName
        pickImageLauncher.launch("image/*")
    }

    private fun abrirCameraSegura(fieldName: String, hasLocationPermission: Boolean) {
        currentPhotoField = fieldName
        if (hasLocationPermission) {
            val loc = obterLocalizacaoRapida()
            if (loc != null) capturedLocation = loc
        }

        val photoFile = criarArquivoImagem()
        if (photoFile != null) {
            currentPhotoPath = photoFile.absolutePath
            val photoUri =
                    FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider", // Autoridade correta
                            photoFile
                    )
            takePictureLauncher.launch(photoUri)
        } else {
            Toast.makeText(this, "Erro ao criar arquivo temporário.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obterLocalizacaoRapida(): Location? {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                val gps = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val net = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                gps ?: net
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun criarArquivoImagem(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            null
        }
    }

    private fun copiarUriParaArquivo(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = criarArquivoImagem()
            if (inputStream != null && file != null) {
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                file
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun mostrarDialogoPermissaoNecessaria() {
        AlertDialog.Builder(this)
                .setTitle("Permissões Necessárias")
                .setMessage(
                        "Para registrar a vistoria com evidências, precisamos de acesso à câmera e localização."
                )
                .setPositiveButton("Configurações") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSalvar.isEnabled = !isLoading && !isSaving
    }

    // --- CORREÇÃO DO ERRO ---
    private fun tratarResultadoSalvamento(result: SaveResult) {
        when (result) {
            is SaveResult.Success -> {
                // Mensagem de sucesso (Offline First)
                finalizarComSucesso("Vistoria salva! Sincronizando...", "concluido_pendente")
            }
            is SaveResult.Failure -> {
                showLoading(false)
                isSaving = false
                Toast.makeText(this, "Erro: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun finalizarComSucesso(msg: String, novoStatus: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        demandaAtual?.id?.let { viewModel.limparRascunho(it) } // Limpa rascunho
        val resultIntent =
                Intent().apply {
                    putExtra("NOVO_STATUS", novoStatus)
                    putExtra("DEMANDA_ID", demandaAtual?.id)
                }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun salvarRascunhoLocal() {
        if (demandaAtual == null || isSaving) return
        val respostas = coletarRespostas()
        if (respostas.isNotEmpty()) {
            viewModel.salvarRascunho(demandaAtual!!.id, respostas)
        }
    }

    private fun preencherAnexosExistentes() {
        val anexos = demandaAtual?.anexos
        if (anexos.isNullOrEmpty()) {
            sectionAnexosExistentes.visibility = View.GONE
            return
        }

        sectionAnexosExistentes.visibility = View.VISIBLE
        containerAnexosExistentes.removeAllViews()

        anexos.forEach { anexo ->
            val btn =
                    Button(
                                    this,
                                    null,
                                    com.google
                                            .android
                                            .material
                                            .R
                                            .style
                                            .Widget_MaterialComponents_Button_OutlinedButton
                            )
                            .apply {
                                text = "📎 ${anexo.nome}"
                                isAllCaps = false
                                setOnClickListener {
                                    try {
                                        val intent =
                                                Intent(Intent.ACTION_VIEW, Uri.parse(anexo.url))
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
                                        LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                )
                                                .apply { setMargins(0, 0, 0, 8) }
                            }
            containerAnexosExistentes.addView(btn)
        }
    }
}
