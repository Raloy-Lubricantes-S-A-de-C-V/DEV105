package com.example.myapplication.data

import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.odoo.OdooAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LoginDataSource {

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        return try {
            val uid = withContext(Dispatchers.IO) {
                OdooAuthService.authenticate(username, password)
            }

            if (uid != null) {
                Result.Success(
                    LoggedInUser(uid.toString(), username)
                )
            } else {
                Result.Error(IOException("Credenciales inválidas"))
            }

        } catch (e: Exception) {
            Result.Error(IOException("Error de conexión"))
        }
    }

    fun logout() {}
}