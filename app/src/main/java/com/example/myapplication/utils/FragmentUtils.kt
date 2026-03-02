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
 * Función global para todos los fragmentos.
 * Maneja la validación de sesión (3 intentos), el overlay de carga y ejecuta las descargas.
 */
fun Fragment.ejecutarFlujoSeguro(
    tituloCarga: String,
    logTag: String,
    accionCarga: suspend CoroutineScope.() -> Unit,
    onFalloSesion: () -> Unit
) {
    // Captura los elementos genéricos del XML del fragment actual
    val overlay = view?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = view?.findViewById<TextView>(R.id.tvLoadingTitle)
    val tvCounter = view?.findViewById<TextView>(R.id.tvRetryCounter)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = tituloCarga

    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
        var sesionValida = false

        // ✅ CORRECCIÓN BUG: Solo validamos res.isSuccessful (HTTP 200).
        // Ya no dependemos de "access == true" porque el API test_token no lo envía.
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

        // ✅ FLUJO DEPENDIENTE DE LA SESIÓN
        if (sesionValida) {
            tvCounter?.text = "Descargando datos y tablas..."
            try {
                // Ejecutamos la carga específica del fragment que nos hayan pasado
                coroutineScope { accionCarga() }
            } catch (e: Exception) {
                Log.e(logTag, "❌ Error al descargar fuentes: ${e.message}")
                Toast.makeText(requireContext(), "Error al sincronizar", Toast.LENGTH_SHORT).show()
            } finally {
                // Oculta el loader sin importar si tuvo éxito o fallo la tabla
                overlay?.visibility = View.GONE
            }
        } else {
            overlay?.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesión caducada o sin red", Toast.LENGTH_LONG).show()
            onFalloSesion()
        }
    }
}