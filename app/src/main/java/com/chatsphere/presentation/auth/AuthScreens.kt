package com.chatsphere.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatsphere.R
import com.chatsphere.presentation.theme.ChatSphereTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onAuthenticated: () -> Unit, onRegister: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    LoginContent(
        state = state,
        onEvent = viewModel::onEvent,
        onRegister = onRegister,
        onGoogleSignIn = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        viewModel.onEvent(AuthEvent.LoginWithGoogle(credential.idToken))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )
}

@Composable
fun LoginContent(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onRegister: () -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ChatSphere", style = MaterialTheme.typography.headlineLarge)
        Text("Sign in to continue", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            onClick = { onEvent(AuthEvent.Login) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            if (state.isLoading) CircularProgressIndicator() else Text("Sign in")
        }

        Button(
            onClick = onGoogleSignIn,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Sign in with Google")
        }

        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Create account")
        }
    }
}

@Composable
fun RegisterScreen(onAuthenticated: () -> Unit, onLogin: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }
    
    RegisterContent(
        state = state,
        onEvent = viewModel::onEvent,
        onLogin = onLogin
    )
}

@Composable
fun RegisterContent(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Button(
            onClick = { onEvent(AuthEvent.Register) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            Text("Create account")
        }
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("I already have an account")
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginPreview() {
    ChatSphereTheme {
        Surface {
            LoginContent(
                state = AuthUiState(email = "test@example.com"),
                onEvent = {},
                onRegister = {},
                onGoogleSignIn = {}
            )
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegisterPreview() {
    ChatSphereTheme {
        Surface {
            RegisterContent(
                state = AuthUiState(),
                onEvent = {},
                onLogin = {}
            )
        }
    }
}
