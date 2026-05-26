package com.campusfix.feature.auth.google

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInHelper(context: Context) {

    private val client: GoogleSignInClient

    init {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        client = GoogleSignIn.getClient(context, options)
    }

    fun signInIntent(): Intent = client.signInIntent

    fun extractIdToken(data: Intent?): String? = try {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
            .idToken
    } catch (e: ApiException) {
        null
    }

    companion object {
        // TODO: reemplazar por el Web Client ID real del proyecto Firebase
        private const val WEB_CLIENT_ID = "408094579154:android:cb40cac969209332ed100b.apps.googleusercontent.com"
    }
}

@Composable
fun rememberGoogleSignIn(context: Context): GoogleSignInHelper =
    remember { GoogleSignInHelper(context) }
