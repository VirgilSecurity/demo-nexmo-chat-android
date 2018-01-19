package com.virgilsecurity.chat.nexmo.http

import com.google.gson.Gson
import com.virgilsecurity.chat.nexmo.http.model.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response

interface NexmoServerApi {

    @POST("users")
    @Headers("Content-Type: application/json")
    fun signup(@Body csr: CSR): Call<RegistrationResult>

    @GET("users")
    fun getUsers(@Header("Authorization") authHeader: String): Call<MutableList<NexmoUser>>

    @POST("conversations")
    @Headers("Content-Type: application/json")
    fun createConversation(@Header("Authorization") authHeader: String,
                           @Body request: CreateConversationRequest): Call<CreateConversationResponse>

    @PUT("conversations")
    @Headers("Content-Type: application/json")
    fun addUser(@Header("Authorization") authHeader: String,
                @Body request: AddUserRequest): Call<AddUserResponse>

    @GET("jwt")
    fun jwt(@Header("Authorization") authHeader: String): Call<JWT>

    companion object Factory {

        fun create(): NexmoServerApi {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val httpClient = OkHttpClient.Builder()
            httpClient.addInterceptor(logging);

            val retrofit = Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://nexmo.virgilsecurity.com/")
                    .client(httpClient.build())
                    .build()

            return retrofit.create(NexmoServerApi::class.java);
        }

        fun <T> parseError(response: Response<T>): APIError {
            try {
                return Gson().fromJson(response.errorBody()?.string(), APIError::class.java);
            } catch (e: Exception) {
                return APIError(-1, -1, "Unknown error")
            }
        }
    }
}