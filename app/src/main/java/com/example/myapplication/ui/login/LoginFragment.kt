package com.example.myapplication.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.network.AuthInterceptor
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.databinding.FragmentLoginBinding
import com.example.myapplication.ui.home.HomeFragment

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el ViewModel con su Factory
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(requireContext())
        )[LoginViewModel::class.java]

        // Observar el resultado del Login
        loginViewModel.loginResult.observe(viewLifecycleOwner) { success ->
            if (success == null) return@observe

            binding.loading.visibility = View.GONE
            binding.login.isEnabled = true

            if (success) {
                // 🔐 1. PERSISTENCIA DE SESIÓN
                // Guardamos el email ingresado para que el AuthInterceptor lo use
                val emailIngresado = binding.username.text.toString()
                val session = SessionManager(requireContext())
                session.saveSession(0, emailIngresado)

                // 🔐 2. RESET DEL INTERCEPTOR
                // Esta llamada ya no marcará error al añadir el companion object
                AuthInterceptor.resetToken()

                Toast.makeText(requireContext(), "Acceso concedido", Toast.LENGTH_SHORT).show()

                // 🚀 3. NAVEGACIÓN AL HOME
                // Usamos el ID 'main_container' definido en activity_main.xml
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, HomeFragment())
                    .commitAllowingStateLoss()
            } else {
                Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar clic del botón
        binding.login.setOnClickListener {
            val user = binding.username.text.toString()
            val pass = binding.password.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                binding.loading.visibility = View.VISIBLE
                binding.login.isEnabled = false
                loginViewModel.login(user, pass)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}