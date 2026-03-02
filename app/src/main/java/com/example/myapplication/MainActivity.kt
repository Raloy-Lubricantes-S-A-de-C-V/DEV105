package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.ui.login.LoginFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar cliente de red
        RetrofitClient.init(applicationContext)

        // Mostrar login directamente, la autenticación REST se hará tras Odoo en el repositorio
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, LoginFragment())
                .commitNow()
        }
    }
}