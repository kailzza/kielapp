package com.example.scholartrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScholarshipViewModel : ViewModel() {

    private val _scholarships = MutableStateFlow<List<ScholarshipApp>>(emptyList())
    val scholarships = _scholarships.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun fetchScholarships(userId: String) {
        viewModelScope.launch {
            try {
                _scholarships.value = ApiClient.apiService.getScholarships(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load scholarships: ${e.message}"
            }
        }
    }
}