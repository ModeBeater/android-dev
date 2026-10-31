package kz.kbtu.releaseflow.data.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kz.kbtu.releaseflow.MainActivity
import kz.kbtu.releaseflow.R

/** The one notification channel of the app and a helper that respects the runtime permission. */
object Notifications {

    const val CHANNEL_STORE = "store_updates"
    private const val SUMMARY_ID = 1
    private const val APP_ID_BASE = 1000

    /** Channels must exist before the first notification; creating one twice is a no-op. */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_STORE, "Store updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A new version of a tracked app appeared on the App Store"
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the app may post notifications (permission on 13+, channel/app not muted). */
    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** "Signal 7.60.0 is live on the App Store" — one notification per app, replaced on the next change. */
    fun newStoreVersion(context: Context, appId: Int, appName: String, version: String, previous: String) {
        post(
            context,
            id = APP_ID_BASE + appId,
            title = "$appName $version is live on the App Store",
            text = "The store moved from $previous to $version. Compare it with what ReleaseFlow tracks.",
        )
    }

    /** Summary after a manual sync, so the demo always shows something. */
    fun syncSummary(context: Context, summary: String) {
        post(context, id = SUMMARY_ID, title = "Store sync finished", text = summary)
    }

    @SuppressLint("MissingPermission")   // checked in canNotify()
    private fun post(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_STORE)
            .setSmallIcon(R.drawable.ic_stat_release)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
