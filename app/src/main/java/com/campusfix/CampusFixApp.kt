package com.campusfix

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Clase Application. Punto de entrada de Hilt (@HiltAndroidApp) y configuracion
 * de WorkManager para que pueda inyectar dependencias en los Workers.
 */
@HiltAndroidApp
class CampusFixApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
