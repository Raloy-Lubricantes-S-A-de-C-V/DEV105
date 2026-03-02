package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.ui.home.HomeFragment
import com.example.myapplication.ui.login.LoginFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RetrofitClient.init(applicationContext)
        val sessionManager = SessionManager(this)

        if (savedInstanceState == null) {
            // ✅ Verificamos si el usuario ya inició sesión previamente
            if (sessionManager.getUid() != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, HomeFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, LoginFragment())
                    .commit()
            }
        }
    }
}