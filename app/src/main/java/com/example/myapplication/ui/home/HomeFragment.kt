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
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.ui.rol.CreateRolFragment
import kotlinx.coroutines.*

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val MAX_INTENTOS = 3
    private val DELAY_REINTENTO = 5000L
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        setupToolbar() // ✅ Configuración del menú
        setupRecyclerView()
        iniciarCicloValidacion()
    }

    private fun setupToolbar() {
        // ✅ Carga el archivo home_menu.xml en el Toolbar
        binding.toolbar.inflateMenu(R.menu.home_menu)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    doLogout("Sesión cerrada")
                    true
                }
                R.id.create_rol -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, CreateRolFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun iniciarCicloValidacion() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            var exitoso = false
            for (intento in 1..MAX_INTENTOS) {
                if (_binding == null) break
                binding.loader.visibility = View.VISIBLE

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.verificarSesion()
                    }
                    if (response.isSuccessful) {
                        exitoso = true
                        break
                    }
                } catch (e: Exception) { }

                if (intento < MAX_INTENTOS) {
                    binding.loader.visibility = View.GONE
                    delay(DELAY_REINTENTO)
                }
            }

            if (exitoso) {
                binding.loader.visibility = View.GONE
                binding.welcomeText.text = "Usuario: ${sessionManager.getUsername()}"
                validarPermisosAdmin()
            } else {
                doLogout("Error de verificación de API tras $MAX_INTENTOS intentos.")
            }
        }
    }

    private fun validarPermisosAdmin() {
        val email = sessionManager.getUsername() ?: ""
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(email)) }
                if (res.isSuccessful && res.body()?.access == true) {
                    binding.sectionCortesAbiertos.visibility = View.VISIBLE
                }
            } catch (e: Exception) { }
        }
    }

    private fun setupRecyclerView() {
        val adapter = CortesHomeAdapter(
            onItemSelected = { _: CorteData -> },
            onActionClick = { _: CorteData, _: Int -> }
        )
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = adapter
    }

    private fun doLogout(msj: String) {
        sessionManager.clearSession()
        Toast.makeText(requireContext(), msj, Toast.LENGTH_LONG).show()
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, LoginFragment())
            .commitAllowingStateLoss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}