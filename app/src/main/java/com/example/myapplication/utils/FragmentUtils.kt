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

            // Bucle que hace el "Refresh de Fragment" automático si hay error fatal
            while (intentosDatos < maxIntentosDatos && !datosCargados) {
                intentosDatos++

                if (intentosDatos == 1) {
                    tvCounter?.text = "Descargando datos y tablas..."
                } else {
                    // Notificamos al usuario que estamos auto-recargando el fragmento
                    tvCounter?.text = "Recargando fragmento... (Auto-Refresh $intentosDatos/$maxIntentosDatos)"
                    Log.w(logTag, "🔄 Ejecutando Auto-Refresh del fragmento...")
                }

                delay(500) // Respiro para desatascar Flask

                try {
                    coroutineScope { accionCarga() }
                    datosCargados = true // Éxito: marcamos bandera para salir del bucle
                } catch (e: Exception) {
                    Log.e(logTag, "❌ Error fatal al descargar datos: ${e.message}")

                    if (intentosDatos >= maxIntentosDatos) {
                        // Si falla los 3 refresh automáticos, avisamos y liberamos la pantalla
                        Toast.makeText(
                            requireContext(),
                            "Servidor saturado. Usa el botón de actualizar tabla.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Espera de 1.5s antes del siguiente Auto-Refresh
                        delay(1500)
                    }
                }
            }

            // Siempre quitamos el candado visual al terminar (haya éxito o no)
            overlay?.visibility = View.GONE

        } else {
            // Fase de Sesión fallida
            overlay?.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesión caducada o sin red", Toast.LENGTH_LONG).show()
            onFalloSesion()
        }
    }
}