package com.example.myapplication.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.network.*
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.login.LoginFragment
import kotlinx.coroutines.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var isAdmin = false
    private val MAX_INTENTOS = 3
    private val TIEMPO_ESPERA = 10000L // 10 segundos
    private lateinit var cortesAdapter: CortesHomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        iniciarCicloValidacion()

        binding.btnVerificarSesion.setOnClickListener {
            iniciarCicloValidacion()
        }
    }

    private fun setupRecyclerView() {
        cortesAdapter = CortesHomeAdapter(
            onItemSelected = {
                binding.btnRegistrarCheckIn.isEnabled = true
                binding.sectionCheckIn.visibility = View.VISIBLE
            },
            onActionClick = { corte, estado -> actualizarEstadoCorte(corte, estado) }
        )
        binding.rvCortesAbiertos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCortesAbiertos.adapter = cortesAdapter
    }

    private fun iniciarCicloValidacion() {
        val session = SessionManager(requireContext())
        val email = session.getUsername() ?: ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            for (intento in 1..MAX_INTENTOS) {
                if (_binding == null) break

                binding.loader.visibility = View.VISIBLE
                binding.tvStatusError.visibility = View.GONE

                try {
                    // El Interceptor inyectará Authorization: Bearer <TOKEN>
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.checkIsAdmin(AccessRequest(email))
                    }

                    if (response.isSuccessful) {
                        isAdmin = response.body()?.access ?: false
                        actualizarInterfazUI()
                        binding.loader.visibility = View.GONE
                        return@launch
                    } else if (response.code() == 401) {
                        doLogout("Token inválido o expirado (401)")
                        return@launch
                    } else {
                        mostrarErrorUI("Error servidor: ${response.code()}", intento)
                    }
                } catch (e: Exception) {
                    mostrarErrorUI("Sin conexión", intento)
                }

                if (intento < MAX_INTENTOS) {
                    binding.loader.visibility = View.GONE
                    delay(TIEMPO_ESPERA)
                } else {
                    doLogout("Agotados los 3 intentos de conexión")
                }
            }
        }
    }

    private fun mostrarErrorUI(msj: String, num: Int) {
        if (_binding == null) return
        binding.tvStatusError.visibility = View.VISIBLE
        binding.tvStatusError.text = "⚠️ $msj ($num/$MAX_INTENTOS). Reintento en 10s..."
    }

    private fun actualizarInterfazUI() {
        if (_binding == null) return
        val vis = if (isAdmin) View.VISIBLE else View.GONE
        binding.sectionCortesAbiertos.visibility = vis
        binding.sectionCheckIn.visibility = vis
        if (isAdmin) cargarCortesAdmin()
    }

    private fun cargarCortesAdmin() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getCortesSource(SourceRequest("Home"))
                }
                if (res.isSuccessful && _binding != null) {
                    val lista = res.body()?.data?.filter { it.state == 1 } ?: emptyList()
                    cortesAdapter.updateData(lista)
                }
            } catch (e: Exception) {
                Log.e("HOME", "Fallo al cargar tabla")
            }
        }
    }

    private fun actualizarEstadoCorte(corte: CorteData, nuevoEstado: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.loader.visibility = View.VISIBLE
            try {
                val req = CorteRequest(corte.id, corte.user, "U", nuevoEstado, corte.description)
                val res = withContext(Dispatchers.IO) { RetrofitClient.instance.escribirCorte(req) }
                if (res.isSuccessful) {
                    delay(1500)
                    parentFragmentManager.beginTransaction().detach(this@HomeFragment).attach(this@HomeFragment).commit()
                }
            } finally {
                if (_binding != null) binding.loader.visibility = View.GONE
            }
        }
    }

    private fun doLogout(motivo: String) {
        if (!isAdded) return
        SessionManager(requireContext()).clearSession()
        Toast.makeText(requireContext(), motivo, Toast.LENGTH_LONG).show()

        val containerId = (view?.parent as? View)?.id ?: android.R.id.content
        parentFragmentManager.beginTransaction()
            .replace(containerId, LoginFragment())
            .commitAllowingStateLoss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}