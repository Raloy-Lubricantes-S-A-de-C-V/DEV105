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

    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        setupToolbar()
        setupRecyclerView()
        iniciarValidacionSesion()
    }

    private fun setupToolbar() {
        // ✅ SOLUCIÓN AL MENÚ: Inflamos el menú manualmente en el toolbar del Fragment
        binding.toolbar.inflateMenu(R.menu.home_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> { doLogout("Sesión finalizada"); true }
                R.id.create_rol -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, CreateRolFragment())
                        .addToBackStack(null).commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun iniciarValidacionSesion() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.loader.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { RetrofitClient.instance.verificarSesion() }

                if (response.isSuccessful) {
                    binding.loader.visibility = View.GONE
                    binding.welcomeText.text = "Bienvenido, ${sessionManager.getUsername()}"
                    // ✅ ACTIVAR DISEÑO: Mostramos las secciones ocultas
                    binding.sectionCheckIn.visibility = View.VISIBLE
                    validarAdmin(sessionManager.getUsername() ?: "")
                } else {
                    // ✅ TOKEN VENCIDO: Cierre automático
                    doLogout("Su sesión ha expirado. Ingrese nuevamente.")
                }
            } catch (e: Exception) {
                binding.loader.visibility = View.GONE
                binding.tvStatusError.apply {
                    visibility = View.VISIBLE
                    text = "Sin conexión con servidor Raloy"
                }
            }
        }
    }

    private fun validarAdmin(email: String) {
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
        val adapter = CortesHomeAdapter(onItemSelected = { _: CorteData -> }, onActionClick = { _: CorteData, _: Int -> })
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