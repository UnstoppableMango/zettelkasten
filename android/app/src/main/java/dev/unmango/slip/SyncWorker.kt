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
            // The reason is on LastError, which the capture screen reads.
            //
            // The binding says nothing about whether a failure is worth trying
            // again, and most are: a dropped connection, a captive portal, a
            // remote that moved. A rejected token is not, and retrying that
            // one forever spends battery on a request that cannot succeed and
            // that nobody is watching. Giving up after a few attempts leaves
            // the notes pending and the reason on screen, and the next capture
            // enqueues a fresh sync anyway.
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val NAME = "sync"

        /** Roughly half an hour of exponential backoff before giving up. */
        private const val MAX_ATTEMPTS = 5

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
