package com.example.myapplication.data.odoo

import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

object OdooAuthService {

    private const val URL_ODOO = "http://odoo.raloy.com.mx:8069"
    private const val DB = "raloy_productivo"

    fun authenticate(username: String, password: String): Int? {
        return try {
            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("$URL_ODOO/xmlrpc/2/common")
                isEnabledForExtensions = true
            }

            val client = XmlRpcClient().apply {
                setConfig(config)
            }

            val result = client.execute(
                "authenticate",
                listOf(DB, username, password, emptyMap<String, Any>())
            )

            (result as? Int)?.takeIf { it > 0 }

        } catch (e: Exception) {
            null
        }
    }
}