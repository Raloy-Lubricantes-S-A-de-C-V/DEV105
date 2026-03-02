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
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            return try {
                val authRequest = AuthAppRequest(username = "app-movile-001", password = "Zsh4cvz4tvGyQa56P")
                val apiResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.autenticateApp(authRequest)
                }

                if (apiResponse.isSuccessful && apiResponse.body()?.data != null) {
                    val tokenKey = apiResponse.body()!!.data!!.key
                    // ✅ Persistencia: Guardamos UID y Token
                    sessionManager.saveSession(result.data.userId.toInt(), username)
                    sessionManager.saveToken(tokenKey)
                    result
                } else {
                    Result.Error(Exception("Error 400 en vinculación API REST"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
        return result
    }
}