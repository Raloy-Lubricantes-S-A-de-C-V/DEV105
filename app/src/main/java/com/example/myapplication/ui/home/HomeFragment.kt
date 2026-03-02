package com.example.myapplication.ui.home

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
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.corte.CreateCorteFragment
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.ui.rol.CreateRolFragment
import com.example.myapplication.utils.ejecutarFlujoSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragmento Principal del Kiosko.
 * Administra la vista general y permite operar rápidamente Cortes activos.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var cortesAdapter: CortesHomeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        binding.toolbar.title = "Panel Principal - Raloy"
        setupMenu()
        setupRecyclerView()

        binding.btnRefreshCortes.setOnClickListener { iniciarFlujoDeCarga() }
        iniciarFlujoDeCarga()
    }

    private fun iniciarFlujoDeCarga() {
        ejecutarFlujoSeguro(
            tituloCarga = "CARGANDO PANEL PRINCIPAL",
            logTag = "HOME_FLOW",
            accionCarga = {
                val user = sessionManager.getUsername() ?: ""
                binding.welcomeText.text = "Bienvenido: $user"

                val adminRes = withContext(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
                delay(200) // Freno anti-saturación
                val cortesRes = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }

                if (adminRes.isSuccessful && adminRes.body()?.access == true) {
                    binding.sectionCortesAbiertos.visibility = View.VISIBLE
                    if (cortesRes.isSuccessful && cortesRes.body() != null) {
                        // Filtro de negocio: Mostrar únicamente Cortes Activos
                        val cortesActivos = cortesRes.body()!!.data.filter { it.state == 1 }
                        cortesAdapter.updateData(cortesActivos)
                    }
                }
            },
            onFalloSesion = { logout() }
        )
    }

    private fun ejecutarAccionBoton(corte: CorteData, nuevoEstado: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayLoading.visibility = View.VISIBLE
            binding.tvLoadingTitle.text = if (nuevoEstado == 0) "CERRANDO CORTE..." else "ACTUALIZANDO..."

            try {
                val request = CorteRequest(
                    id = corte.id,
                    user = sessionManager.getUsername() ?: "",
                    i = "U",
                    description = corte.description,
                    state = nuevoEstado
                )
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }

                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Corte actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Aviso: Corte modificado previamente", Toast.LENGTH_LONG).show()
                }

                delay(600) // Pausa para permitir Commits en DB
                iniciarFlujoDeCarga()

            } catch (e: Exception) {
                Log.e("HOME_FLOW", "Error acción: ${e.message}")
                Toast.makeText(requireContext(), "Fallo de red", Toast.LENGTH_SHORT).show()
                binding.overlayLoading.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        cortesAdapter = CortesHomeAdapter(
            onItemSelected = {},
            onActionClick = { corte, accion ->
                ejecutarAccionBoton(corte, accion)
            }
        )
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = cortesAdapter
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

    private fun nav(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.main_container, fragment).addToBackStack(null).commit()
    }

    private fun logout() {
        sessionManager.clearSession()
        parentFragmentManager.beginTransaction().replace(R.id.main_container, LoginFragment()).commit()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}