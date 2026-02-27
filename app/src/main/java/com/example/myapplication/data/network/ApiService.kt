package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- MODELOS DE DATOS (Basados en tus comandos CURL) ---

/**
 * Módulo de Autenticación
 * Se usa para obtener el Token JWT inicial.
 */
data class AuthAppRequest(
    val username: String, // "app-movile-001"
    val password: String  // "Zsh4cvz4tvGyQa56P"
)

data class AuthResponse(
    val token: String // El token largo que empieza con eyJ...
)

/**
 * Módulo de Roles y Seguridad
 */
data class AccessRequest(
    val user: String // Correo del usuario a consultar
)

data class AccessResponse(
    val access: Boolean,
    val msj: String,
    val status: Int
)

data class RolRequest(
    val user: String,
    val i: String,     // 'C' (Create), 'U' (Update), 'D' (Delete)
    val sys: Int,      // 1 o 0
    val admin: Int,    // 1 o 0
    val normal: Int    // 1 o 0
)

data class RolSourceResponse(
    val count: Int,
    val data: List<RolData>,
    val error: Boolean,
    val msj: String,
    val status: Int
)

data class RolData(
    val user: String,
    val admin: Int,
    val normal: Int,
    val sys: Int
)

/**
 * Módulo de Cortes
 */
data class CorteRequest(
    val id: Int? = null, // Necesario para 'U' y 'D'
    val user: String,
    val i: String,       // 'C', 'U', 'D'
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

data class CorteData(
    val id: Int,
    val user: String,
    val description: String,
    val start_day: String?,
    val end_date: String?,
    val state: Int,
    val reopen: Int
)

data class SourceRequest(
    val data: String = "{}" // Se envía un JSON vacío para obtener todo
)

// --- INTERFAZ API ---

interface ApiService {

    /**
     * 1. Autenticación
     * POST /kioskorem/api/v1/autenticate
     */
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    /**
     * 2. Roles (CRUD y Consulta)
     * POST /kioskorem/api/v1/rol/write
     * POST /kioskorem/api/v1/rol/source
     */
    @POST("rol/write")
    suspend fun escribirRol(@Body request: RolRequest): Response<Any>

    @POST("rol/source")
    suspend fun getRolesSource(@Body request: SourceRequest): Response<RolSourceResponse>

    /**
     * 3. Verificación de Permisos Específicos
     * POST /kioskorem/api/v1/rol/source/is_admin
     */
    @POST("rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSys(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source/is_normal")
    suspend fun checkIsNormal(@Body request: AccessRequest): Response<AccessResponse>

    /**
     * 4. Cortes (CRUD y Consulta)
     * POST /kioskorem/api/v1/corte/write
     * POST /kioskorem/api/v1/corte/source
     */
    @POST("corte/write")
    suspend fun escribirCorte(@Body request: CorteRequest): Response<Any>

    @POST("corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): Response<CorteSourceResponse>
}