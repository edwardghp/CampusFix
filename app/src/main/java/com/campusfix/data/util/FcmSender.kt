package com.campusfix.data.util

import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilidad para enviar notificaciones push usando la API HTTP v1 de FCM.
 * Se ejecuta desde el dispositivo del coordinador (Opción B).
 */
@Singleton
class FcmSender @Inject constructor() {

    // Datos de la cuenta de servicio (JSON proporcionado por el usuario)
    private val serviceAccountJson = """
        {
          "type": "service_account",
          "project_id": "campusfix-fcf01",
          "private_key_id": "d5a762a1485dbab371b8f9a9cef38677271cd1e5",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDDpvEmZ+fxSpRE\nsDsSaGuUUHlRLZeVsTkUwAsF+TicAIP30+695CBFgjCXis0JKYFxgaZSB8+97W2d\nvfez4J8zPbZ+a+K3IqAY8u6ReSm13y08/51EDSyvoJ83lNfboZ7Qoky3EjmWSNSb\nZUzQUrV7FWV9YxZ0I+arwxOOayoHLfo1+rQzO8jC8fLp6fanYTYKHslrBwHErd8I\nK1SDOcrxlY5WidMi+3eFd10MegHzPbl57jEYyynJEvzRiaHUdylEyJm0r1uWnnA/\nYeUice5BTY5p1Q4pmSdXlhuyx7JLOWlFdw4yLGIYwRivbQ8y0i/K2sLLoa0vkF9P\niYoibXObAgMBAAECggEAXUTWLOLXtTVCXURNxMa2kiuSydocKyYi+ftaxew6ylOM\nlbYYV7nKkJgxpexrNfTtWjjeGrjSlc37tXj7/moUOo8u0jGIEJmXDI2yoPLLqYHM\nxQlt5SUKqma9v/dat77iQL8+Jv+vKC9r+vBdn+ntzdEzYoS8DX2X10XVqsz3ahfw\nV+wLdlHsHH+Qtgb8Ui7RlDU+D6g5zMoqAA5ZfLgRhezQMWxSCqaIlstWqz+5xL4N\nrEuCQQ4Ph+09XTZA06gwt4boIKf42okrGC/2uliHL6WVy4zXt7xP+KL5A2xjWRnP\nAfb6SUCDNjOHbgmZe2Z8xEUw6DaKMxnUTiWRwzOWMQKBgQD5D/UzeC9IDE1mZr2x\nGdPkUKdunnBMXDl16l6wkDQ5G4gbe/7UObRKsYOrbLuU7Y81wcipV8QgZMwPbyNc\nhuIccBcVCtlY/TnexmmrDBFbGS9PG656wOEwUoFapBL/VcsdDPfPppjkwAXenpP5\n09lMzAqF8YNn9C5Z5bLn41/R5QKBgQDJGh7YE6YB6h7o3PuUWqx5o0Dk/lMUOMgh\npUtKyex+ZlkL4nytRhAy7grjNbmynnhlta2+lZRt4lyMk1qq5Os7ih0kb35BYNIT\nw5OQJ9polCjBtpZh0Eqw2q1UwtEotktGeGUNzgpEeUWxlqYviOS6VH3DPXfP6Wzf\n/9cxFFnXfwKBgFCex8JXXwa1ZMCG9VREhgBb0zbNdpBhMgBnUytYIm9x6Abthjlw\nTFn5SCPPWJEGrNq71ZZrYIMT3bIJSasDxmFqLy6SinRx06+3DjFeAKg6aMP0s+/s\nS3h23IRYpAWe4daPgg/nX4p8VeoP7tpppudjDOz6loypz+8tmVHSmgu5AoGAOjNk\nD9cH6W/viCdEbDgdyIpV4rA0LRsN2Kb09m0gE6jdRpJC0QcA8yxvXDiVSLuSTqVY\n5lp1/aNQc6LFz8W2yqF7M6tM1/EEe5HTqBnQnQmcDgwg06grpal0Fp7XV7gCax+2\n51rlH9IVOINID2PIBqEjUJj+jt6Yku+BQQQJyFMCgYEA5E5kzIRvGm2CgMTQOObU\nfXmFrQhAEuM+m/xZALi/+WmLqIK9aj3fZZMqEgvgjGWClWE8askKs+Rgxtuoff7A\n7GMildsvW8LMfyGuv0LJD8VbRzRgARlqdwe0ByOsGDce1/tzOA+edZYpsIEl72pW\nK+V4voDHN48tMAjfULk0hTU=\n-----END PRIVATE KEY-----\n",
          "client_email": "firebase-adminsdk-fbsvc@campusfix-fcf01.iam.gserviceaccount.com",
          "token_uri": "https://oauth2.googleapis.com/token"
        }
    """.trimIndent()

    private val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")

    /** Envía una notificación a un token específico. */
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
                    // Datos extra para que la app pueda reaccionar incluso en primer plano
                    put("data", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                })
            }

            conn.outputStream.use { it.write(message.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = conn.errorStream.bufferedReader().use { it.readText() }
                throw Exception("FCM Error: $responseCode - $error")
            }
        }
    }

    private fun getAccessToken(): String {
        val stream = serviceAccountJson.byteInputStream()
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(SCOPES)
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }
}
