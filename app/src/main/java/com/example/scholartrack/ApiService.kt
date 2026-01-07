package com.example.scholartrack

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("signup.php")
    suspend fun signup(@Body request: SignUpRequest): AuthResponse
}

object ApiClient {
    private const val BASE_URL = "http://192.168.254.108/scholarapi/"
    private const val API_KEY = "sk_scholartrack_8f92a3b4c5d6e7f8" // Must match PHP

    val apiService: ApiService by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // 1. Create client that adds the key to every request
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-KEY", API_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()

        // 2. Build Retrofit with the custom client
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // <--- Add this line
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(ApiService::class.java)
    }
}