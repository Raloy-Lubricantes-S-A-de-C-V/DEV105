package com.example.myapplication.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_USERNAME = "username"
        private const val KEY_TOKEN = "access_token" // ✅ NUEVO: Para el JWT real
    }

    // Guardar solo el token (para el inicio de la app)
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).commit()
    }

    // Guardar sesión completa (al hacer login de usuario)
    fun saveSession(uid: Int, username: String) {
        sharedPreferences.edit().apply {
            putInt(KEY_UID, uid)
            putString(KEY_USERNAME, username)
            commit()
        }
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    fun getUid(): Int? {
        val uid = sharedPreferences.getInt(KEY_UID, -1)
        return if (uid == -1) null else uid
    }

    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}