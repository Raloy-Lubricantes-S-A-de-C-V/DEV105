package com.example.myapplication.ui.login

import androidx.lifecycle.*
import com.example.myapplication.data.LoginRepository
import com.example.myapplication.data.Result
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            // Llamada al repositorio que usa OdooAuthService internamente
            val result = loginRepository.login(username, password)
            _loginResult.value = result is Result.Success
        }
    }
}