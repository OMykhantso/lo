package com.example.expensetracker.di

import android.content.Context
import com.example.expensetracker.data.local.db.AppDatabase
import com.example.expensetracker.data.prefs.SharedPrefsSettingsRepository
import com.example.expensetracker.data.remote.NbuApiFactory
import com.example.expensetracker.data.remote.NbuRatesRemoteDataSource
import com.example.expensetracker.data.repository.OfflineFirstRatesRepository
import com.example.expensetracker.data.repository.RoomExpenseRepository
import com.example.expensetracker.data.repository.RoomRatesLocalDataSource
import com.example.expensetracker.data.secure.EncryptedPrefsKeyValueStore
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import com.example.expensetracker.domain.usecase.ObserveBalance
import com.example.expensetracker.domain.usecase.ObserveCategoryBreakdown
import com.example.expensetracker.domain.usecase.RatesSync
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Композиційний корінь застосунку: живе стільки ж, скільки процес
 * (див. [com.example.expensetracker.ExpenseTrackerApp]). Важкі об’єкти створюються ліниво.
 */
class AppContainer(private val context: Context) : AppDependencies {
    override val clock: Clock = Clock.systemDefaultZone()

    /** Скоуп, що переживає екрани: фонові оновлення курсів не скасовуються разом із ViewModel. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy { AppDatabase.create(context) }

    override val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }

    override val settingsRepository: SettingsRepository by lazy { SharedPrefsSettingsRepository(context) }

    override val ratesRepository: RatesRepository by lazy {
        OfflineFirstRatesRepository(
            local = RoomRatesLocalDataSource(database.ratesDao()),
            remote = NbuRatesRemoteDataSource(NbuApiFactory.create()),
            clock = clock,
        )
    }

    override val ratesSync: RatesSync by lazy { RatesSync(ratesRepository, appScope, clock) }

    override val observeBalance: ObserveBalance by lazy {
        ObserveBalance(expenseRepository, settingsRepository, ratesRepository, clock)
    }

    override val observeCategoryBreakdown: ObserveCategoryBreakdown by lazy {
        ObserveCategoryBreakdown(expenseRepository, settingsRepository, ratesRepository, clock)
    }

    // Ініціалізація EncryptedSharedPreferences звертається до Android Keystore, тому вона відкладена до першого PIN-запиту.
    override val pinAuthenticator: PinAuthenticator by lazy {
        PinAuthenticator(EncryptedPrefsKeyValueStore(context), PinHasher(), clock)
    }

    override val appLock: AppLock by lazy { AppLock(pinAuthenticator, clock) }

    override val demoDataSeeder: DemoDataSeeder by lazy { DemoDataSeeder(expenseRepository, clock) }
}
