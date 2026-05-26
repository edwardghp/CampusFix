package com.campusfix.data.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CampusFixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "CampusFix"
        val body = message.notification?.body ?: "Tienes una nueva notificacion"
        NotificationHelper.show(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        // TODO (Sprint 2): guardar el token FCM en el perfil del usuario (Firestore)
        // para poder enviarle notificaciones dirigidas.
    }
}
