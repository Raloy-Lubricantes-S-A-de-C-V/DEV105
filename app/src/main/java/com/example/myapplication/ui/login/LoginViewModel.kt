package com.example.myapplication.ui.login

import androidx.lifecycle.*
import com.example.myapplication.data.LoginRepository
import com.example.myapplication.data.Result
import kotlinx.coroutines.launch
import com.example.myapplication.R

class LoginViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = loginRepository.login(username, password)
            _loginResult.value = result is Result.Success
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (username.isBlank()) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (password.length < 4) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }
}