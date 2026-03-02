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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateCorteBinding.bind(view)
        session = SessionManager(requireContext())

        binding.toolbarCorte.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        setupTablaCortes()

        binding.btnGuardarCorte.setOnClickListener {
            val desc = binding.etDescription.text.toString().trim()
            if (desc.isNotEmpty()) ejecutarEscritura(CorteRequest(user = session.getUsername() ?: "", i = "C", description = desc))
        }

        binding.tvActualizarLabel.setOnClickListener { iniciarFlujoDeCarga() }
        iniciarFlujoDeCarga()
    }

    private fun iniciarFlujoDeCarga() {
        ejecutarFlujoSeguro(
            tituloCarga = "CARGANDO CORTES",
            logTag = "CORTE_FLOW",
            accionCarga = {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }
                if (res.isSuccessful && res.body() != null) {
                    corteAdapter.updateData(res.body()!!.data)
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
            binding.tvLoadingTitle.text = "GUARDANDO CORTE"

            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }
                if (res.isSuccessful) {
                    binding.etDescription.text?.clear()
                    Toast.makeText(requireContext(), "Corte creado", Toast.LENGTH_SHORT).show()
                    iniciarFlujoDeCarga() // Re-ejecuta validación y descarga de tablas
                }
            } catch (e: Exception) {
                Log.e("CORTE_FLOW", "❌ Fallo escritura: ${e.message}")
                binding.overlayLoading.visibility = View.GONE
            }
        }
    }

    private fun setupTablaCortes() {
        corteAdapter = CorteAdapter(emptyList()) { binding.etDescription.setText(it.description) }
        binding.rvCortes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = corteAdapter
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}