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
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
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
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import android.graphics.Bitmap
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.PhotoRenderer

@AndroidEntryPoint
class VistoriaActivity : AppCompatActivity() {

    @Inject lateinit var rendererFactory: FormRendererFactory
    @Inject lateinit var demandaRepository: DemandaRepository

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
    
    // GPS Ativo
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isMonitoringLocation = false

    private var isSaving = false
    private var lastDraft: Map<String, Any>? = null

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
        setupLocationClient()

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
            buscarNotificacoes()
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
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
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
                Log.d("VistoriaActivity", "Iniciando processamento da foto: $path")
                
                // 1. Prepara os dados (Data e GPS)
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                
                // 1.1 Tenta obter localização (Aguarda até 5s se estiver nula)
                var loc = location ?: capturedLocation
                if (loc == null) {
                    Log.d("VistoriaActivity", "GPS nulo, aguardando fix por até 5s...")
                    withTimeoutOrNull(5000) {
                        while (capturedLocation == null) {
                            delay(500)
                        }
                        loc = capturedLocation
                    }
                }

                // 1.2 Fallback final: Busca última localização conhecida do sistema
                if (loc == null) {
                    try {
                        loc = Tasks.await(fusedLocationClient.lastLocation)
                        Log.d("VistoriaActivity", "GPS Fallback (LastLocation): $loc")
                    } catch (e: Exception) {
                        Log.w("VistoriaActivity", "Falha ao obter LastLocation")
                    }
                }

                val gpsStr = if (loc != null) {
                    "Lat: ${String.format("%.5f", loc!!.latitude)} | Lon: ${String.format("%.5f", loc!!.longitude)}"
                } else {
                    "GPS: Indisponível"
                }

                // 2. Busca endereço com TIMEOUT aumentado (Geocoder pode travar)
                val addressStr = if (loc != null) {
                    val end = withTimeoutOrNull(8000) { getAddressString(loc!!) }
                    if (end != null) "\n$end" else ""
                } else ""

                val textoFinal = "$dateStr\n$gpsStr$addressStr"
                
                // 3. PROCESSAMENTO UNIFICADO (Resize + Watermark + WebP)
                // Usamos o nome original com extensão .webp
                val originalFile = File(path)
                val finalFileName = originalFile.name.substringBeforeLast(".") + "_proc.webp"
                val finalFile = File(originalFile.parent, finalFileName)
                
                Log.d("VistoriaActivity", "Chamando compressAndWatermark...")
                val sucesso = ImageWatermarkUtils.compressAndWatermark(
                    inputPath = path,
                    outputPath = finalFile.absolutePath,
                    watermarkText = textoFinal,
                    targetSize = 1280
                )

