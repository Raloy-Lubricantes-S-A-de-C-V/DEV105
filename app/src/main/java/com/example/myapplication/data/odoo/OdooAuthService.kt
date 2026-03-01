package com.example.myapplication.data.odoo

import android.util.Log
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

object OdooAuthService {
    // URL Actualizada a producción.
    // Nota: Si usas https:// el puerto estándar es 443. Si es puerto 80, suele ser http://
    // He configurado la URL que indicaste.
    private const val URL_ODOO = "https://odooerp.raloy.com.mx"
    private const val DB = "raloy_productivo"

    fun authenticate(username: String, password: String): Int? {
        Log.d("ODOO AUTH", "1. Iniciando autenticación en Producción: $username")

        return try {
            val config = XmlRpcClientConfigImpl().apply {
                // Se concatena el endpoint de Odoo para autenticación común
                serverURL = URL("$URL_ODOO/xmlrpc/2/common")
                isEnabledForExtensions = true
                connectionTimeout = 15000
                replyTimeout = 15000
            }

            val client = XmlRpcClient().apply { setConfig(config) }

            // Argumentos desglosados para cumplir con la firma del método en Odoo
            val params = arrayOf(
                DB,
                username,
                password,
                emptyMap<String, Any>() // Contexto vacío requerido por Odoo
            )

            Log.d("ODOO AUTH", "2. Enviando petición a: ${config.serverURL}")

            // Ejecución
            val result = client.execute("authenticate", params)

            // Procesamiento de respuesta (Integer = Éxito, Boolean/False = Fallo)
            val uid = (result as? Int)?.takeIf { it > 0 }

            if (uid != null) {
                Log.d("ODOO AUTH", "3. LOGIN EXITOSO. UID: $uid")
            } else {
                Log.e("ODOO AUTH", "3. ERROR: Credenciales inválidas. Respuesta: $result")
            }
            uid

        } catch (e: Exception) {
            Log.e("ODOO AUTH", "3. ERROR DE CONEXIÓN: ${e.message}")
            // Si el error persiste, verificar si el puerto 80 requiere http:// en lugar de https://
            e.printStackTrace()
            null
        }
    }
}