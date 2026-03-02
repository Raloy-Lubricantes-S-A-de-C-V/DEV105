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

        // Como desactivamos la caché de red, las peticiones POST ahora son 100% estables.
        // Mantenemos 1 intento para escritura (Write) y 3 para lectura (Source).
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
                    response.peekBody(Long.MAX_VALUE) // Forzar lectura total para validar túnel
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
            Log.d(TAG, "⚙️ Inicializando Motor Retrofit/OkHttp (Strict Mode)...")
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // 🔥 CAMBIO ARQUITECTÓNICO CRÍTICO:
                // ConnectionPool(0, 1, MILLISECONDS) significa que el Pool retiene 0 conexiones.
                // Obliga a Android a crear un Socket TCP fresco en cada click.
                // Esto ERRADICA el error "unexpected end of stream" con servidores Flask.
                .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
                .retryOnConnectionFailure(true)
                .addInterceptor(retryInterceptor)
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            Log.i(TAG, "🟢 Motor de Red Operativo y blindado.")
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient no inicializado")
}