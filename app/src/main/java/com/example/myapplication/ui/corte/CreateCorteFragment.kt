package com.example.myapplication.ui.corte

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentCreateCorteBinding
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.utils.ejecutarFlujoSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateCorteFragment : Fragment(R.layout.fragment_create_corte) {
    private var _binding: FragmentCreateCorteBinding? = null
    private val binding get() = _binding!!
    private lateinit var corteAdapter: CorteAdapter
    private lateinit var session: SessionManager

    private var isSysUser = false
    private var isAdminUser = false
    private var currentCorteId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateCorteBinding.bind(view)
        session = SessionManager(requireContext())

        binding.toolbarCorte.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        setupTablaCortes()

        binding.cbEstadoCorte.setOnCheckedChangeListener { _, isChecked ->
            binding.cbEstadoCorte.text = if (isChecked) "Estado: Activo" else "Estado: Cerrado"
        }

        binding.btnGuardarCorte.setOnClickListener {
            val desc = binding.etDescription.text.toString().trim()
            if (desc.isNotEmpty()) {
                val op = if (currentCorteId == null) "C" else "U"
                val state = if (currentCorteId == null) 1 else (if (binding.cbEstadoCorte.isChecked) 1 else 0)
                ejecutarEscritura(CorteRequest(id = currentCorteId, user = session.getUsername() ?: "", i = op, description = desc, state = state))
            } else {
                Toast.makeText(requireContext(), "Falta descripción", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEliminarCorte.setOnClickListener {
            if (currentCorteId != null) {
                ejecutarEscritura(CorteRequest(id = currentCorteId, user = session.getUsername() ?: "", i = "D"))
            }
        }

        binding.tvActualizarLabel.setOnClickListener {
            iniciarFlujoDeCarga()
        }

        iniciarFlujoDeCarga()
    }

    private fun iniciarFlujoDeCarga() {
        ejecutarFlujoSeguro(
            tituloCarga = "CARGANDO CORTES",
            logTag = "CORTE_FLOW",
            accionCarga = {
                val user = session.getUsername() ?: ""

                val adminRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
                delay(200)
                val sysRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsSys(AccessRequest(user)) }
                delay(200)
                val cortesRes = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }

                if (adminRes.isSuccessful) isAdminUser = adminRes.body()?.access == true
                if (sysRes.isSuccessful) isSysUser = sysRes.body()?.access == true

                limpiarFormulario()

                if (cortesRes.isSuccessful && cortesRes.body() != null) {
                    corteAdapter.updateData(cortesRes.body()!!.data)
                }
            },
            onFalloSesion = {
                session.clearSession()
                parentFragmentManager.beginTransaction().replace(R.id.main_container, LoginFragment()).commit()
            }
        )
    }

    private fun ejecutarEscritura(request: CorteRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = if (request.i == "D") "ELIMINANDO CORTE" else "GUARDANDO CORTE"

            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }

                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Operación exitosa", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Aviso: Acción pre-procesada o error de servidor", Toast.LENGTH_LONG).show()
                }

                limpiarFormulario()
                delay(600) // Pausa para permitir que la BD termine su proceso
                iniciarFlujoDeCarga()

            } catch (e: Exception) {
                Log.e("CORTE_FLOW", "❌ Fallo escritura: ${e.message}")
                Toast.makeText(requireContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show()
                binding.overlayLoading.visibility = View.GONE
            }
        }
    }

    private fun setupTablaCortes() {
        corteAdapter = CorteAdapter(emptyList()) { item ->
            currentCorteId = item.id
            binding.etDescription.setText(item.description)

            if (isAdminUser) {
                binding.btnGuardarCorte.text = "ACTUALIZAR CORTE"
                binding.cbEstadoCorte.visibility = View.VISIBLE
                binding.cbEstadoCorte.isChecked = item.state == 1
            }
            if (isSysUser) {
                binding.btnEliminarCorte.visibility = View.VISIBLE
            }
        }
        binding.rvCortes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = corteAdapter
        }
    }

    private fun limpiarFormulario() {
        currentCorteId = null

        val calendar = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val hora = calendar.get(Calendar.HOUR_OF_DAY)
        val sufijoTurno = if (hora < 13) "1/2" else "2/2"
        binding.etDescription.setText("Corte ${formatoFecha.format(calendar.time)} $sufijoTurno")

        binding.cbEstadoCorte.visibility = View.GONE
        binding.btnEliminarCorte.visibility = View.GONE

        if (isAdminUser) {
            binding.btnGuardarCorte.text = "CREAR CORTE"
            binding.btnGuardarCorte.isEnabled = true
            binding.etDescription.isEnabled = true
        } else {
            binding.btnGuardarCorte.text = "MODO LECTURA"
            binding.btnGuardarCorte.isEnabled = false
            binding.etDescription.isEnabled = false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}