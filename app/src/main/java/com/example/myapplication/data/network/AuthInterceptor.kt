package com.example.myapplication.data.network

import android.content.Context
import com.example.myapplication.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = SessionManager(context)
        val token = session.getToken() ?: ""

        val requestBuilder = chain.request().newBuilder()
        if (token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // 🔥 CAMBIO CRÍTICO: "close" en lugar de "Keep-Alive".
        // Esto evita el error "unexpected end of stream" forzando al motor a
        // no reutilizar sockets viejos que el servidor Flask ya dio por cerrados.
        requestBuilder.addHeader("Connection", "close")
        requestBuilder.addHeader("Accept-Encoding", "identity")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        fun resetToken() {}
    }
}