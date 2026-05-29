package com.campusfix.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.R
import com.campusfix.feature.auth.google.rememberGoogleSignIn
import kotlinx.coroutines.launch

/** HU01 - Pantalla de inicio de sesion (email/contrasena + Google). */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Configuracion de Google Sign-In con Credential Manager
    // (ver feature/auth/google/GoogleSignIn.kt)
    val googleClient = rememberGoogleSignIn(context)

    fun onGoogleClick() {
        scope.launch {
            val idToken = googleClient.getIdToken()
            if (idToken != null) viewModel.loginWithGoogle(idToken)
            else snackbar.showSnackbar("No se pudo obtener la cuenta de Google")
        }
    }

    LaunchedEffect(state.success) { if (state.success) onLoggedIn() }
    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.info?.let { snackbar.showSnackbar(it) }
        viewModel.consumeMessages()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CampusFix", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Soporte tecnico universitario",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo institucional") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contrasena") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = { viewModel.resetPassword(email) },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Olvide mi contrasena") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.height(20.dp))
                else Text("Iniciar sesion")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onGoogleClick() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continuar con Google") }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoToRegister) {
                Text("No tienes cuenta? Registrate")
            }
        }
    }
}
