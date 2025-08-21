package com.Aman.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.Aman.myapplication.SupabaseAuthRepository
import com.Aman.myapplication.AuthResult
import com.Aman.myapplication.User
import com.Aman.myapplication.UserProfile

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Loading : AuthState()
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

class SupabaseAuthViewModel : ViewModel() {
    private val authRepository = SupabaseAuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Add profile update state
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                Log.d("SupabaseAuthVM", "Checking auth state...")
                _authState.value = AuthState.Loading

                val currentUser = authRepository.getCurrentUser()
                Log.d("SupabaseAuthVM", "Current user from repository: ${currentUser?.id}")

                if (currentUser != null) {
                    Log.d("SupabaseAuthVM", "User authenticated: ${currentUser.id}")
                    _authState.value = AuthState.Authenticated(currentUser)
                    loadUserProfile()
                } else {
                    Log.d("SupabaseAuthVM", "No authenticated user found")
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuthVM", "Error checking auth state: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            when (val result = authRepository.signUp(email, password, fullName, phone)) {
                is AuthResult.Success -> {
                    Log.d("SupabaseAuthVM", "Signup successful: ${result.user.id}")
                    _authState.value = AuthState.Authenticated(result.user)
                    loadUserProfile()
                }
                is AuthResult.Error -> {
                    Log.e("SupabaseAuthVM", "Signup failed: ${result.message}")
                    _authState.value = AuthState.Error(result.message)
                }
            }
            _isLoading.value = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    Log.d("SupabaseAuthVM", "Signin successful: ${result.user.id}")
                    _authState.value = AuthState.Authenticated(result.user)
                    loadUserProfile()
                }
                is AuthResult.Error -> {
                    Log.e("SupabaseAuthVM", "Signin failed: ${result.message}")
                    _authState.value = AuthState.Error(result.message)
                }
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Log.d("SupabaseAuthVM", "Signing out")
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
            _userProfile.value = null
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            val success = authRepository.resetPassword(email)
            _resetPasswordState.value = if (success) {
                ResetPasswordState.Success
            } else {
                ResetPasswordState.Error("Failed to send reset email")
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                // Get the current user ID from auth state
                val currentUserId = when (val currentAuthState = _authState.value) {
                    is AuthState.Authenticated -> currentAuthState.user.id
                    else -> {
                        Log.w("SupabaseAuthVM", "Cannot load profile - user not authenticated")
                        return@launch
                    }
                }

                Log.d("SupabaseAuthVM", "Loading profile for user: $currentUserId")

                val profile = authRepository.getUserProfile(currentUserId)
                if (profile != null) {
                    Log.d("SupabaseAuthVM", "Profile loaded successfully")
                    _userProfile.value = profile
                } else {
                    Log.w("SupabaseAuthVM", "Profile not found for user: $currentUserId")
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuthVM", "Error loading user profile: ${e.message}", e)
            }
        }
    }

    // Add a method to get current user ID reliably
    fun getCurrentUserId(): String {
        return when (val currentAuthState = _authState.value) {
            is AuthState.Authenticated -> {
                Log.d("SupabaseAuthVM", "Getting current user ID: ${currentAuthState.user.id}")
                currentAuthState.user.id
            }
            else -> {
                Log.w("SupabaseAuthVM", "No authenticated user for getting user ID, state: $currentAuthState")
                ""
            }
        }
    }

    // Add this method for updating profile
    fun updateProfile(fullName: String, phone: String) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading

            try {
                val currentProfile = _userProfile.value
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        full_name = fullName,
                        phone = phone
                    )

                    val success = authRepository.updateUserProfile(updatedProfile)
                    if (success) {
                        _userProfile.value = updatedProfile
                        _profileUpdateState.value = ProfileUpdateState.Success
                    } else {
                        _profileUpdateState.value = ProfileUpdateState.Error("Failed to update profile")
                    }
                } else {
                    _profileUpdateState.value = ProfileUpdateState.Error("No profile found")
                }
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun uploadProfileImage(imageUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentProfile = _userProfile.value
            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(avatar_url = imageUrl)
                val success = authRepository.updateUserProfile(updatedProfile)
                if (success) {
                    _userProfile.value = updatedProfile
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearResetPasswordState() {
        _resetPasswordState.value = ResetPasswordState.Idle
    }

    fun clearProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }
}

// Add this sealed class for profile update states
sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}