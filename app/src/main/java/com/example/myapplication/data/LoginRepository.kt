package com.example.myapplication.data

import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.session.SessionManager

class LoginRepository(
    private val dataSource: LoginDataSource,
    private val sessionManager: SessionManager
) {

    suspend fun login(username: String, password: String): Result<LoggedInUser> {

        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            val uid = result.data.userId.toInt()
            sessionManager.saveSession(uid, username)
        }

        return result
    }

    fun logout() {
        sessionManager.clearSession()
    }
}