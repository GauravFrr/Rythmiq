package com.premium.spotifyclone.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class SyncRequest(
    val name: String,
    val username: String? = null,
    val photoUrl: String? = null,
    val loginMethod: String
)
data class SyncResponse(val message: String, val error: String?)

data class UsernameCheckResponse(val available: Boolean)

interface BackendApi {
    @POST("api/auth/sync")
    suspend fun syncUser(@Body request: SyncRequest): retrofit2.Response<SyncResponse>

    @retrofit2.http.GET("api/auth/check-username")
    suspend fun checkUsername(@retrofit2.http.Query("username") username: String): retrofit2.Response<UsernameCheckResponse>
}

object BackendApiClient {
    var authToken: String? = null

    val instance: BackendApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(com.premium.spotifyclone.data.network.DevApiBaseUrl.resolve())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
