package com.example.expensetracker.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.ui.navigation.ADD_EXPENSE_DEEP_LINK

/** Канал і показ сповіщення-нагадування. Тап по сповіщенню відкриває екран додавання витрати (deep link). */
object NotificationHelper {
    const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 2000
    private const val REQUEST_CODE = 2001

    const val TITLE = "Трекер витрат"
    const val TEXT = "Не забудьте зафіксувати сьогоднішні витрати!"

    /** Ідемпотентно: повторне створення каналу з тим самим id нічого не змінює. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** `PendingIntent` на `expensetracker://add` → навігація (`navDeepLink`) відкриває [AddExpenseRoute]. */
    fun addExpensePendingIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ADD_EXPENSE_DEEP_LINK), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** @return `true`, якщо сповіщення показано; `false` — якщо дозволу немає (тоді нічого не робимо). */
    @SuppressLint("MissingPermission") // дозвіл перевіряється нижче
    fun showDailyReminder(context: Context): Boolean {
        if (!canPostNotifications(context)) return false
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(TITLE)
            .setContentText(TEXT)
            .setContentIntent(addExpensePendingIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
