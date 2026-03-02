package com.example.myapplication.data

import android.util.Log // ✅ Import para los logs
import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.network.* // ✅ Importa AuthAppRequest y ApiService
import com.example.myapplication.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepository(
    private val dataSource: LoginDataSource,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        // PASO 1: Odoo
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            return try {
                Log.d("ODOO_AUTH", "Login exitoso en Odoo. Obteniendo Token REST...")

                // PASO 2: API REST Raloy
                val authRequest = AuthAppRequest(username = "app-movile-001", password = "Zsh4cvz4tvGyQa56P")

                val apiResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(authRequest)
                }

                if (apiResponse.isSuccessful && apiResponse.body()?.data != null) {
                    val tokenKey = apiResponse.body()!!.data!!.key

                    sessionManager.saveSession(result.data.userId.toInt(), username)
                    sessionManager.saveToken(tokenKey)

                    Log.d("ODOO_AUTH", "Token REST guardado: ${tokenKey.take(10)}...")
                    result
                } else {
                    Log.e("ODOO_AUTH", "Error 400 o Body nulo en REST Raloy")
                    Result.Error(Exception("Error en autenticación REST"))
                }
            } catch (e: Exception) {
                Log.e("ODOO_AUTH", "Excepción en LoginRepository: ${e.message}")
                Result.Error(e)
            }
        }
        return result
    }
}