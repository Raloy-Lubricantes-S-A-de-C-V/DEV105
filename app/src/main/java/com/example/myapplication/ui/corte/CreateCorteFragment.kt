package com.example.myapplication.ui.corte

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentCreateCorteBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CreateCorteFragment : Fragment(R.layout.fragment_create_corte) {
    private var _binding: FragmentCreateCorteBinding? = null
    private val binding get() = _binding!!
    private val adapter = CorteAdapter(emptyList()) { seleccionar(it) }
    private var selectedId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateCorteBinding.bind(view)

        binding.rvCortes.layoutManager = LinearLayoutManager(context)
        binding.rvCortes.adapter = adapter

        reiniciarForm()
        cargar()

        binding.btnGuardarCorte.setOnClickListener { ejecutar("C") }
        binding.btnEliminarCorte.setOnClickListener { ejecutar("D") }
    }

    private fun generarSugerencia(): String {
        val cal = Calendar.getInstance()
        val fecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(cal.time)
        val turno = if (cal.get(Calendar.HOUR_OF_DAY) < 13) "1/2" else "2/2"
        return "Corte $fecha - $turno"
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.getCortesSource(SourceRequest("Carga")) }
                if (res.isSuccessful) adapter.updateData(res.body()?.data ?: emptyList())
            } catch (e: Exception) { }
        }
    }

    private fun ejecutar(op: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.loader.visibility = View.VISIBLE
            try {
                val request = CorteRequest(id = selectedId, user = SessionManager(requireContext()).getUsername()!!, i = op, description = binding.etDescription.text.toString(), state = 1)
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(request) }
                if (res.isSuccessful) {
                    delay(1000)
                    reiniciarForm()
                    cargar()
                }
            } finally {
                if (_binding != null) binding.loader.visibility = View.GONE
            }
        }
    }

    private fun seleccionar(c: CorteData) {
        selectedId = c.id
        binding.etDescription.setText(c.description)
        binding.btnGuardarCorte.text = "ACTUALIZAR"
        binding.btnEliminarCorte.visibility = View.VISIBLE
    }

    private fun reiniciarForm() {
        selectedId = null
        binding.etDescription.setText(generarSugerencia())
        binding.btnGuardarCorte.text = "CREAR CORTE"
        binding.btnEliminarCorte.visibility = View.GONE
    }
}