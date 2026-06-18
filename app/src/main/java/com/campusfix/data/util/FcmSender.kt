package com.campusfix.data.util

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilidad para enviar notificaciones push usando la API HTTP v1 de FCM.
 * Lee la configuración desde un archivo local para evitar exponer secretos en Git.
 */
@Singleton
class FcmSender @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")

    /** 
     * Envía una notificación a un token específico.
     * El JSON de la cuenta de servicio se lee desde assets/fcm_config.json
     */
    suspend fun sendNotification(token: String, title: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isBlank()) return@runCatching

            val accessToken = getAccessToken()
            val url = URL("https://fcm.googleapis.com/v1/projects/campusfix-fcf01/messages:send")
            val conn = url.openConnection() as HttpURLConnection
            
            conn.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer ${accessToken}")
                setRequestProperty("Content-Type", "application/json; UTF-8")
            }

            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", token)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                    put("data", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                })
            }

            conn.outputStream.use { it.write(message.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("FCM Error: $responseCode - $error")
            }
        }
    }

    private fun getAccessToken(): String {
        val stream = context.assets.open("fcm_config.json")
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(SCOPES)
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }
}
