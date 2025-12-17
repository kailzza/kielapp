package com.example.scholartrack

import com.google.gson.annotations.SerializedName

// --- Request Data Classes ---

data class LoginRequest(
    val email: String,
    val pass: String // Assuming your PHP script uses 'pass'
)

data class SignUpRequest(
    val f_name: String, // Corresponds to your likely database columns
    val l_name: String,
    val email: String,
    val pass: String
)

// --- Response Data Classes ---

data class AuthResponse(
    val status: String,
    val message: String,
    @SerializedName("user_id") val userId: String? = null
)