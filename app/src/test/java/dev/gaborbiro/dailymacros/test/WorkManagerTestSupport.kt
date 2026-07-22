package dev.gaborbiro.dailymacros.test

import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper

/** Resets WorkManager between tests with synchronous no-op workers (avoids Hilt/Room in unit tests). */
object WorkManagerTestSupport {

    private class NoOpWorker(
        context: Context,
        params: WorkerParameters,
    ) : Worker(context, params) {
        override fun doWork(): ListenableWorker.Result = ListenableWorker.Result.success()
    }

    private val testConfiguration: Configuration = Configuration.Builder()
        .setExecutor(SynchronousExecutor())
        .setTaskExecutor(SynchronousExecutor())
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = NoOpWorker(appContext, workerParameters)
            },
        )
        .build()

    fun setUp(context: Context) {
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
        WorkManagerTestInitHelper.initializeTestWorkManager(context, testConfiguration)
    }

    fun tearDown(context: Context) {
        if (WorkManager.isInitialized()) {
            WorkManager.getInstance(context).cancelAllWork()
        }
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
    }
}
