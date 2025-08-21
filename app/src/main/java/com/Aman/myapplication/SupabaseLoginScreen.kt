package com.Aman.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Aman.myapplication.viewmodel.AuthState
import com.Aman.myapplication.viewmodel.ResetPasswordState
import com.Aman.myapplication.viewmodel.SupabaseAuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseLoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: SupabaseAuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var showResetPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val resetPasswordState by authViewModel.resetPasswordState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onLoginSuccess()
            }
            else -> {}
        }
    }

    // Reset password dialog
    if (showResetPassword) {
        AlertDialog(
            onDismissRequest = {
                showResetPassword = false
                authViewModel.clearResetPasswordState()
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (resetPasswordState) {
                        is ResetPasswordState.Loading -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        is ResetPasswordState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Reset link sent! Check your email.",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is ResetPasswordState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                (resetPasswordState as ResetPasswordState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { authViewModel.resetPassword(resetEmail) },
                    enabled = resetEmail.isNotBlank()
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetPassword = false
                    authViewModel.clearResetPasswordState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = if (isRegistering) "Create Account" else "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (isRegistering) "Sign up with Supabase" else "Sign in to continue",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Full Name Field (only for registration)
        if (isRegistering) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Full Name")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Field (only for registration)
        if (isRegistering) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = "Phone")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("+1234567890") }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "👁" else "🙈",
                        fontSize = 18.sp
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val canSubmit = if (isRegistering) {
                        email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank() && phone.isNotBlank()
                    } else {
                        email.isNotBlank() && password.isNotBlank()
                    }

                    if (canSubmit) {
                        if (isRegistering) {
                            authViewModel.signUp(email, password, fullName, phone)
                        } else {
                            authViewModel.signIn(email, password)
                        }
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Forgot Password
        if (!isRegistering) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        resetEmail = email
                        showResetPassword = true
                    }
                ) {
                    Text("Forgot Password?", fontSize = 14.sp)
                }
            }
        }

        // Error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login/Register Button
        val canSubmit = if (isRegistering) {
            email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank() && phone.isNotBlank()
        } else {
            email.isNotBlank() && password.isNotBlank()
        }

        Button(
            onClick = {
                authViewModel.clearError()
                if (canSubmit) {
                    if (isRegistering) {
                        authViewModel.signUp(email, password, fullName, phone)
                    } else {
                        authViewModel.signIn(email, password)
                    }
                }
            },
            enabled = canSubmit && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isRegistering) "Create Account" else "Sign In",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle between login and register
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRegistering) "Already have an account? " else "Don't have an account? ",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = {
                    isRegistering = !isRegistering
                    authViewModel.clearError()
                    // Clear the additional fields when switching modes
                    if (!isRegistering) {
                        fullName = ""
                        phone = ""
                    }
                }
            ) {
                Text(
                    text = if (isRegistering) "Sign In" else "Sign Up",
                    fontSize = 14.sp
                )
            }
        }
    }
}