package com.example.myapplication.ui.rol

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentCreateRolBinding
import kotlinx.coroutines.launch
import java.io.IOException

class CreateRolFragment : Fragment(R.layout.fragment_create_rol) {

    private var _binding: FragmentCreateRolBinding? = null
    private val binding get() = _binding!!
    private lateinit var rolAdapter: RolAdapter

    private var isSysUser: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRolBinding.bind(view)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbarRol)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarRol.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        val sessionManager = SessionManager(requireContext())
        val userEmail = sessionManager.getUsername() ?: ""

        setupTable()
        validarPermisosSys(userEmail)
        cargarTabla()

        binding.btnRefreshTable.setOnClickListener { cargarTabla() }

        binding.btnCrearRol.setOnClickListener {
            val operacion = if (binding.btnCrearRol.text.toString().contains("ACTUALIZAR")) "U" else "C"
            procesarAccion(operacion)
        }

        binding.btnEliminarRol.setOnClickListener {
            procesarAccion("D")
        }
    }

    private fun validarPermisosSys(email: String) {
        if (email.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.checkIsSys(AccessRequest(email))
                if (response.isSuccessful) {
                    isSysUser = response.body()?.access ?: false
                }
            } catch (e: Exception) {
                Log.e("PERMISOS", "Error al validar SYS: ${e.message}")
            }
        }
    }

    private fun setupTable() {
        rolAdapter = RolAdapter(emptyList()) { rolSeleccionado ->
            binding.etUserEmail.setText(rolSeleccionado.user)
            binding.cbSys.isChecked = rolSeleccionado.sys == 1
            binding.cbAdmin.isChecked = rolSeleccionado.admin == 1
            binding.cbNormal.isChecked = rolSeleccionado.normal == 1

            binding.etUserEmail.isEnabled = false

            if (isSysUser) {
                binding.btnCrearRol.text = "ACTUALIZAR ROL"
                binding.btnCrearRol.isEnabled = true
                binding.btnEliminarRol.visibility = View.VISIBLE
            } else {
                binding.btnCrearRol.text = "MODO LECTURA"
                binding.btnCrearRol.isEnabled = false
                binding.btnEliminarRol.visibility = View.GONE
                Toast.makeText(requireContext(), "No tienes permisos de edición", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvRoles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rolAdapter
        }
    }

    private fun procesarAccion(operacion: String) {
        val email = binding.etUserEmail.text.toString().trim()
        if (email.isEmpty()) return

        val request = RolRequest(
            user = email,
            i = operacion,
            sys = if (binding.cbSys.isChecked) 1 else 0,
            admin = if (binding.cbAdmin.isChecked) 1 else 0,
            normal = if (binding.cbNormal.isChecked) 1 else 0
        )

        ejecutarPeticion(request)
    }

    private fun ejecutarPeticion(request: RolRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnCrearRol.isEnabled = false
            binding.btnEliminarRol.isEnabled = false

            try {
                // ✅ CORRECCIÓN: Se cambió 'crearOActualizarRol' por 'escribirRol' para coincidir con ApiService
                val response = RetrofitClient.instance.escribirRol(request)

                if (response.isSuccessful) {
                    val msg = when(request.i) {
                        "C" -> "✅ Registro creado"
                        "U" -> "✅ Registro actualizado"
                        "D" -> "🗑️ Registro eliminado"
                        else -> "Operación exitosa"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    limpiarInterfaz()
                    cargarTabla()
                } else {
                    Toast.makeText(requireContext(), "❌ Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "⚠️ Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnCrearRol.isEnabled = true
                if (isSysUser && binding.btnEliminarRol.visibility == View.VISIBLE) {
                    binding.btnEliminarRol.isEnabled = true
                }
            }
        }
    }

    private fun cargarTabla() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getRolesSource(SourceRequest("{}"))
                if (response.isSuccessful && response.body() != null) {
                    rolAdapter.updateData(response.body()!!.data)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error tabla: ${e.message}")
            }
        }
    }

    private fun limpiarInterfaz() {
        binding.etUserEmail.text.clear()
        binding.etUserEmail.isEnabled = true
        binding.cbSys.isChecked = false
        binding.cbAdmin.isChecked = false
        binding.cbNormal.isChecked = false
        binding.btnCrearRol.text = "CREAR ROL"
        binding.btnEliminarRol.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}