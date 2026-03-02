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

        // ✅ REGLA DE ORO CONTRA LA INACTIVIDAD:
        // Le dice a Flask y a Android que corten la llamada al terminar.
        // Así no quedan conexiones fantasmas tras 4 minutos.
        requestBuilder.addHeader("Connection", "close")
        requestBuilder.addHeader("Accept-Encoding", "identity")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        fun resetToken() {}
    }
}