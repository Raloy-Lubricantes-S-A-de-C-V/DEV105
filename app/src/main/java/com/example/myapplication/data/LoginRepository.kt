package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.network.AuthAppRequest
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepository(
    private val dataSource: LoginDataSource,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        // PASO 1: Primero Odoo (UID 766)
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            return try {
                // PASO 2: Solo si Odoo es OK, pedimos el Token REST
                val authRequest = AuthAppRequest(username = "app-movile-001", password = "Zsh4cvz4tvGyQa56P")

                val apiResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(authRequest)
                }

                if (apiResponse.isSuccessful && apiResponse.body()?.data != null) {
                    val tokenKey = apiResponse.body()!!.data!!.key // Extrae 'key' de 'data'

                    sessionManager.saveSession(result.data.userId.toInt(), username)
                    sessionManager.saveToken(tokenKey)

                    Log.d("AUTH_FLOW", "UID Odoo y JWT Key REST vinculados correctamente")
                    result
                } else {
                    Log.e("AUTH_FLOW", "Error 400 en API REST Raloy")
                    Result.Error(Exception("Error al vincular con servicios REST (400)"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
        return result
    }
}