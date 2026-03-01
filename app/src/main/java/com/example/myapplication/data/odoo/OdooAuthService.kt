package com.example.myapplication.data.odoo

import android.util.Log
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

object OdooAuthService {
    private const val URL_ODOO = "http://odoo.raloy.com.mx:8069"
    private const val DB = "raloy_productivo"

    fun authenticate(username: String, password: String): Int? {
        Log.d("ODOO AUTH", "1. Iniciando lectura de credenciales: Usuario=$username")
        return try {
            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("$URL_ODOO/xmlrpc/2/common")
                isEnabledForExtensions = true
            }

            val client = XmlRpcClient().apply { setConfig(config) }

            Log.d("ODOO AUTH", "2. Conectando a $URL_ODOO con base $DB...")

            // Ejecución del procedimiento remoto
            val result = client.execute(
                "authenticate",
                listOf(DB, username, password, emptyMap<String, Any>())
            )

            val uid = (result as? Int)?.takeIf { it > 0 }

            if (uid != null) {
                Log.d("ODOO AUTH", "3. EXITOSO: Sesión iniciada. UID=$uid")
            } else {
                Log.e("ODOO AUTH", "3. ERROR: Credenciales incorrectas (UID nulo o 0)")
            }
            uid

        } catch (e: Exception) {
            Log.e("ODOO AUTH", "3. FALLO CRÍTICO: ${e.message}")
            null
        }
    }
}