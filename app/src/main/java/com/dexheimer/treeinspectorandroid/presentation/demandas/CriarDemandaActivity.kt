package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.CreateDemandaParams
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CriarDemandaActivity : AppCompatActivity() {

    private val viewModel: CriarDemandaViewModel by viewModels()

    private lateinit var toolbar: Toolbar
    private lateinit var editCep: TextInputEditText
    private lateinit var editLogradouro: TextInputEditText
    private lateinit var editNumero: TextInputEditText
    private lateinit var editBairro: TextInputEditText
    private lateinit var editCidade: TextInputEditText
    private lateinit var spinnerTipoDemanda: AutoCompleteTextView
    private lateinit var editDescricao: TextInputEditText
    private lateinit var btnGpsLocation: MaterialButton
    private lateinit var btnAddFoto: MaterialButton
    private lateinit var btnSalvarDemanda: MaterialButton
    private lateinit var rvFotos: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var fotoAdapter: FotoAdapter
    private var currentPhotoPath: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("camera_photo_path", currentPhotoPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Redundant but harmless if already restored in onCreate
        currentPhotoPath = savedInstanceState.getString("camera_photo_path")
    }

    private val requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                if (locationGranted) {
                    requestLocation()
                } else {
                    Toast.makeText(this, "Permissão de localização necessária.", Toast.LENGTH_SHORT)
                            .show()
                }
            }

    private val takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && currentPhotoPath != null) {
                    viewModel.addFoto(currentPhotoPath!!)
                }
            }

    private val requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) abrirCamera()
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_demanda)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        editCep = findViewById(R.id.editCep)
        editLogradouro = findViewById(R.id.editLogradouro)
        editNumero = findViewById(R.id.editNumero)
        editBairro = findViewById(R.id.editBairro)
        editCidade = findViewById(R.id.editCidade)
        spinnerTipoDemanda = findViewById(R.id.spinnerTipoDemanda)
        editDescricao = findViewById(R.id.editDescricao)
        btnGpsLocation = findViewById(R.id.btnGpsLocation)
        btnAddFoto = findViewById(R.id.btnAddFoto)
        btnSalvarDemanda = findViewById(R.id.btnSalvarDemanda)
        rvFotos = findViewById(R.id.rvFotos)
        progressBar = findViewById(R.id.progressBar)

        // Adapter inicial vazio, será populado pelo observer
        val adapter =
                ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        mutableListOf<String>()
                )
        spinnerTipoDemanda.setAdapter(adapter)

        fotoAdapter = FotoAdapter { path -> viewModel.removeFoto(path) }
        rvFotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvFotos.adapter = fotoAdapter

        btnGpsLocation.setOnClickListener { checkLocationPermission() }
        btnAddFoto.setOnClickListener { checkCameraPermission() }
        btnSalvarDemanda.setOnClickListener { salvar() }

        // Auto-update CEP logic
        editLogradouro.addTextChangedListener(
                object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.updateCepFromAddress(
                                this@CriarDemandaActivity,
                                s.toString(),
                                editCidade.text.toString()
                        )
                    }
                    override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                    ) {}
                    override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                    ) {}
                }
        )
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CriarDemandaViewModel.CriarDemandaUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSalvarDemanda.isEnabled = false
                }
                is CriarDemandaViewModel.CriarDemandaUiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnSalvarDemanda.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is CriarDemandaViewModel.CriarDemandaUiState.Success -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Demanda criada com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is CriarDemandaViewModel.CriarDemandaUiState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnSalvarDemanda.isEnabled = true
                }
            }
        }

        viewModel.addressInfo.observe(this) { info ->
            editCep.setText(info.cep)
            editLogradouro.setText(info.logradouro)
            editBairro.setText(info.bairro)
            editCidade.setText(info.cidade)
            // Note: coordinates and UF are hidden but will be used in salvar()
        }

        viewModel.fotos.observe(this) { fotos -> fotoAdapter.submitList(fotos) }

        viewModel.tiposDemanda.observe(this) { tipos ->
            val nomes = tipos.map { it.nome }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
            spinnerTipoDemanda.setAdapter(adapter)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation()
        } else {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun requestLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location =
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                viewModel.setLocation(this, location)
            } else {
                Toast.makeText(
                                this,
                                "Não foi possível obter a localização atual.",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Erro de permissão de localização.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamera() {
        val photoFile = criarArquivoImagem()
        if (photoFile != null) {
            val photoUri =
                    FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            currentPhotoPath = photoFile.absolutePath
            takePictureLauncher.launch(photoUri)
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

    private fun salvar() {
        // Campos de solicitante removidos da UI, usando valor padrão
        val nome = "Anônimo"
        val cep = editCep.text.toString()
        val numero = editNumero.text.toString()
        val tipo = spinnerTipoDemanda.text.toString()
        val descricao = editDescricao.text.toString()

        if (cep.isBlank() || numero.isBlank() || tipo.isBlank() || descricao.isBlank()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT)
                    .show()
            return
        }

        val addressInfo = viewModel.addressInfo.value
        val params =
                CreateDemandaParams(
                        nome_solicitante = nome,
                        telefone_solicitante = null,
                        email_solicitante = null,
                        cep = cep,
                        logradouro = editLogradouro.text.toString(),
                        numero = numero,
                        bairro = editBairro.text.toString(),
                        cidade = editCidade.text.toString(),
                        uf = addressInfo?.uf,
                        tipo_demanda = tipo,
                        descricao = descricao,
                        coordinates = addressInfo?.let { listOf(it.lat ?: 0.0, it.lng ?: 0.0) },
                        anexos = emptyList()
                )

        viewModel.salvarDemanda(params)
    }
}

class FotoAdapter(private val onRemove: (String) -> Unit) :
        RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {
    private var fotos: List<String> = emptyList()

    fun submitList(newList: List<String>) {
        fotos = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_foto_horizontal, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val path = fotos[position]
        holder.bind(path)
    }

    override fun getItemCount() = fotos.size

    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgFoto: ImageView = itemView.findViewById(R.id.imgFoto)
        private val btnRemove: com.google.android.material.button.MaterialButton =
                itemView.findViewById(R.id.btnRemove)

        fun bind(path: String) {
            val bitmap =
                    BitmapFactory.decodeFile(
                            path,
                            BitmapFactory.Options().apply { inSampleSize = 4 }
                    )
            imgFoto.setImageBitmap(bitmap)
            btnRemove.setOnClickListener { onRemove(path) }
        }
    }
}
