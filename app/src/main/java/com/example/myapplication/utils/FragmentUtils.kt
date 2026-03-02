package com.example.myapplication.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.*

/**
 * Función de extensión inyectable para Fragmentos.
 * Despliega un overlay de bloqueo visual, verifica la sesión con el servidor (con 3 intentos)
 * y ejecuta funciones asíncronas, protegiendo contra corrupciones de datos (Auto-Refresh).
 *
 * @param tituloCarga Texto principal a mostrar en el Loader.
 * @param logTag Etiqueta para seguimiento en el Logcat.
 * @param accionCarga Bloque de código Coroutine a ejecutar si la sesión es válida.
 * @param onFalloSesion Acción a detonar si se agotan los intentos o no hay red.
 */
fun Fragment.ejecutarFlujoSeguro(
    tituloCarga: String,
    logTag: String,
    accionCarga: suspend CoroutineScope.() -> Unit,
    onFalloSesion: () -> Unit
) {
    val overlay = view?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = view?.findViewById<TextView>(R.id.tvLoadingTitle)
    val tvCounter = view?.findViewById<TextView>(R.id.tvRetryCounter)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = tituloCarga

    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
        var sesionValida = false

        // ==========================================
        // 1. FASE DE SESIÓN (3 Intentos de conexión)
        // ==========================================
        for (intento in 1..3) {
            Log.w(logTag, "Verificando sesión. Intento $intento/3")
            tvCounter?.text = "Validando red... (Intento $intento/3)"

            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.verificarSesion() }
                if (res.isSuccessful) {
                    sesionValida = true
                    Log.i(logTag, "✅ Sesión confirmada por servidor")
                    break
                }
            } catch (e: Exception) {
                Log.e(logTag, "❌ Error de red, intento $intento: ${e.message}")
            }
            if (intento < 3) delay(1500)
        }

        // ==========================================
        // 2. FASE DE DESCARGA CON AUTO-REFRESH
        // ==========================================
        if (sesionValida) {
            var datosCargados = false
            var intentosDatos = 0
            val maxIntentosDatos = 3

            while (intentosDatos < maxIntentosDatos && !datosCargados) {
                intentosDatos++

                if (intentosDatos == 1) {
                    tvCounter?.text = "Descargando datos y tablas..."
                } else {
                    tvCounter?.text = "Recargando fragmento... (Auto-Refresh $intentosDatos/$maxIntentosDatos)"
                    Log.w(logTag, "🔄 Ejecutando Auto-Refresh del fragmento...")
                }

                delay(500) // Respiro para desatascar el backend

                try {
                    coroutineScope { accionCarga() }
                    datosCargados = true // Se marca como exitoso y rompe el ciclo
                } catch (e: Exception) {
                    Log.e(logTag, "❌ Error fatal al descargar datos: ${e.message}")

                    if (intentosDatos >= maxIntentosDatos) {
                        Toast.makeText(
                            requireContext(),
                            "Servidor saturado. Usa el botón de actualizar tabla.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        delay(1500)
                    }
                }
            }
            overlay?.visibility = View.GONE
        } else {
            overlay?.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesión caducada o sin red", Toast.LENGTH_LONG).show()
            onFalloSesion()
        }
    }
}