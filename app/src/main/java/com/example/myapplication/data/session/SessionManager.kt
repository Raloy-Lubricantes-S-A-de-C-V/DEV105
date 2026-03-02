package com.example.myapplication.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
        private const val KEY_TOKEN = "access_token"
        private const val TAG = "DEV105_SESSION" // 🔥 Etiqueta centralizada para Logcat
    }

    fun saveToken(token: String) {
        // 🔥 CAMBIO CRÍTICO: apply() evita el I/O Block en el Main Thread
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
        Log.d(TAG, "🔑 Token JWT guardado en memoria segura.")
    }

    fun saveSession(uid: Int, username: String) {
        sharedPreferences.edit().apply {
            putInt(KEY_UID, uid)
            putString(KEY_USERNAME, username)
            apply() // 🔥 CAMBIO CRÍTICO: apply()
        }
        Log.i(TAG, "✅ Sesión persistida asíncronamente para el usuario: $username | UID: $uid")
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_TOKEN, null)
        if (token == null) Log.w(TAG, "⚠️ Intento de lectura de Token: NULL")
        return token
    }

    fun getUid(): Int? {
        val uid = sharedPreferences.getInt(KEY_UID, -1)
        return if (uid == -1) null else uid
    }

    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
        Log.w(TAG, "🗑️ Sesión destruida completamente del EncryptedStorage.")
    }
}