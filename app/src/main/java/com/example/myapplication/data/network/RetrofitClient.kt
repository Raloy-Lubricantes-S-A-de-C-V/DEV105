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

    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null
        var tryCount = 0

        while (tryCount < 3) {
            try {
                response?.close() // Cierra cualquier intento previo
                response = chain.proceed(request)

                // 🔥 LA MAGIA CONTRA EL FANTASMA DE LOS 4 MINUTOS:
                // Obliga a Android a descargar todo el body AQUÍ ADENTRO.
                // Si el socket estaba muerto por inactividad, explotará en esta exacta línea,
                // el catch lo atrapará de inmediato y reintentará con un socket fresco.
                if (response.isSuccessful) {
                    response.body?.source()?.request(Long.MAX_VALUE)
                }

                return@Interceptor response
            } catch (e: Exception) {
                exception = e
                tryCount++
                Log.w("NETWORK_RETRY", "⚠️ Socket inactivo detectado. Reintentando $tryCount/3... (${e.message})")
                Thread.sleep(500) // Medio segundo para limpiar la basura de la red
            }
        }
        throw exception ?: IOException("Fallo de red persistente")
    }

    fun init(context: Context) {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 🔥 Asesina la memoria caché de sockets. Garantiza que toda petición nueva nazca limpia.
                .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                .retryOnConnectionFailure(true)
                .addInterceptor(retryInterceptor) // 1ro: El escudo de reintentos
                .addInterceptor(AuthInterceptor(context)) // 2do: Las cabeceras
                .build()

            retrofit = Retrofit.Builder()
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