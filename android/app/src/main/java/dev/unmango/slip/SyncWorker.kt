package dev.unmango.slip

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncWorker publishes whatever is pending. Every sync goes through it, the
 * one the person taps included, so that WorkManager's unique work is the only
 * thing that decides when a sync runs.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Notebook.client(applicationContext).sync()
            Result.success()
        } catch (e: Exception) {
            // The reason is on the client's LastError, which the capture screen
            // reads. Retrying is right for a dropped connection and harmless
            // for a bad token, which will simply keep failing in view.
            Result.retry()
        }
    }

    companion object {
        const val NAME = "sync"

        /**
         * enqueue replaces any sync already queued. A sync publishes everything
         * pending, so the newer request subsumes the older one, and replacing
         * keeps two of them off the same worktree.
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
