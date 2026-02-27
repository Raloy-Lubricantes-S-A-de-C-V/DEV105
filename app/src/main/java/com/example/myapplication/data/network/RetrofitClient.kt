package com.example.myapplication.data.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- MODELOS DE DATOS (Para evitar errores de importación) ---
data class AuthAppRequest(val username: String, val password: String)
data class AuthResponse(val token: String)
data class AccessRequest(val user: String)
data class AdminResponse(val access: Boolean)
data class SourceRequest(val source: String)
data class CorteResponse(val data: List<CorteData>)
data class CorteData(val id: Int, val user: String, val description: String, val state: Int)
data class CorteRequest(val id: Int?, val user: String, val i: String, val state: Int, val description: String)

// --- INTERFAZ API ---
interface ApiService {
    @POST("kioskorem/api/v1/autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): retrofit2.Response<AuthResponse>

    @POST("kioskorem/api/v1/rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): retrofit2.Response<AdminResponse>

    @POST("kioskorem/api/v1/corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): retrofit2.Response<CorteResponse>

    @POST("kioskorem/api/v1/corte/escribir")
    suspend fun escribirCorte(@Body request: CorteRequest): retrofit2.Response<Unit>
}

// --- CLIENTE RETROFIT ---
object RetrofitClient {
    private var retrofit: Retrofit? = null

    fun init(context: Context) {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(context)) // Asegúrate que AuthInterceptor ya use SessionManager
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("http://172.17.0.1:5000/") // Ajusta a tu IP de Docker/Servidor
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no inicializado")
}