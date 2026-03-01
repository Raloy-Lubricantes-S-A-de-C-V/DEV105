package com.example.myapplication.data

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
        // Paso 1: Autenticación en Odoo
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            return try {
                // Paso 2: Obtener JWT para la API REST
                val authRequest = AuthAppRequest(user = username, password = password)
                val apiResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(authRequest)
                }

                if (apiResponse.isSuccessful && apiResponse.body() != null) {
                    val uid = result.data.userId.toInt()
                    sessionManager.saveSession(uid, username)
                    sessionManager.saveToken(apiResponse.body()!!.token)
                    result
                } else {
                    Result.Error(Exception("Fallo al obtener token de servicios Raloy"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
        return result
    }
}