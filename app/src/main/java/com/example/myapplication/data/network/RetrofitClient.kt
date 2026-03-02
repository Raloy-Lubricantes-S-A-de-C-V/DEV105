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

    // ✅ ESCUDO BLINDADO CON BUCLE FOR
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null

        for (tryCount in 1..3) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful) {
                    // Forzamos descarga para asegurar el socket
                    response.peekBody(Long.MAX_VALUE)
                    return@Interceptor response
                } else {
                    // ✅ CORRECCIÓN MAESTRA: Si es un error 500 o 400, NO reintenta.
                    // Lo devuelve de inmediato para que la UI lo maneje y no se cicle.
                    return@Interceptor response
                }
            } catch (e: Exception) {
                exception = e
                Log.w("NETWORK_RETRY", "⚠️ Socket inactivo. Intento $tryCount/3... (${e.message})")
                if (tryCount == 3) throw e // Lanza error al último intento
                Thread.sleep(800) // Respiro de red
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