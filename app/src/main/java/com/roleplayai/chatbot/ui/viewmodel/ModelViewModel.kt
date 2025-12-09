package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.download.ModelDownloader
import com.roleplayai.chatbot.data.model.DownloadProgress
import com.roleplayai.chatbot.data.model.ModelConfig
import com.roleplayai.chatbot.data.model.ModelState
import com.roleplayai.chatbot.data.preferences.PreferencesManager
import com.roleplayai.chatbot.data.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ModelViewModel(application: Application) : AndroidViewModel(application) {
    
    private val modelRepository = ModelRepository()
    private val modelDownloader = ModelDownloader(application)
    private val preferencesManager = PreferencesManager(application)
    
    private val _availableModels = MutableStateFlow<List<ModelConfig>>(emptyList())
    val availableModels: StateFlow<List<ModelConfig>> = _availableModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<ModelConfig?>(null)
    val selectedModel: StateFlow<ModelConfig?> = _selectedModel.asStateFlow()
    
    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()
    
    private val _availableStorage = MutableStateFlow(0L)
    val availableStorage: StateFlow<Long> = _availableStorage.asStateFlow()
    
    private val _availableRam = MutableStateFlow(0L)
    val availableRam: StateFlow<Long> = _availableRam.asStateFlow()
    
    init {
        loadModels()
        checkSystemResources()
        loadSavedModel()
    }
    
    private fun loadModels() {
        viewModelScope.launch {
            _availableModels.value = modelRepository.getAvailableModels()
        }
    }
    
    private fun loadSavedModel() {
        viewModelScope.launch {
            val savedModelId = preferencesManager.selectedModelId.first()
            
            if (savedModelId != null) {
                // Charger le modèle sauvegardé
                val model = modelRepository.getModelById(savedModelId)
                if (model != null) {
                    _selectedModel.value = model
                    
                    // Vérifier si téléchargé
                    if (modelDownloader.isModelDownloaded(model)) {
                        _modelState.value = ModelState.Downloaded
                    } else {
                        _modelState.value = ModelState.NotDownloaded
                    }
                } else {
                    // Modèle sauvegardé introuvable, sélectionner recommandé
                    selectRecommendedModel()
                }
            } else {
                // Première installation, sélectionner recommandé
                selectRecommendedModel()
            }
        }
    }
    
    private fun selectRecommendedModel() {
        viewModelScope.launch {
            val recommended = modelRepository.getRecommendedModel(_availableRam.value)
            _selectedModel.value = recommended
            
            // Check if already downloaded
            if (modelDownloader.isModelDownloaded(recommended)) {
                _modelState.value = ModelState.Downloaded
            } else {
                _modelState.value = ModelState.NotDownloaded
            }
        }
    }
    
    private fun checkSystemResources() {
        _availableStorage.value = modelDownloader.getAvailableStorageSpace()
        _availableRam.value = modelDownloader.getAvailableRamMB()
    }
    
    fun selectModel(model: ModelConfig) {
        viewModelScope.launch {
            _selectedModel.value = model
            
            // Sauvegarder la sélection
            preferencesManager.setSelectedModel(model.id, model.name)
            
            // Check if model is already downloaded
            if (modelDownloader.isModelDownloaded(model)) {
                _modelState.value = ModelState.Downloaded
                val path = modelDownloader.getModelPath(model)
                if (path != null) {
                    preferencesManager.setModelPath(path)
                }
            } else {
                _modelState.value = ModelState.NotDownloaded
            }
        }
    }
    
    fun downloadSelectedModel() {
        val model = _selectedModel.value ?: return
        
        viewModelScope.launch {
            try {
                _modelState.value = ModelState.Downloading(0f)
                
                modelDownloader.downloadModel(model).collect { progress ->
                    _downloadProgress.value = progress
                    _modelState.value = ModelState.Downloading(progress.percentage)
                    
                    if (progress.percentage >= 100f) {
                        _modelState.value = ModelState.Downloaded
                        preferencesManager.setModelDownloaded(true)
                        
                        val path = modelDownloader.getModelPath(model)
                        if (path != null) {
                            preferencesManager.setModelPath(path)
                        }
                    }
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Erreur de téléchargement")
            }
        }
    }
    
    fun deleteModel(model: ModelConfig) {
        viewModelScope.launch {
            try {
                val deleted = modelDownloader.deleteModel(model)
                if (deleted) {
                    _modelState.value = ModelState.NotDownloaded
                    preferencesManager.setModelDownloaded(false)
                    checkSystemResources()
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }
    
    suspend fun isFirstLaunch(): Boolean {
        return preferencesManager.isFirstLaunch.first()
    }
    
    suspend fun isModelDownloaded(): Boolean {
        return preferencesManager.isModelDownloaded.first()
    }
    
    suspend fun setFirstLaunchCompleted() {
        preferencesManager.setFirstLaunchCompleted()
    }
    
    fun loadModel() {
        val model = _selectedModel.value ?: return
        
        viewModelScope.launch {
            try {
                _modelState.value = ModelState.Loading(0f)
                
                // Simulate loading progress (will be replaced with actual llama.cpp loading)
                for (i in 0..100 step 10) {
                    _modelState.value = ModelState.Loading(i.toFloat())
                    kotlinx.coroutines.delay(100)
                }
                
                _modelState.value = ModelState.Loaded
            } catch (e: Exception) {
                _modelState.value = ModelState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }
    
    fun isModelReady(): Boolean {
        return _modelState.value == ModelState.Loaded
    }
    
    fun getModelPath(): String? {
        val model = _selectedModel.value ?: return null
        return modelDownloader.getModelPath(model)
    }
    
    fun isModelDownloaded(model: ModelConfig): Boolean {
        return modelDownloader.isModelDownloaded(model)
    }
    
    fun refreshModelState() {
        val model = _selectedModel.value ?: return
        viewModelScope.launch {
            if (modelDownloader.isModelDownloaded(model)) {
                _modelState.value = ModelState.Downloaded
                val path = modelDownloader.getModelPath(model)
                if (path != null) {
                    preferencesManager.setModelPath(path)
                }
            } else {
                _modelState.value = ModelState.NotDownloaded
            }
        }
    }
}
