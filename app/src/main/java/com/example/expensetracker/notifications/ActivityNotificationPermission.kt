package com.example.expensetracker.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.domain.notifications.NotificationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Дозвіл на сповіщення, прив’язаний до Activity. `registerForActivityResult` потрібно викликати до `STARTED`,
 * тому об’єкт створюється в `onCreate`.
 *
 * Логіка [request]:
 *  - вже дозволено → нічого;
 *  - Android 13+ і діалог ще можна показати → системний діалог `POST_NOTIFICATIONS`;
 *  - інакше (відмовлено «назавжди», або Android < 13, де сповіщення вимкнено вручну) → налаштування сповіщень застосунку.
 */
class ActivityNotificationPermission(private val activity: ComponentActivity) : NotificationPermission {
    private val state = MutableStateFlow(isEnabled())
    override val granted: StateFlow<Boolean> = state

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh()
    }

    override fun request() {
        refresh()
        if (state.value) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && canShowPermissionDialog()) {
            prefs().edit().putBoolean(KEY_ASKED, true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSystemSettings()
        }
    }

    /** Оновити стан (наприклад, після повернення з системних налаштувань). */
    fun refresh() {
        state.value = isEnabled()
    }

    /**
     * `shouldShowRequestPermissionRationale` повертає `false` і до першого запиту, і після «Більше не питати»,
     * тому «запитували раніше» запам’ятовується окремо.
     */
    private fun canShowPermissionDialog(): Boolean =
        !prefs().getBoolean(KEY_ASKED, false) ||
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        activity.startActivity(intent)
    }

    private fun isEnabled(): Boolean = NotificationManagerCompat.from(activity).areNotificationsEnabled()

    private fun prefs() = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS = "notification_permission"
        const val KEY_ASKED = "asked"
    }
}
