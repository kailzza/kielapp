package com.example.scholartrack

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// --- Retrofit API Service ---

interface ApiService {
    @GET("scholarships.php") // Example endpoint, replace with your actual endpoint
    suspend fun getScholarships(): List<ScholarshipApp>
}

object ApiClient {
    private const val BASE_URL = "https://your-api-base-url.com/"

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