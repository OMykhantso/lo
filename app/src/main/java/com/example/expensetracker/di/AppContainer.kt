package com.example.expensetracker.di

import android.content.Context
import com.example.expensetracker.data.local.db.AppDatabase
import com.example.expensetracker.data.repository.RoomExpenseRepository
import com.example.expensetracker.data.secure.EncryptedPrefsKeyValueStore
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import java.time.Clock

/**
 * Композиційний корінь застосунку: живе стільки ж, скільки процес
 * (див. [com.example.expensetracker.ExpenseTrackerApp]). Важкі об’єкти створюються ліниво.
 */
class AppContainer(private val context: Context) : AppDependencies {
    override val clock: Clock = Clock.systemDefaultZone()

    private val database: AppDatabase by lazy { AppDatabase.create(context) }

    override val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }

    // Ініціалізація EncryptedSharedPreferences звертається до Android Keystore, тому вона відкладена до першого PIN-запиту.
    override val pinAuthenticator: PinAuthenticator by lazy {
        PinAuthenticator(EncryptedPrefsKeyValueStore(context), PinHasher(), clock)
    }

    override val appLock: AppLock by lazy { AppLock(pinAuthenticator, clock) }

    override val demoDataSeeder: DemoDataSeeder by lazy { DemoDataSeeder(expenseRepository, clock) }
}
