package com.example.myapplication.data.network

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
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

        val isWriteOperation = request.url.encodedPath.contains("/write")
        val maxRetries = if (isWriteOperation) 1 else 3

        var response: Response? = null
        var exception: Exception? = null

        Log.d(TAG, "🚀 Petición saliente: [${request.method}] ${request.url} (Max Retries: $maxRetries)")

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
                Thread.sleep((400 * tryCount).toLong())
            }
        }
        return@Interceptor response ?: throw exception ?: IOException("Fallo de red persistente")
    }

    fun init(context: Context) {
        if (retrofit == null) {
            Log.d(TAG, "⚙️ Inicializando Motor Retrofit/OkHttp (PROD SSL Mode + Nulls)...")

            // 🔥 CAMBIO CRÍTICO DE ARQUITECTURA:
            // Forzamos a Gson a serializar e incluir los valores "null".
            // Esto evita que Python asigne "Ellipsis" cuando Android omite un campo vacío.
            val gson = GsonBuilder()
                .serializeNulls()
                .create()

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(15, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .addInterceptor(retryInterceptor)
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(client)
                // 🔥 Inyectamos el Gson configurado a Retrofit
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            Log.i(TAG, "🟢 Motor de Red HTTPS Operativo y blindado contra Ellipsis.")
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no inicializado")
}