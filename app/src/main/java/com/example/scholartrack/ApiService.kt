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
    // IMPORTANT: Replace "YOUR_SERVER_IP" with the local IP address of your XAMPP server.
    // Example: "http://192.168.1.5/your_api_folder/"
    private const val BASE_URL = "http://YOUR_SERVER_IP/"

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