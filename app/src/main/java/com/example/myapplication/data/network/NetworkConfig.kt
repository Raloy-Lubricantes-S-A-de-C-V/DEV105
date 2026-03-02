package com.example.myapplication.data.network

import android.util.Log

object NetworkConfig {
    /**
     * IP ESPECIAL PARA EMULADOR: 10.0.2.2 apunta al localhost de la PC.
     * Si usas dispositivo físico, cambia a la IP de tu red (ej. 192.168.1.65).
     */
    //private const val DEFAULT_URL = "http://10.0.2.2:5000/kioskorem/api/v1/"
    private const val DEFAULT_URL = "https://apir.raloy.com.mx/kioskorem/api/v1/"

    var BASE_URL: String = DEFAULT_URL
        set(value) {
            // Aseguramos que la URL siempre termine en "/" para evitar errores en Retrofit
            field = if (value.endsWith("/")) value else "$value/"
            Log.d("NETWORK_CONFIG", "🚀 URL Base actualizada a: $field")
        }

    /**
     * Función de pre-validación rápida:
     * Verifica si la URL es válida antes de que el Interceptor intente conectar.
     */
    fun isUrlValid(): Boolean {
        return BASE_URL.startsWith("http://") || BASE_URL.startsWith("https://")
    }
}