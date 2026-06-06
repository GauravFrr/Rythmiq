package com.premium.spotifyclone.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        com.premium.spotifyclone.data.api.BackendApiClient.authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DevApiBaseUrl.resolve())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }
}
