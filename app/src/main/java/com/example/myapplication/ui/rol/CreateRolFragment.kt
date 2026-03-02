package com.example.myapplication.ui.rol

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
import com.example.myapplication.databinding.FragmentCreateRolBinding
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.utils.ejecutarFlujoSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragmento de Gestión de Roles de Usuario.
 * Restringe su funcionamiento completo únicamente a usuarios con privilegios Sys (Sistema).
 */
class CreateRolFragment : Fragment(R.layout.fragment_create_rol) {

    private var _binding: FragmentCreateRolBinding? = null
    private val binding get() = _binding!!
    private lateinit var rolAdapter: RolAdapter
    private lateinit var sessionManager: SessionManager
    private var isSysUser: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRolBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        binding.toolbarRol.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        setupTable()

        binding.btnRefreshTable.setOnClickListener { iniciarFlujoDeCarga() }
        binding.btnCrearRol.setOnClickListener { procesarAccion(if (binding.btnCrearRol.text.toString().contains("ACTUALIZAR")) "U" else "C") }
        binding.btnEliminarRol.setOnClickListener { procesarAccion("D") }

        iniciarFlujoDeCarga()
    }

    private fun iniciarFlujoDeCarga() {
        ejecutarFlujoSeguro(
            tituloCarga = "CARGANDO MÓDULO DE ROLES",
            logTag = "ROL_FLOW",
            accionCarga = {
                val userEmail = sessionManager.getUsername() ?: ""

                val sysRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsSys(AccessRequest(userEmail)) }
                delay(200) // Freno de saturación
                val rolesRes = withContext(Dispatchers.IO) { RetrofitClient.instance.getRolesSource(SourceRequest("{}")) }

                if (sysRes.isSuccessful) isSysUser = sysRes.body()?.access ?: false

                if (rolesRes.isSuccessful && rolesRes.body() != null) {
                    rolAdapter.updateData(rolesRes.body()!!.data)
                }

                if (!isSysUser) {
                    binding.btnCrearRol.text = "MODO LECTURA"
                    binding.btnCrearRol.isEnabled = false
                    binding.btnEliminarRol.visibility = View.GONE
                }
            },
            onFalloSesion = {
                sessionManager.clearSession()
                parentFragmentManager.beginTransaction().replace(R.id.main_container, LoginFragment()).commit()
            }
        )
    }

    private fun procesarAccion(operacion: String) {
        val email = binding.etUserEmail.text.toString().trim()
        if (email.isEmpty()) return

        val request = RolRequest(email, operacion, if (binding.cbSys.isChecked) 1 else 0, if (binding.cbAdmin.isChecked) 1 else 0, if (binding.cbNormal.isChecked) 1 else 0)

        viewLifecycleOwner.lifecycleScope.launch {
            // ✅ Protección Anti-Double-Click
            binding.btnCrearRol.isEnabled = false
            binding.btnEliminarRol.isEnabled = false

            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = if (operacion == "D") "ELIMINANDO ROL" else "GUARDANDO ROL"

            try {
                val response = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirRol(request) }

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Operación exitosa", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Aviso: El rol ya no existe o error en servidor", Toast.LENGTH_LONG).show()
                }

                limpiarInterfaz()
                delay(600) // Respiro DB
                iniciarFlujoDeCarga()

            } catch (e: Exception) {
                Log.e("ROL_FLOW", "❌ Fallo escritura: ${e.message}")
                binding.overlayLoading.visibility = View.GONE
                binding.btnCrearRol.isEnabled = true
                if (isSysUser && binding.btnEliminarRol.visibility == View.VISIBLE) binding.btnEliminarRol.isEnabled = true
            }
        }
    }

    private fun setupTable() {
        rolAdapter = RolAdapter(emptyList()) { item ->
            binding.etUserEmail.setText(item.user)
            binding.cbSys.isChecked = item.sys == 1
            binding.cbAdmin.isChecked = item.admin == 1
            binding.cbNormal.isChecked = item.normal == 1
            binding.etUserEmail.isEnabled = false

            if (isSysUser) {
                binding.btnCrearRol.text = "ACTUALIZAR ROL"
                binding.btnEliminarRol.visibility = View.VISIBLE
            }
        }
        binding.rvRoles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rolAdapter
        }
    }

    private fun limpiarInterfaz() {
        binding.etUserEmail.text?.clear()
        binding.etUserEmail.isEnabled = true
        binding.cbSys.isChecked = false; binding.cbAdmin.isChecked = false; binding.cbNormal.isChecked = false
        binding.btnEliminarRol.visibility = View.GONE

        if (isSysUser) {
            binding.btnCrearRol.text = "CREAR ROL"
            binding.btnCrearRol.isEnabled = true
        } else {
            binding.btnCrearRol.text = "MODO LECTURA"
            binding.btnCrearRol.isEnabled = false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}