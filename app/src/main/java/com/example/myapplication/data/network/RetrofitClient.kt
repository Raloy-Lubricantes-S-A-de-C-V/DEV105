package com.example.myapplication.data.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- MODELOS DE DATOS (Sincronizados con tus CURLs) ---
data class AuthAppRequest(val username: String, val password: String)
data class AuthRequest(val user: String, val password: String)
data class AuthResponse(val token: String)
data class AccessRequest(val user: String)
data class AccessResponse(val access: Boolean, val msj: String, val status: Int)
data class SourceRequest(val data: String = "{}")
data class CorteRequest(val id: Int? = null, val user: String, val i: String, val description: String? = null, val state: Int? = null, val reopen: Int = 0)
data class CorteSourceResponse(val count: Int, val data: List<CorteData>, val error: Boolean, val msj: String, val status: Int)
data class CorteData(val id: Int, val user: String, val description: String, val start_day: String?, val end_date: String?, val state: Int, val reopen: Int)

// --- INTERFAZ API ---
interface ApiService {
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): retrofit2.Response<AuthResponse>

    @POST("rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): retrofit2.Response<AccessResponse>

    @POST("corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): retrofit2.Response<CorteSourceResponse>

    @POST("corte/write")
    suspend fun escribirCorte(@Body request: CorteRequest): retrofit2.Response<Any>
}

// --- CLIENTE RETROFIT ---
object RetrofitClient {
    private var retrofit: Retrofit? = null

    fun init(context: Context) {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                // Usamos el host definido en NetworkConfig
                .baseUrl(NetworkConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no inicializado")
}