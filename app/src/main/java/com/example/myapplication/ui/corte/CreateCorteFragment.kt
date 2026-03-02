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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            binding.cbEstadoCorte.text = if (isChecked) "Estado: Abierto" else "Estado: Cerrado"
        }

        binding.btnGuardarCorte.setOnClickListener {
            val desc = binding.etDescription.text.toString().trim()
            if (desc.isNotEmpty()) {
                val op = if (currentCorteId == null) "C" else "U"
                val state = if (binding.cbEstadoCorte.isChecked) 1 else 0
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
            limpiarFormulario()
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

                // SECUENCIAL para evitar 'unexpected end of stream'
                val adminRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
                val sysRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsSys(AccessRequest(user)) }
                val cortesRes = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }

                if (adminRes.isSuccessful) isAdminUser = adminRes.body()?.access == true
                if (sysRes.isSuccessful) isSysUser = sysRes.body()?.access == true

                if (isAdminUser) {
                    binding.cbEstadoCorte.visibility = View.VISIBLE
                } else {
                    binding.btnGuardarCorte.text = "MODO LECTURA"
                    binding.btnGuardarCorte.isEnabled = false
                }

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
                    limpiarFormulario()
                    iniciarFlujoDeCarga()
                } else {
                    Toast.makeText(requireContext(), "Error en el servidor", Toast.LENGTH_SHORT).show()
                    binding.overlayLoading.visibility = View.GONE
                }
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
            binding.cbEstadoCorte.isChecked = item.state == 1

            if (isAdminUser) {
                binding.btnGuardarCorte.text = "ACTUALIZAR CORTE"
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
        binding.etDescription.text?.clear()
        binding.cbEstadoCorte.isChecked = false
        binding.btnGuardarCorte.text = if (isAdminUser) "CREAR CORTE" else "MODO LECTURA"
        binding.btnEliminarCorte.visibility = View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}