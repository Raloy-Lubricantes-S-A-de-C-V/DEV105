package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.async

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

                // Descarga concurrente
                val adminResDeferred = async(Dispatchers.IO) { RetrofitClient.instance.checkIsAdmin(AccessRequest(user)) }
                val cortesResDeferred = async(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }

                val adminRes = adminResDeferred.await()
                val cortesRes = cortesResDeferred.await()

                if (adminRes.isSuccessful && adminRes.body()?.access == true) {
                    binding.sectionCortesAbiertos.visibility = View.VISIBLE
                    if (cortesRes.isSuccessful && cortesRes.body() != null) {
                        cortesAdapter.updateData(cortesRes.body()!!.data)
                    }
                }
            },
            onFalloSesion = { logout() }
        )
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

    private fun setupRecyclerView() {
        cortesAdapter = CortesHomeAdapter(onItemSelected = {}, onActionClick = { _, _ -> })
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = cortesAdapter
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