                if (sucesso) {
                    Log.i("VistoriaActivity", "Processamento concluído com sucesso: ${finalFile.absolutePath}")
                    // Remove o arquivo original (RAW)
                    if (originalFile.exists()) originalFile.delete()
                    
                    withContext(Dispatchers.Main) {
                        adicionarFotoNaTela(currentPhotoField ?: "", finalFile.absolutePath)
                    }
                } else {
                    throw Exception("Falha no processamento da imagem (Bitmap error)")
                }

            } catch (e: Exception) {
                Log.e("VistoriaActivity", "Erro ao processar foto: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VistoriaActivity, "Erro ao processar foto.", Toast.LENGTH_SHORT).show()
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
            if (viewContainer != null) {
                // Tenta atualizar como MultiPhoto ou Single Photo
                if (viewContainer.findViewWithTag<View>("photos_container") != null) {
                    MultiPhotoRenderer.addPhotoToView(viewContainer, path)
                } else if (viewContainer.findViewWithTag<View>("path_value") != null) {
                    PhotoRenderer.updatePhoto(viewContainer, path)
                }
            }
        }
    }

    private fun adicionarFotoViewEstatica(path: String) {
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250).apply { setMargins(0, 0, 16, 0) }
        }

        val imageView =
                ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
                    setOnClickListener {
                        val intent = Intent(this@VistoriaActivity, VisualizadorImagemActivity::class.java)
                        intent.putExtra("IMAGE_PATH", path)
                        startActivity(intent)
                    }
                }

        val deleteBtn = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(60, 60).apply {
                gravity = Gravity.TOP or Gravity.END
            }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.drawable.presence_offline)
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                fotosEstaticasFilePaths.remove(path)
                containerFotosEstaticas.removeView(frame)
                if (fotosEstaticasFilePaths.isEmpty()) {
                    txtSemFotos.visibility = View.VISIBLE
                }
            }
        }

        frame.addView(imageView)
        frame.addView(deleteBtn)
        containerFotosEstaticas.addView(frame)
        carregarImagemNoImageView(path, imageView)
    }

    /**
     * Método centralizado para carregar imagens de forma segura.
     * Suporta: Arquivos Locais (JPG/WebP), Fallback de extensão e URLs remotas.
     */
    fun carregarImagemNoImageView(path: String, imageView: ImageView) {
        if (path.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var finalPath = path
                val file = File(path)

                // 1. Caso seja arquivo local mas não exista (talvez mudou de .jpg para .webp)
                if (path.startsWith("/") && !file.exists()) {
                    val webpPath = path.substringBeforeLast(".") + ".webp"
                    if (File(webpPath).exists()) finalPath = webpPath
                }

                // 2. Carregamento
                val bitmap = if (finalPath.startsWith("http")) {
                    // Download simples de URL
                    val connection = java.net.URL(finalPath).openConnection()
                    connection.doInput = true
                    connection.connect()
                    val input = connection.getInputStream()
                    BitmapFactory.decodeStream(input)
                } else {
                    // Arquivo Local
                    val bmOptions = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(finalPath, bmOptions)
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }
            } catch (e: Exception) {
                Log.e("VistoriaActivity", "Erro ao carregar imagem: $path", e)
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                }
            }
        }
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
        // capturedLocation já estará atualizado pelo monitoramento contínuo
        
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

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    capturedLocation = location
                    Log.d("GPS", "Localização atualizada: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (isMonitoringLocation) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5 segundos
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isMonitoringLocation = true
            Log.i("GPS", "Monitoramento de GPS iniciado.")
        } catch (e: SecurityException) {
            Log.e("GPS", "Erro ao iniciar GPS: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (!isMonitoringLocation) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isMonitoringLocation = false
        Log.i("GPS", "Monitoramento de GPS parado.")
    }

    private fun obterLocalizacaoRapida(): Location? {
        // Agora mantemos este método apenas como fallback, mas o monitoramento ativo é a prioridade.
        return capturedLocation
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
                finalizarComSucesso("Vistoria salva! Sincronizando...", "Concluído")
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

    private fun buscarNotificacoes() {
        val demanda = demandaAtual ?: return

        lifecycleScope.launch {
            try {
                val notificacoes = demandaRepository.getNotificacoesByDemanda(demanda.id)
                notificacoes.forEach { notificacao ->
                    notificacao.fotos?.forEach { anexo ->
                        sectionAnexosExistentes.visibility = View.VISIBLE
                        val btn =
                                Button(
                                                this@VistoriaActivity,
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
                                                                    Uri.parse(anexo.url)
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
                                                    LinearLayout.LayoutParams(
                                                                    LinearLayout.LayoutParams
                                                                            .MATCH_PARENT,
                                                                    LinearLayout.LayoutParams
                                                                            .WRAP_CONTENT
                                                            )
                                                            .apply { setMargins(0, 0, 0, 8) }
                                        }
                        containerAnexosExistentes.addView(btn)
                    }
                }
            } catch (e: Exception) {
                Log.e("VistoriaActivity", "Erro ao buscar notificações", e)
            }
        }
    }
}
