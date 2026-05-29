package com.campusfix.feature.auth.google

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * HU01 - Ayudante para el inicio de sesion con Google.
 *
 * IMPORTANTE: reemplaza WEB_CLIENT_ID por el "Web client (auto created by
 * Google Service)" que aparece en la consola de Firebase >
 * Authentication > Sign-in method > Google, o en google-services.json
 * (campo oauth_client con client_type 3).
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Abre el selector de cuentas de Google y devuelve el idToken.
     * Es una funcion suspendida: se llama desde una corrutina.
     *
     * @param filterByAuthorized si es true, solo muestra cuentas ya usadas
     *        en la app (para re-login silencioso). false muestra todas.
     */
    suspend fun getIdToken(filterByAuthorized: Boolean = false): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            // Verificamos que la credencial sea efectivamente de Google
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        } catch (e: GetCredentialException) {
            // El usuario cancelo, no hay cuentas, o no hubo red
            null
        } catch (e: GoogleIdTokenParsingException) {
            null
        }
    }

    companion object {
        // TODO: reemplazar por el Web Client ID real del proyecto Firebase
        private const val WEB_CLIENT_ID = "408094579154-dnvv6jbrcl8b3shq74aljqfi44b4tvkq.apps.googleusercontent.com"
    }
}

@Composable
fun rememberGoogleSignIn(context: Context): GoogleSignInHelper =
    remember { GoogleSignInHelper(context) }
