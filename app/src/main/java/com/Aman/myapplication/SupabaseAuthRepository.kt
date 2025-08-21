package com.Aman.myapplication

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Patterns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val full_name: String? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class SupabaseAuthRepository {
    private val client = SupabaseConfig.client

    suspend fun signUp(emailParam: String, passwordParam: String, fullName: String, phone: String): AuthResult {
        return try {
            if (!Patterns.EMAIL_ADDRESS.matcher(emailParam).matches()) {
                return AuthResult.Error("Invalid email format")
            }

            if (passwordParam.length < 6) {
                return AuthResult.Error("Password must be at least 6 characters")
            }

            if (fullName.trim().isEmpty()) {
                return AuthResult.Error("Full name is required")
            }

            if (phone.trim().isEmpty()) {
                return AuthResult.Error("Phone number is required")
            }

            // Basic phone validation (adjust regex as needed for your requirements)
            if (!phone.matches(Regex("^[+]?[0-9]{10,15}$"))) {
                return AuthResult.Error("Invalid phone number format")
            }

            val result = client.auth.signUpWith(Email) {
                email = emailParam
                password = passwordParam
                data = buildJsonObject {
                    put("full_name", fullName)
                    put("phone", phone)
                }
            }

            // Get current user after successful signup
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser != null) {
                // The profile should be automatically created by the database trigger
                // But we can also manually ensure it exists
                try {
                    val userProfile = UserProfile(
                        id = currentUser.id,
                        email = emailParam,
                        full_name = fullName,
                        phone = phone
                    )

                    // Try to insert profile (might not be needed if trigger works)
                    client.from("profiles").insert(userProfile)
                } catch (profileError: Exception) {
                    // Profile might already exist from trigger, that's okay
                    println("Profile creation note: ${profileError.message}")
                }

                AuthResult.Success(
                    User(
                        id = currentUser.id,
                        email = currentUser.email ?: emailParam
                    )
                )
            } else {
                AuthResult.Error("Sign up failed - no user returned")
            }

        } catch (e: Exception) {
            AuthResult.Error("Sign up failed: ${e.message}")
        }
    }

    suspend fun signIn(emailParam: String, passwordParam: String): AuthResult {
        return try {
            if (!Patterns.EMAIL_ADDRESS.matcher(emailParam).matches()) {
                return AuthResult.Error("Invalid email format")
            }

            val result = client.auth.signInWith(Email) {
                email = emailParam
                password = passwordParam
            }

            // Get current user after successful signin
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser != null) {
                AuthResult.Success(
                    User(
                        id = currentUser.id, // Use the actual user ID
                        email = currentUser.email ?: emailParam
                    )
                )
            } else {
                AuthResult.Error("Sign in failed - no user returned")
            }

        } catch (e: Exception) {
            AuthResult.Error("Invalid email or password: ${e.message}")
        }
    }

    suspend fun signOut(): Boolean {
        return try {
            client.auth.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser != null) {
                User(
                    id = currentUser.id,
                    email = currentUser.email ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getAuthStateFlow(): Flow<Boolean> = flow {
        emit(client.auth.currentUserOrNull() != null)
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            client.auth.resetPasswordForEmail(email)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            client.from("profiles")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            client.from("profiles")
                .update(profile) {
                    filter {
                        eq("id", profile.id)
                    }
                }
            true
        } catch (e: Exception) {
            false
        }
    }
// Add these methods to SupabaseAuthRepository.kt

    suspend fun getUserByEmail(email: String): User? {
        return try {
            client.from("users")
                .select() {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            client.from("users")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }
}