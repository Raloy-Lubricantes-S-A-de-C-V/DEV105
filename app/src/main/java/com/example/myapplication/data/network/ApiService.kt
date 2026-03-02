package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

// Modelos de Autenticación
data class AuthAppRequest(val username: String, val password: String)
data class AuthResponse(val data: AuthData?, val status: Int)
data class AuthData(val error: Boolean, val key: String, val msj: String)

// Modelos de Permisos
data class AccessRequest(val user: String)
data class AccessResponse(val access: Boolean, val msj: String, val status: Int)

// Modelos de Roles
data class RolRequest(val user: String, val i: String, val sys: Int = 0, val admin: Int = 0, val normal: Int = 0)
data class RolData(val user: String, val admin: Int, val normal: Int, val sys: Int)
data class RolSourceResponse(val count: Int, val data: List<RolData>, val error: Boolean, val msj: String, val status: Int)

// Modelos de Cortes
data class CorteRequest(val id: Int? = null, val user: String, val i: String, val description: String? = null, val state: Int? = null, val reopen: Int = 0)
data class CorteData(val id: Int, val user: String, val description: String, val start_day: String?, val end_date: String?, val state: Int, val reopen: Int)
data class CorteSourceResponse(val count: Int, val data: List<CorteData>, val error: Boolean, val msj: String, val status: Int)

data class SourceRequest(val data: String = "{}")

// Modelos de CheckIn
data class CheckInRequest(
    val id: Int? = null,
    val user: String,
    val i: String,
    val albaran: String? = null,
    val corte_id: Int? = null,
    val confirm: Int? = null
)

data class CheckInData(
    val id: Int,
    val user: String?,
    val albaran: String?,
    val confirm: Int?,
    val corte_id: Int?,
    val signature_date: String?
)

data class CheckInSourceResponse(val count: Int, val data: List<CheckInData>, val error: Boolean, val msj: String, val status: Int)

data class CheckInWriteResponse(
    val error: Boolean?,
    val msj: String?,
    val albaran: String?,
    // 🔥 CAMBIO CRÍTICO: 'Any?' en lugar de 'Int?'.
    // Esto evita que Retrofit se rompa si Flask envía un Float (1.0), String ("1") o un Array ([1]).
    val result: Any?,
    val status: Int?
)

interface ApiService {
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @GET("verificar-sesion")
    suspend fun verificarSesion(): Response<AccessResponse>

    @POST("rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSys(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source")
    suspend fun getRolesSource(@Body request: SourceRequest): Response<RolSourceResponse>

    @POST("rol/write")
    suspend fun escribirRol(@Body request: RolRequest): Response<Any>

    @POST("corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): Response<CorteSourceResponse>

    @POST("corte/write")
    suspend fun escribirCorte(@Body request: CorteRequest): Response<Any>

    @POST("checkin/source")
    suspend fun getCheckInSource(@Body request: SourceRequest): Response<CheckInSourceResponse>

    @POST("checkin/write")
    suspend fun escribirCheckIn(@Body request: CheckInRequest): Response<CheckInWriteResponse>
}