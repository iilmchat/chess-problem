package com.iilmchat.chessproblem.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iilmchat.chessproblem.core.repository.HintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import kotlin.math.min

private val Context.hintsDataStore by preferencesDataStore(name = "hints")

class HintWallet(
    private val context: Context,
) : HintRepository {

    override val balance: Flow<Int> = context.hintsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[KEY_BALANCE] ?: DEFAULT_BALANCE }

    override suspend fun currentBalance(): Int =
        context.hintsDataStore.data.first()[KEY_BALANCE] ?: DEFAULT_BALANCE

    override suspend fun spend(amount: Int): Boolean {
        if (amount <= 0) return true
        var spent = false
        context.hintsDataStore.edit { prefs ->
            val current = prefs[KEY_BALANCE] ?: DEFAULT_BALANCE
            if (current >= amount) {
                prefs[KEY_BALANCE] = current - amount
                spent = true
            }
        }
        return spent
    }

    override suspend fun refillIfNeeded() {
        val today = LocalDate.now().toEpochDay()
        context.hintsDataStore.edit { prefs ->
            val lastRefill = prefs[KEY_LAST_REFILL]
            when {
                lastRefill == null -> {
                    prefs[KEY_BALANCE] = prefs[KEY_BALANCE] ?: DEFAULT_BALANCE
                    prefs[KEY_LAST_REFILL] = today
                }

                lastRefill < today -> {
                    val current = prefs[KEY_BALANCE] ?: DEFAULT_BALANCE
                    prefs[KEY_BALANCE] = min(MAX_BALANCE, current + DAILY_REFILL)
                    prefs[KEY_LAST_REFILL] = today
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_BALANCE = 3
        const val DAILY_REFILL = 3
        const val MAX_BALANCE = 5
        val KEY_BALANCE = intPreferencesKey("balance")
        val KEY_LAST_REFILL = longPreferencesKey("last_refill")
    }
}
