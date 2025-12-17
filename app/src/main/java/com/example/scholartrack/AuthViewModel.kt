package com.example.scholartrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- Authentication State ---
enum class AuthState {
    IDLE,
    LOADING,
    AUTHENTICATED,
    ERROR
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Add this to hold the successful login response
    private val _loginResponse = MutableStateFlow<AuthResponse?>(null)
    val loginResponse = _loginResponse.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val request = LoginRequest(email, pass)
                val response = ApiClient.apiService.login(request)
                if (response.status == "success") {
                    _loginResponse.value = response // Store the response
                    _authState.value = AuthState.AUTHENTICATED
                } else {
                    _errorMessage.value = response.message
                    _authState.value = AuthState.ERROR
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login failed: ${e.message}"
                _authState.value = AuthState.ERROR
            }
        }
    }

    fun signup(fname: String, lname: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val request = SignUpRequest(fname, lname, email, pass)
                val response = ApiClient.apiService.signup(request)
                if (response.status == "success") {
                    // Automatically log the user in after successful sign-up
                    login(email, pass)
                } else {
                    _errorMessage.value = response.message
                    _authState.value = AuthState.ERROR
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign-up failed: ${e.message}"
                _authState.value = AuthState.ERROR
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
        _loginResponse.value = null // Reset on logout
    }
}