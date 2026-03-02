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

/**
 * Cliente global de Retrofit optimizado para comunicarse con servidores Flask/Werkzeug.
 * Implementa una piscina de conexiones y un interceptor de resiliencia para evitar
 * el error común de "unexpected end of stream".
 */
object RetrofitClient {
    private var retrofit: Retrofit? = null

    /**
     * Escudo Blindado: Atrapa caídas de socket causadas por cierres abruptos del servidor.
     * Si la red falla o el servidor cierra el tubo, reintenta hasta 3 veces automáticamente.
     */
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null

        for (tryCount in 1..3) {
            try {
                response?.close() // Limpia basuras de memoria de intentos previos
                response = chain.proceed(request)

                if (response.isSuccessful) {
                    // Fuerza la lectura total para detonar cualquier fallo de socket dentro del Try-Catch
                    response.peekBody(Long.MAX_VALUE)
                    return@Interceptor response
                } else {
                    // Si el servidor responde 400 o 500, se devuelve para no ciclar el servidor
                    return@Interceptor response
                }
            } catch (e: Exception) {
                exception = e
                Log.w("NETWORK_RETRY", "⚠️ Socket inactivo. Intento $tryCount/3... (${e.message})")
                if (tryCount == 3) throw e // Lanza el error definitivo al último intento
                Thread.sleep(800) // Respiro de red de 800ms para destrabar el backend
            }
        }
        return@Interceptor response ?: throw exception ?: IOException("Fallo de red persistente")
    }

    fun init(context: Context) {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(5, 2, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .addInterceptor(retryInterceptor)
                .addInterceptor(AuthInterceptor(context))
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