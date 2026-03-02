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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateCorteFragment : Fragment(R.layout.fragment_create_corte) {
    private var _binding: FragmentCreateCorteBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateCorteBinding.bind(view)
        val session = SessionManager(requireContext())

        binding.toolbarCorte.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        binding.rvCortes.layoutManager = LinearLayoutManager(requireContext())

        binding.btnGuardarCorte.setOnClickListener {
            val desc = binding.etDescription.text.toString()
            if (desc.isNotEmpty()) {
                ejecutarEscritura(CorteRequest(user = session.getUsername() ?: "", i = "C", description = desc))
            }
        }

        binding.tvActualizarLabel.setOnClickListener { consultarTabla() }
        consultarTabla()
    }

    private fun ejecutarEscritura(request: CorteRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.loader.visibility = View.VISIBLE
            try {
                Log.d("CORTE_API", "Enviando: $request")
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }
                if (res.isSuccessful) {
                    Log.d("CORTE_API", "Respuesta exitosa: ${res.body()}")
                    binding.etDescription.text?.clear()
                    consultarTabla()
                }
            } catch (e: Exception) {
                Log.e("CORTE_API", "Fallo en ejecución: ${e.message}")
            } finally {
                binding.loader.visibility = View.GONE
            }
        }
    }

    private fun consultarTabla() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("{}")) }
                if (res.isSuccessful && res.body() != null) {
                    Log.d("CORTE_API", "Cortes en tabla: ${res.body()?.count}")
                    // Aquí se asignaría el adaptador: binding.rvCortes.adapter = ...
                }
            } catch (e: Exception) {
                Log.e("CORTE_API", "Error al consultar tabla")
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}