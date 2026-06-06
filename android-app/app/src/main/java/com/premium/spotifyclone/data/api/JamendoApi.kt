package com.premium.spotifyclone.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    
    @GET("tracks")
    suspend fun getTracks(
        @Query("client_id") clientId: String = "345e9844",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("fullcount") fullcount: Boolean = false,
        @Query("order") order: String = "popularity_total",
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null
    ): JamendoResponse

    companion object {
        private const val BASE_URL = "https://api.jamendo.com/v3.0/"

        fun create(): JamendoApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(JamendoApiService::class.java)
        }
    }
}
