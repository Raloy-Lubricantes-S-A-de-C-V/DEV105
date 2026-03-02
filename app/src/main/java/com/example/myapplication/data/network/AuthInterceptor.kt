package com.example.myapplication.data.network

import android.content.Context
import com.example.myapplication.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = SessionManager(context)
        val token = session.getToken() ?: ""

        // Usamos .newBuilder() para modificar la petición en vuelo
        val requestBuilder = chain.request().newBuilder()

        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // 🔥 BARRERA CONTRA EL ERROR (Ellipsis, Ellipsis) DE FLASK:
        // 1. Evitamos que Android comprima el JSON. Flask/Nginx HTTP/1.0 odia el GZIP entrante.
        requestBuilder.header("Accept-Encoding", "identity")

        // 2. Forzamos estrictamente el tipo de contenido para que Flask detecte que SÍ es un JSON.
        // Usamos .header() en lugar de .addHeader() para sobreescribir cualquier basura que ponga Android.
        requestBuilder.header("Accept", "application/json")
        requestBuilder.header("Content-Type", "application/json")

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        fun resetToken() {}
    }
}