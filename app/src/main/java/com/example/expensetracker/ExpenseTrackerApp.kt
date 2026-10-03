package com.example.expensetracker

import android.app.Application
import com.example.expensetracker.di.AppContainer
import com.example.expensetracker.notifications.NotificationHelper

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        container.scheduleReminderIfEnabled()
    }
}
