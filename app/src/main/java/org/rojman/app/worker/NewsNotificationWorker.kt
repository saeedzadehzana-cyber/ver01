package org.rojman.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.rojman.app.MainActivity
import org.rojman.app.R
import org.rojman.app.data.AppRepository

class NewsNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val repository = AppRepository.get(applicationContext)
            val latest = repository.getLatestPosts().firstOrNull() ?: return Result.success()
            val seenId = repository.getLatestSeenPostId()
            if (seenId == null) {
                repository.storeLatestSeenPostId(latest.id)
                return Result.success()
            }
            if (latest.id != seenId) {
                showNotification(latest.title)
                repository.storeLatestSeenPostId(latest.id)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun showNotification(title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rojman News", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("خبر جدید در روژمان")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }

    companion object {
        const val WORK_NAME = "rojman_news_worker"
        private const val CHANNEL_ID = "rojman_news"
    }
}
