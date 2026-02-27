package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// --- SEGURIDAD ---
data class AccessRequest(val user: String)
data class AccessResponse(val access: Boolean, val msj: String, val status: Int)

// --- ROLES ---
data class RolRequest(val user: String, val i: String, val sys: Int, val admin: Int, val normal: Int)
data class RolSourceResponse(val count: Int, val data: List<RolData>, val error: Boolean, val msj: String, val status: Int)
data class RolData(val user: String, val admin: Int, val normal: Int, val sys: Int)

// --- CORTES ---
data class CorteRequest(
    val id: Int? = null,
    val user: String,
    val i: String, // 'C', 'U', 'D'
    val description: String? = null,
    val state: Int? = null,
    val reopen: Int = 0
)

data class CorteResponse(val error: Boolean, val msj: String, val result: Any?, val status: Int)
data class CorteSourceResponse(val count: Int, val data: List<CorteData>, val error: Boolean, val msj: String, val status: Int)
data class CorteData(val id: Int, val user: String, val description: String, val start_day: String?, val end_date: String?, val state: Int, val reopen: Int)

data class SourceRequest(val comentario: String)

interface ApiService {
    @GET("verificar-sesion")
    suspend fun verificarSesion(): Response<Any>

    @POST("rol/write")
    suspend fun crearOActualizarRol(@Body request: RolRequest): Response<Any>

    @POST("rol/source")
    suspend fun getRolesSource(@Body request: SourceRequest): Response<RolSourceResponse>

    @POST("corte/write")
    suspend fun escribirCorte(@Body request: CorteRequest): Response<CorteResponse>

    @POST("corte/source")
    suspend fun getCortesSource(@Body request: SourceRequest): Response<CorteSourceResponse>

    @POST("rol/source/is_admin")
    suspend fun checkIsAdmin(@Body request: AccessRequest): Response<AccessResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSys(@Body request: AccessRequest): Response<AccessResponse>
}