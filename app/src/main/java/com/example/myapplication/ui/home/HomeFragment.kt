package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.corte.CreateCorteFragment
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.ui.rol.CreateRolFragment
import kotlinx.coroutines.*

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        // 1. Configurar Toolbar con el nombre del módulo (Estilo Roles)
        binding.toolbar.title = "Panel Principal - Raloy"
        setupMenu()

        setupRecyclerView()

        // 2. Botón de Actualizar manual
        binding.btnRefreshCortes.setOnClickListener { cargarCortes() }

        // 3. Carga inicial de datos y validación de permisos
        validarYRefrescar()
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

    private fun validarYRefrescar() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.loader.visibility = View.VISIBLE
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.verificarSesion() }
                if (res.isSuccessful) {
                    binding.loader.visibility = View.GONE
                    binding.welcomeText.text = "Bienvenido: ${sessionManager.getUsername()}"
                    verificarAdmin(sessionManager.getUsername() ?: "")
                } else {
                    logout()
                }
            } catch (e: Exception) {
                binding.loader.visibility = View.GONE
            }
        }
    }

    private fun verificarAdmin(user: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
                if (res.isSuccessful && res.body()?.access == true) {
                    binding.sectionCortesAbiertos.visibility = View.VISIBLE
                    cargarCortes()
                }
            } catch (e: Exception) { }
        }
    }

    private fun cargarCortes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }
                if (res.isSuccessful && res.body() != null) {
                    (binding.rvCortesAbiertos.adapter as? CortesHomeAdapter)?.updateData(res.body()!!.data)
                }
            } catch (e: Exception) { }
        }
    }

    private fun setupRecyclerView() {
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = CortesHomeAdapter(
            onItemSelected = { },
            onActionClick = { _, _ -> }
        )
    }

    private fun nav(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.main_container, fragment)
            .addToBackStack(null).commit()
    }

    private fun logout() {
        sessionManager.clearSession()
        parentFragmentManager.beginTransaction().replace(R.id.main_container, LoginFragment()).commit()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}