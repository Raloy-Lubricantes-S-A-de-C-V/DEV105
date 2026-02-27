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
import kotlinx.coroutines.*

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val MAX_INTENTOS = 3
    private lateinit var cortesAdapter: CortesHomeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        setupRecyclerView()
        iniciarCicloValidacion()
    }

    private fun iniciarCicloValidacion() {
        val email = SessionManager(requireContext()).getUsername() ?: ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            for (intento in 1..MAX_INTENTOS) {
                if (_binding == null) break
                binding.loader.visibility = View.VISIBLE
                binding.tvStatusError.visibility = View.GONE

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.checkIsAdmin(AccessRequest(email))
                    }

                    if (response.isSuccessful) {
                        val isAdmin = response.body()?.access ?: false
                        binding.sectionCortesAbiertos.visibility = if (isAdmin) View.VISIBLE else View.GONE
                        binding.loader.visibility = View.GONE
                        return@launch
                    } else if (response.code() == 401) {
                        doLogout("No autorizado (401)")
                        return@launch
                    }
                } catch (e: Exception) { }

                if (intento < MAX_INTENTOS) {
                    binding.loader.visibility = View.GONE
                    delay(10000)
                } else {
                    doLogout("Error de conexión tras 3 intentos")
                }
            }
        }
    }

    private fun doLogout(msj: String) {
        SessionManager(requireContext()).clearSession()
        Toast.makeText(requireContext(), msj, Toast.LENGTH_LONG).show()
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, LoginFragment())
            .commitAllowingStateLoss()
    }

    private fun setupRecyclerView() {
        cortesAdapter = CortesHomeAdapter({}, { _, _ -> })
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = cortesAdapter
    }
}