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
        requestBuilder.addHeader("Content-Type", "application/json")
        return chain.proceed(requestBuilder.build())
    }

    companion object {
        // ✅ SOLUCIÓN: Método para resetear el estado del token si es necesario
        fun resetToken() {
            // Lógica opcional para limpiar caché de tokens
        }
    }
}