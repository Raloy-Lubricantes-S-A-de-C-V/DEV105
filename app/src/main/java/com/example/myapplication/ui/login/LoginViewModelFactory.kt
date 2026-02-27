package com.example.myapplication.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.*
import com.example.myapplication.data.session.SessionManager

class LoginViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val dataSource = LoginDataSource()
        val sessionManager = SessionManager(context.applicationContext)
        val repository = LoginRepository(dataSource, sessionManager)

        return LoginViewModel(repository) as T
    }
}