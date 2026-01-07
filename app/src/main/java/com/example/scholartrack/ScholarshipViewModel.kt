package com.example.scholartrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScholarshipViewModel : ViewModel() {

    private val _scholarships = MutableStateFlow<List<ScholarshipApp>>(emptyList())
    val scholarships = _scholarships.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    /**
     * Fetches scholarships once and starts a polling loop to keep data "real-time".
     */
    fun startPolling(userId: String) {
        // Cancel existing job if user changes or to prevent duplicates
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val result = ApiClient.apiService.getScholarships(userId)
                    // Only update if data actually changed to prevent unnecessary UI jumps
                    if (_scholarships.value != result) {
                        _scholarships.value = result
                    }
                    _errorMessage.value = null
                } catch (e: Exception) {
                    // Don't show error message every poll to avoid UI flickers, 
                    // only show if we have no data at all.
                    if (_scholarships.value.isEmpty()) {
                        _errorMessage.value = "Connection issues. Checking for updates..."
                    }
                }
                // Poll every 5 seconds
                delay(5000)
            }
        }
    }

    /**
     * Manual refresh if needed
     */
    fun refresh(userId: String) {
        viewModelScope.launch {
            try {
                _scholarships.value = ApiClient.apiService.getScholarships(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}