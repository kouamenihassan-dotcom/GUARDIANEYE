package com.example.guardianeye.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardianeye.repository.FirebaseRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginSuccess = MutableLiveData(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _registerSuccess = MutableLiveData(false)
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                repository.login(email, password)
                _loginSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur de connexion"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                repository.register(email, password)
                _registerSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur d'inscription"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerDevice(deviceId: String, fcmToken: String) {
        viewModelScope.launch {
            try {
                repository.registerDevice(deviceId, fcmToken)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isUserLoggedIn() = repository.getCurrentUser() != null

    fun logout() = repository.logout()
}