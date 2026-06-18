package com.campusfix.data.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CampusFixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        // 1. Priorizar datos (Payload personalizado)
        val dataTitle = message.data["title"]
        val dataBody = message.data["body"]
        
        // 2. Notificacion estandar de Firebase
        val notifTitle = message.notification?.title
        val notifBody = message.notification?.body

        val title = dataTitle ?: notifTitle ?: "CampusFix"
        val body = dataBody ?: notifBody ?: "Tienes una nueva actualización"

        NotificationHelper.show(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        // TODO (Sprint 2): guardar el token FCM en el perfil del usuario (Firestore)
        // para poder enviarle notificaciones dirigidas.
    }
}
