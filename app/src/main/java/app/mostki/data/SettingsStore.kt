package app.mostki.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The evening reminders: a paper boat waiting, a bridge starting to creak. */
    val nudges: Boolean = true,
    /** Sorties skipped with "another one", so the next task differs. */
    val skips: Int = 0,
    /** Which reminders were already sent, so each is sent once. */
    val told: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        nudges = this[NUDGES] ?: true,
        skips = this[SKIPS] ?: 0,
        told = this[TOLD] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[NUDGES] = s.nudges
            it[SKIPS] = s.skips
            it[TOLD] = s.told
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NUDGES = booleanPreferencesKey("nudges")
        val SKIPS = intPreferencesKey("skips")
        val TOLD = stringSetPreferencesKey("told")
    }
}
