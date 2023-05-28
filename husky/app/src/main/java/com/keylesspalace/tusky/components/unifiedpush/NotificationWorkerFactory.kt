package com.keylesspalace.tusky.components.unifiedpush

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class NotificationWorkerFactory(
    private val notificationsFetcher: NotificationFetcher
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        if (workerClassName == NotificationWorker::class.java.name) {
            return NotificationWorker(appContext, workerParameters, notificationsFetcher)
        }
        return null
    }
}
