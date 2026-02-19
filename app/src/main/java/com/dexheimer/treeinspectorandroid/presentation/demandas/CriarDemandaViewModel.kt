package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.data.remote.CreateDemandaRequest
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CriarDemandaViewModel @Inject constructor(private val repository: DemandaRepository) :
        ViewModel() {

    private val _uiState = MutableLiveData<CriarDemandaUiState>(CriarDemandaUiState.Idle)
    val uiState: LiveData<CriarDemandaUiState> = _uiState

    private val _addressInfo = MutableLiveData<AddressInfo>()
    val addressInfo: LiveData<AddressInfo> = _addressInfo

    private val _fotos = MutableLiveData<List<String>>(emptyList())
    val fotos: LiveData<List<String>> = _fotos

    private val _tiposDemanda =
            MutableLiveData<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>>()
    val tiposDemanda: LiveData<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>> =
            _tiposDemanda

    init {
        fetchTiposDemanda()
    }

    private fun fetchTiposDemanda() {
        viewModelScope.launch {
            repository
                    .getTiposDemanda()
                    .onSuccess { tipos -> _tiposDemanda.value = tipos }
                    .onFailure {
                        // Log or handle error silently, maybe retry?
                        // For now, we rely on the user seeing an empty list or default types if we
                        // had them.
                    }
        }
    }

    fun addFoto(path: String) {
        val current = _fotos.value ?: emptyList()
        _fotos.value = current + path
    }

    fun removeFoto(path: String) {
        val current = _fotos.value ?: emptyList()
        _fotos.value = current - path
    }

    fun setLocation(context: Context, location: Location) {
        _uiState.value = CriarDemandaUiState.Loading("Obtendo endereço...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val cityCandidate =
                                address.locality
                                        ?: address.subAdminArea ?: address.subLocality ?: ""

                        val stateName = address.adminArea ?: ""
                        val stateCode = mapStateNameToCode(stateName)

                        _addressInfo.value =
                                AddressInfo(
                                        cep = address.postalCode ?: "",
                                        logradouro = address.thoroughfare ?: "",
                                        bairro = address.subLocality ?: "",
                                        cidade = cityCandidate,
                                        uf = stateCode,
                                        lat = location.latitude,
                                        lng = location.longitude
                                )
                        _uiState.value = CriarDemandaUiState.Idle
                    } else {
                        _uiState.value =
                                CriarDemandaUiState.Error(
                                        "Não foi possível encontrar o endereço para esta localização."
                                )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value =
                            CriarDemandaUiState.Error(
                                    "Erro ao processar geolocalização: ${e.message}"
                            )
                }
            }
        }
    }

    private fun mapStateNameToCode(name: String): String {
        val states =
                mapOf(
                        "Acre" to "AC",
                        "Alagoas" to "AL",
                        "Amapá" to "AP",
                        "Amazonas" to "AM",
                        "Bahia" to "BA",
                        "Ceará" to "CE",
                        "Distrito Federal" to "DF",
                        "Espírito Santo" to "ES",
                        "Goiás" to "GO",
                        "Maranhão" to "MA",
                        "Mato Grosso" to "MT",
                        "Mato Grosso do Sul" to "MS",
                        "Minas Gerais" to "MG",
                        "Pará" to "PA",
                        "Paraíba" to "PB",
                        "Paraná" to "PR",
                        "Pernambuco" to "PE",
                        "Piauí" to "PI",
                        "Rio de Janeiro" to "RJ",
                        "Rio Grande do Norte" to "RN",
                        "Rio Grande do Sul" to "RS",
                        "Rondônia" to "RO",
                        "Roraima" to "RR",
                        "Santa Catarina" to "SC",
                        "São Paulo" to "SP",
                        "Sergipe" to "SE",
                        "Tocantins" to "TO"
                )
        return states[name] ?: name.substring(0, minOf(name.length, 2)).uppercase()
    }

    fun updateCepFromAddress(context: Context, logradouro: String, cidade: String) {
        if (logradouro.length < 5) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                val addresses = geocoder.getFromLocationName("$logradouro, $cidade", 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val stateName = address.adminArea ?: ""
                    val stateCode = mapStateNameToCode(stateName)

                    withContext(Dispatchers.Main) {
                        _addressInfo.value =
                                _addressInfo.value?.copy(
                                        cep = address.postalCode ?: _addressInfo.value?.cep ?: "",
                                        uf = stateCode
                                )
                                        ?: AddressInfo(
                                                cep = address.postalCode ?: "",
                                                logradouro = logradouro,
                                                cidade = cidade,
                                                uf = stateCode
                                        )
                    }
                }
            } catch (e: Exception) {
                // Silently ignore or log geocoding errors for background CEP update
            }
        }
    }

    fun salvarDemanda(request: CreateDemandaRequest) {
        _uiState.value = CriarDemandaUiState.Loading("Enviando demanda e fotos...")
        viewModelScope.launch {
            val fotosLocais = _fotos.value ?: emptyList()
            val urlsAnexos = mutableListOf<String>()
            var uploadError = false

            // 1. Upload photos one by one
            for (path in fotosLocais) {
                _uiState.value =
                        CriarDemandaUiState.Loading(
                                "Enviando foto: ${fotosLocais.indexOf(path) + 1}/${fotosLocais.size}"
                        )
                val uploadResult = repository.uploadImage(path)
                uploadResult.onSuccess { url -> urlsAnexos.add(url) }.onFailure {
                    uploadError = true
                    _uiState.value =
                            CriarDemandaUiState.Error("Falha ao enviar foto: ${it.message}")
                    return@launch
                }
            }

            // 2. Create demand with URLs
            if (!uploadError) {
                _uiState.value = CriarDemandaUiState.Loading("Registrando demanda...")
                val finalRequest = request.copy(anexos = urlsAnexos)
                val result = repository.criarDemanda(finalRequest)
                result.onSuccess { _uiState.value = CriarDemandaUiState.Success }.onFailure {
                    _uiState.value =
                            CriarDemandaUiState.Error("Falha ao criar demanda: ${it.message}")
                }
            }
        }
    }

    sealed class CriarDemandaUiState {
        object Idle : CriarDemandaUiState()
        data class Loading(val message: String) : CriarDemandaUiState()
        data class Error(val message: String) : CriarDemandaUiState()
        object Success : CriarDemandaUiState()
    }

    data class AddressInfo(
            val cep: String = "",
            val logradouro: String = "",
            val bairro: String = "",
            val cidade: String = "",
            val uf: String = "",
            val lat: Double? = null,
            val lng: Double? = null
    )
}
