package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.ui.login.LoginFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ✅ Referencia corregida

        RetrofitClient.init(applicationContext)
        autenticarAppDefault(savedInstanceState)
    }

    private fun autenticarAppDefault(savedInstanceState: Bundle?) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val request = AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()?.token ?: ""
                    if (token.isNotEmpty()) {
                        SessionManager(this@MainActivity).saveToken(token)
                        Log.d("ODOO AUTH", ">>> RED: Token JWT obtenido con éxito")
                    }

                    if (savedInstanceState == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, LoginFragment())
                            .commitNowAllowingStateLoss()
                    }
                }
            } catch (e: Exception) {
                Log.e("ODOO AUTH", ">>> RED ERROR: ${e.message}")
            }
        }
    }
}