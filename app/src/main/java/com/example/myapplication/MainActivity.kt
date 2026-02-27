package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.ui.home.HomeFragment
import com.example.myapplication.ui.login.LoginFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- CLASES DE DATOS PARA EVITAR ERROR DE IMPORTACIÓN ---
data class AuthAppRequest(val username: String, val password: String)
data class AuthResponse(val token: String)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Red con el Contexto
        RetrofitClient.init(applicationContext)

        // Ejecutar primer CURL: Autenticación de App
        obtenerTokenDeAplicacion()
    }

    private fun obtenerTokenDeAplicacion() {
        lifecycleScope.launch {
            try {
                // Credenciales exactas de tu CURL
                val request = AuthAppRequest(
                    username = "app-movile-001",
                    password = "Zsh4cvz4tvGyQa56P"
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val tokenRecibido = response.body()?.token ?: ""

                    // Guardar JWT en SessionManager
                    SessionManager(this@MainActivity).saveToken(tokenRecibido)
                    Log.d("AUTH", "✅ Token JWT guardado")

                    // Cargar LoginFragment después de obtener token
                    if (savedInstanceState == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, LoginFragment())
                            .commit()
                    }
                } else {
                    Log.e("AUTH", "❌ Error 401/500 en /autenticate")
                }
            } catch (e: Exception) {
                Log.e("AUTH", "❌ Fallo de red: ${e.message}")
            }
        }
    }
}