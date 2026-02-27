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
        setContentView(R.id.main_container)

        RetrofitClient.init(applicationContext)
        autenticarAppDefault()
    }

    private fun autenticarAppDefault() {
        lifecycleScope.launch {
            try {
                val request = AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    SessionManager(this@MainActivity).saveToken(token)
                    Log.d("AUTH", "✅ Token JWT obtenido")

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, LoginFragment())
                        .commit()
                }
            } catch (e: Exception) {
                Log.e("AUTH", "❌ Error conexión: ${e.message}")
            }
        }
    }
}