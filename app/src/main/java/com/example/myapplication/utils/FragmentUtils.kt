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

        // 1. FASE DE SESIÓN (Los 3 Intentos)
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

        // 2. FASE DE DESCARGA DE DATOS/TABLAS
        if (sesionValida) {
            tvCounter?.text = "Descargando datos y tablas..."
            delay(400) // Micro-pausa para desatascar el servidor

            try {
                coroutineScope { accionCarga() }
            } catch (e: Exception) {
                Log.e(logTag, "❌ Error fatal al descargar datos: ${e.message}")

                // ✅ AQUÍ ESTÁ EL REQUERIMIENTO SOLICITADO
                // Este mensaje SOLO sale si la sesión pasó, pero los datos de la tabla fallaron
                Toast.makeText(
                    requireContext(),
                    "Recarga la tabla, probablemente no se cargaron los datos",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                // Siempre quitamos el candado visual para que el usuario pueda interactuar/recargar
                overlay?.visibility = View.GONE
            }
        } else {
            // Fase de Sesión fallida
            overlay?.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesión caducada o sin red", Toast.LENGTH_LONG).show()
            onFalloSesion()
        }
    }
}