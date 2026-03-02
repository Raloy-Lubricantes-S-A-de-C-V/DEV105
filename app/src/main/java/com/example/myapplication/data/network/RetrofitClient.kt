package com.example.myapplication.data.network

import android.content.Context
import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private const val TAG = "DEV105_NETWORK"

    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()

        // 🔥 CAMBIO CRÍTICO: Regla de Idempotencia.
        // NUNCA reintentar peticiones POST automáticamente porque duplican datos en la BD.
        val maxRetries = if (request.method == "POST") 1 else 3

        var response: Response? = null
        var exception: Exception? = null

        Log.d(TAG, "🚀 Petición saliente: [${request.method}] ${request.url}")

        for (tryCount in 1..maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful) {
                    response.peekBody(Long.MAX_VALUE)
                    Log.i(TAG, "✅ Respuesta exitosa: ${response.code} | Intento: $tryCount")
                    return@Interceptor response
                } else {
                    Log.e(TAG, "❌ Error HTTP: ${response.code} | URL: ${request.url}")
                    return@Interceptor response
                }
            } catch (e: Exception) {
                exception = e
                Log.w(TAG, "⚠️ Caída de Socket. Intento $tryCount/$maxRetries... Detalle: ${e.message}")

                if (tryCount == maxRetries) {
                    Log.e(TAG, "💥 Fallo de red definitivo tras agotar intentos.")
                    throw e
                }
                Thread.sleep((300 * tryCount).toLong())
            }
        }
        return@Interceptor response ?: throw exception ?: IOException("Fallo de red persistente")
    }

    fun init(context: Context) {
        if (retrofit == null) {
            Log.d(TAG, "⚙️ Inicializando Motor Retrofit/OkHttp...")
            val client = OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS) // Aumentamos a 45s para darle tiempo a Odoo de procesar el Base64
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(15, 2, TimeUnit.MINUTES))
                .retryOnConnectionFailure(false) // 🔥 Apagamos el reintento nativo agresivo
                .addInterceptor(retryInterceptor)
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            Log.i(TAG, "🟢 Motor de Red Operativo.")
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no inicializado")
}