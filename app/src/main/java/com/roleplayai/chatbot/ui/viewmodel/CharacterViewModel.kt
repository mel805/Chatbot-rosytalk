package com.roleplayai.chatbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.CharacterCategory
import com.roleplayai.chatbot.data.model.CharacterTheme
import com.roleplayai.chatbot.data.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterViewModel : ViewModel() {
    
    private val repository = CharacterRepository()
    
    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()
    
    private val _filteredCharacters = MutableStateFlow<List<Character>>(emptyList())
    val filteredCharacters: StateFlow<List<Character>> = _filteredCharacters.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<CharacterCategory?>(null)
    val selectedCategory: StateFlow<CharacterCategory?> = _selectedCategory.asStateFlow()
    
    private val _selectedTheme = MutableStateFlow<CharacterTheme?>(null)
    val selectedTheme: StateFlow<CharacterTheme?> = _selectedTheme.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadCharacters()
    }
    
    private fun loadCharacters() {
        viewModelScope.launch {
            _characters.value = repository.getAllCharacters()
            _filteredCharacters.value = _characters.value
        }
    }
    
    fun filterByCategory(category: CharacterCategory?) {
        _selectedCategory.value = category
        _selectedTheme.value = null
        applyFilters()
    }
    
    fun filterByTheme(theme: CharacterTheme?) {
        _selectedTheme.value = theme
        _selectedCategory.value = null
        applyFilters()
    }
    
    fun searchCharacters(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    private fun applyFilters() {
        var filtered = _characters.value
        
        // Apply category filter
        _selectedCategory.value?.let { category ->
            filtered = filtered.filter { it.category == category }
        }
        
        // Apply theme filter
        _selectedTheme.value?.let { theme ->
            filtered = filtered.filter { theme in it.themes }
        }
        
        // Apply search
        if (_searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(_searchQuery.value, ignoreCase = true) ||
                it.description.contains(_searchQuery.value, ignoreCase = true)
            }
        }
        
        _filteredCharacters.value = filtered
    }
    
    fun clearFilters() {
        _selectedCategory.value = null
        _selectedTheme.value = null
        _searchQuery.value = ""
        _filteredCharacters.value = _characters.value
    }
    
    fun getCharacterById(id: String): Character? {
        return repository.getCharacterById(id)
    }
}
