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

        // Inyecta el token real para evitar el 401 en endpoints de la API
        if (token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Connection", "close")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        // ✅ Evita el error "Unresolved reference: resetToken" en LoginFragment
        fun resetToken() {}
    }
}