package com.example.scholartrack

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// --- Retrofit API Service ---

interface ApiService {
    @GET("scholarships.php") 
    suspend fun getScholarships(@Query("user_id") userId: String): List<ScholarshipApp>

    @POST("login.php") // Endpoint for user login
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("signup.php") // Endpoint for user registration
    suspend fun signup(@Body request: SignUpRequest): AuthResponse
}

object ApiClient {
    // Using the correct IP address of your XAMPP server.
    private const val BASE_URL = "http://192.168.254.115/scholarapi/"

    val apiService: ApiService by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(ApiService::class.java)
    }
}