package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- MODELOS DE DATOS (Sincronizados con tus CURLs) ---

data class AuthAppRequest(
    val username: String, // app-movile-001
    val password: String  // Zsh4cvz4tvGyQa56P
)

data class AuthResponse(
    val token: String // eyJ...
)

data class AccessRequest(val user: String)

data class AccessResponse(
    val access: Boolean,
    val msj: String,
    val status: Int
)

data class RolRequest(
    val user: String,
    val i: String,
    val sys: Int,
    val admin: Int,
    val normal: Int
)

data class RolSourceResponse(
    val count: Int,
    val data: List<RolData>,
    val error: Boolean,
    val msj: String,
    val status: Int
)

data class RolData(val user: String, val admin: Int, val normal: Int, val sys: Int)

data class CorteRequest(
    val id: Int? = null,
    val user: String,
    val i: String,
    val description: String? = null,
    val state: Int? = null,
    val reopen: Int = 0
)

data class CorteSourceResponse(
    val count: Int,
    val data: List<CorteData>,
    val error: Boolean,
    val msj: String,
    val status: Int
)

data class CorteData(val id: Int, val user: String, val description: String, val start_day: String?, val end_date: String?, val state: Int, val reopen: Int)

data class SourceRequest(val data: String = "{}")

// --- INTERFAZ API ---

interface ApiService {
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("rol/write")
    suspend fun escribirRol(@Body request: RolRequest): Response<Any>

    @POST("rol/source")
    suspend fun getRolesSource(@Body request: SourceRequest): Response<RolSourceResponse>

    @POST("rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSys(@Body request: AccessRequest): Response<AccessResponse>

    @POST("corte/write")
    suspend fun escribirCorte(@Body request: CorteRequest): Response<Any>

    @POST("corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): Response<CorteSourceResponse>
}