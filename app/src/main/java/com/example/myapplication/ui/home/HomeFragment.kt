package com.example.myapplication.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.checkin.CheckInAdapter
import com.example.myapplication.ui.corte.CreateCorteFragment
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.ui.rol.CreateRolFragment
import com.example.myapplication.utils.ejecutarFlujoSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var cortesAdapter: CortesHomeAdapter
    private lateinit var checkInAdapter: CheckInAdapter

    private var corteSeleccionado: CorteData? = null
    private var currentBitmap: Bitmap? = null
    private var currentCheckInId: Int? = null
    private var currentAlbaranName: String? = null

    companion object {
        private const val TAG = "DEV105_CHECKIN"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        binding.toolbar.title = "Panel Principal - Raloy"
        setupMenu()
        setupRecyclerViews()
        setupCheckInModule()

        binding.btnRefreshCortes.setOnClickListener { iniciarFlujoDeCarga() }
        binding.btnRefreshCheckInTable.setOnClickListener { cargarTablaCheckIn() }

        iniciarFlujoDeCarga()
    }

    private fun setupCheckInModule() {
        binding.btnCheckInRemision.setOnClickListener {
            binding.btnCheckInRemision.isEnabled = false
            abrirCamara()
        }

        binding.btnRegistrarDoc.setOnClickListener {
            binding.btnRegistrarDoc.isEnabled = false
            currentBitmap?.let { bmp -> registrarDocumentoCheckIn(bmp) }
        }

        binding.btnValidarCheckIn.setOnClickListener {
            binding.btnValidarCheckIn.isEnabled = false
            mostrarDialogoConfirmacion()
        }
    }

    private fun abrirCamara() {
        try {
            val imagenesPrueba = listOf(R.drawable.k1, R.drawable.k2)
            currentBitmap = BitmapFactory.decodeResource(resources, imagenesPrueba.random())
            binding.ivCheckInPhoto.setImageBitmap(currentBitmap)
            binding.ivCheckInPhoto.visibility = View.VISIBLE

            binding.btnCheckInRemision.visibility = View.GONE
            binding.btnRegistrarDoc.visibility = View.VISIBLE
            binding.btnRegistrarDoc.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Falta imagen de prueba", Toast.LENGTH_SHORT).show()
            binding.btnCheckInRemision.isEnabled = true
        }
    }

    private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        try {
            if (source.width <= maxLength && source.height <= maxLength) return source
            val aspectRatio = source.width.toDouble() / source.height.toDouble()
            return if (source.height >= source.width) {
                Bitmap.createScaledBitmap(source, (maxLength * aspectRatio).toInt(), maxLength, false)
            } else {
                Bitmap.createScaledBitmap(source, maxLength, (maxLength / aspectRatio).toInt(), false)
            }
        } catch (e: Exception) {
            return source
        }
    }

    private fun registrarDocumentoCheckIn(bitmap: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = "REGISTRANDO DOCUMENTO..."

            try {
                val base64Image = withContext(Dispatchers.Default) {
                    val resizedBmp = resizeBitmap(bitmap, 600)
                    val baos = ByteArrayOutputStream()
                    resizedBmp.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }

                val request = CheckInRequest(
                    user = sessionManager.getUsername() ?: "",
                    i = "C",
                    albaran = base64Image,
                    corte_id = corteSeleccionado?.id
                )

                val response = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCheckIn(request) }

                if (response.isSuccessful && response.body()?.error == false) {
                    // 🔥 EXTRACCIÓN AGRESIVA DEL ID:
                    // Convierte lo que sea que mande Flask en un Int puro para Kotlin.
                    val rawResult = response.body()?.result
                    currentCheckInId = when (rawResult) {
                        is Number -> rawResult.toInt()
                        is String -> rawResult.replace(Regex("[^0-9]"), "").toIntOrNull()
                        is List<*> -> (rawResult.firstOrNull() as? Number)?.toInt()
                        else -> null
                    }

                    currentAlbaranName = response.body()?.albaran

                    Log.i(TAG, "✅ Documento registrado. ID Parseado: $currentCheckInId | Raw: $rawResult")

                    if (currentCheckInId == null) {
                        Log.e(TAG, "❌ FATAL: El servidor retornó éxito pero no pudimos extraer el ID ($rawResult).")
                        Toast.makeText(requireContext(), "Error de sistema: ID perdido", Toast.LENGTH_LONG).show()
                        resetUIState()
                        return@launch
                    }

                    binding.btnRegistrarDoc.visibility = View.GONE
                    binding.btnValidarCheckIn.visibility = View.VISIBLE
                    binding.btnValidarCheckIn.isEnabled = true

                    delay(1000)
                    cargarTablaCheckIn()
                } else {
                    var msjError = response.body()?.msj
                    if (!response.isSuccessful && response.errorBody() != null) {
                        try {
                            val errorString = response.errorBody()!!.string()
                            val jsonError = JSONObject(errorString)
                            msjError = jsonError.optString("msj", "No se detectó un patrón válido")
                        } catch (e: Exception) {
                            msjError = "Error de servidor: HTTP ${response.code()}"
                        }
                    }
                    Log.w(TAG, "⚠️ Documento rechazado por OCR/Servidor: $msjError")
                    Toast.makeText(requireContext(), "Rechazado: ${msjError ?: "Fallo de validación"}", Toast.LENGTH_LONG).show()
                    resetUIState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción de red detectada: ${e.message}")
                Toast.makeText(requireContext(), "Fallo de red al registrar.", Toast.LENGTH_SHORT).show()
                resetUIState()
            } finally {
                binding.overlayLoading.visibility = View.GONE
            }
        }
    }

    private fun mostrarDialogoConfirmacion() {
        val nombre = currentAlbaranName ?: "Desconocido"
        AlertDialog.Builder(requireContext())
            .setTitle("Validar Check-In")
            .setMessage("¿Estás segur@ que confirmas el CheckIn del albarán $nombre?")
            .setPositiveButton("SÍ, CONFIRMAR") { _, _ -> procesarConfirmacion(true) }
            .setNegativeButton("NO, ELIMINAR") { _, _ -> procesarConfirmacion(false) }
            .setCancelable(false)
            .show()
    }

    private fun procesarConfirmacion(isConfirmed: Boolean) {
        val operacion = if (isConfirmed) "U" else "D"
        val confirmFlag = if (isConfirmed) 1 else null

        // 🔥 BARRERA DE SEGURIDAD ABSOLUTA
        if (currentCheckInId == null) {
            Log.e(TAG, "❌ Intento de confirmar/eliminar bloqueado. currentCheckInId es NULL.")
            Toast.makeText(requireContext(), "Error crítico: No hay ID seleccionado.", Toast.LENGTH_SHORT).show()
            binding.btnValidarCheckIn.isEnabled = true
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = if (isConfirmed) "CONFIRMANDO..." else "ELIMINANDO..."

            var operacionExitosa = false

            try {
                Log.d(TAG, "🚀 Petición de validación lista. Enviando -> ID: $currentCheckInId | operacion: $operacion | confirm: $confirmFlag")

                val request = CheckInRequest(
                    id = currentCheckInId,
                    user = sessionManager.getUsername() ?: "",
                    i = operacion,
                    confirm = confirmFlag
                )

                val response = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCheckIn(request) }

                if (response.isSuccessful) {
                    val msj = if (isConfirmed) "✅ CheckIn Confirmado" else "🗑️ CheckIn Eliminado"
                    Toast.makeText(requireContext(), msj, Toast.LENGTH_SHORT).show()
                    operacionExitosa = true
                    delay(800)
                } else {
                    Log.e(TAG, "❌ Fallo HTTP en validación. Code: ${response.code()}")
                    Toast.makeText(requireContext(), "Error al contactar base de datos", Toast.LENGTH_SHORT).show()
                    binding.btnValidarCheckIn.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error de red durante la confirmación: ${e.message}")
                Toast.makeText(requireContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show()
                binding.btnValidarCheckIn.isEnabled = true
            } finally {
                binding.overlayLoading.visibility = View.GONE

                if (operacionExitosa) {
                    if (isConfirmed) {
                        resetUIState()
                        cargarTablaCheckIn()
                    } else {
                        iniciarFlujoDeCarga()
                    }
                }
            }
        }
    }

    private fun resetUIState() {
        binding.ivCheckInPhoto.visibility = View.GONE
        binding.btnRegistrarDoc.visibility = View.GONE
        binding.btnValidarCheckIn.visibility = View.GONE
        binding.btnCheckInRemision.visibility = View.VISIBLE
        binding.btnCheckInRemision.isEnabled = corteSeleccionado != null
        currentBitmap = null
        currentCheckInId = null
        currentAlbaranName = null
    }

    private fun iniciarFlujoDeCarga() {
        ejecutarFlujoSeguro("CARGANDO PANEL PRINCIPAL", "HOME_FLOW", {
            val user = sessionManager.getUsername() ?: ""
            binding.welcomeText.text = "Bienvenido: $user"
            val adminRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
            delay(200)
            val cortesRes = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }

            if (adminRes.isSuccessful && adminRes.body()?.access == true) {
                binding.sectionCortesAbiertos.visibility = View.VISIBLE
                if (cortesRes.isSuccessful && cortesRes.body() != null) {
                    val cortesActivos = cortesRes.body()!!.data.filter { it.state == 1 }
                    cortesAdapter.updateData(cortesActivos)

                    corteSeleccionado = null
                    binding.tvCorteSeleccionado.text = "Selecciona un corte ↑"
                    binding.btnCheckInRemision.isEnabled = false

                    resetUIState()

                    if (::checkInAdapter.isInitialized) {
                        checkInAdapter.updateData(emptyList())
                    }
                }
            }
        }, { logout() })
    }

    private fun cargarTablaCheckIn() {
        if (corteSeleccionado == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.btnRefreshCheckInTable.alpha = 0.5f
                checkInAdapter.updateData(emptyList())

                val response = withContext(Dispatchers.IO) { RetrofitClient.instance.getCheckInSource(SourceRequest("{}")) }
                if (response.isSuccessful && response.body() != null) {
                    val filtrados = response.body()!!.data.filter { it.corte_id == corteSeleccionado?.id }
                    checkInAdapter.updateData(filtrados)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando tabla CheckIn: ${e.message}")
            } finally {
                binding.btnRefreshCheckInTable.alpha = 1.0f
            }
        }
    }

    private fun setupRecyclerViews() {
        cortesAdapter = CortesHomeAdapter(
            onItemSelected = { corte ->
                corteSeleccionado = corte
                binding.tvCorteSeleccionado.text = "Corte Seleccionado: ${corte.description}"
                binding.tvCorteSeleccionado.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                binding.btnCheckInRemision.isEnabled = true
                resetUIState()
                cargarTablaCheckIn()
            },
            onActionClick = { corte, accion -> ejecutarAccionCorte(corte, accion) }
        )
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = cortesAdapter

        checkInAdapter = CheckInAdapter(emptyList())
        binding.rvCheckInTable.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCheckInTable.adapter = checkInAdapter
    }

    private fun ejecutarAccionCorte(corte: CorteData, nuevoEstado: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = if (nuevoEstado == 0) "CERRANDO CORTE..." else "ACTUALIZANDO..."
            try {
                val request = CorteRequest(id = corte.id, user = sessionManager.getUsername() ?: "", i = "U", description = corte.description, state = nuevoEstado)
                withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }
                delay(600)
                iniciarFlujoDeCarga()
            } catch (e: Exception) { } finally { binding.overlayLoading.visibility = View.GONE }
        }
    }

    private fun setupMenu() {
        binding.toolbar.inflateMenu(R.menu.home_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> { logout(); true }
                R.id.create_rol -> { nav(CreateRolFragment()); true }
                R.id.create_corte -> { nav(CreateCorteFragment()); true }
                else -> false
            }
        }
    }

    private fun nav(fragment: Fragment) = parentFragmentManager.beginTransaction().replace(R.id.main_container, fragment).addToBackStack(null).commit()
    private fun logout() { sessionManager.clearSession(); parentFragmentManager.beginTransaction().replace(R.id.main_container, LoginFragment()).commit() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}