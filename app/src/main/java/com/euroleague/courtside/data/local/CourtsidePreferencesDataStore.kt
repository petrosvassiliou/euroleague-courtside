package com.euroleague.courtside.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.euroleague.courtside.i18n.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "euroleague_courtside_prefs"
)

data class UserPreferencesState(
    val primaryTeamId: String = "MUN",
    val favoriteTeamIds: Set<String> = setOf("MUN", "PAN"),
    val tipOffAlarmEnabled: Boolean = true,
    val halftimeFinalScoreEnabled: Boolean = true,
    val closeGameAlertEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.DE
)

/**
 * Persistiert das gewählte "My Team" (`primaryTeamId`), Favoriten-Teams, die gewählte Sprache
 * (`EN`, `DE`, `FR`, `IT`, `ES`, `EL`) sowie die Benachrichtigungs-Einstellungen dauerhaft über
 * App-Neustarts hinweg (sowohl synchron in `SharedPreferences` für sofortigen Cold-Boot ohne Flackern
 * als auch reaktiv in Jetpack DataStore).
 */
@Singleton
class CourtsidePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val QUICK_BOOT_PREFS = "courtside_quick_boot_prefs"
        private const val KEY_QUICK_TEAM_ID = "primary_team_id"
        private const val KEY_QUICK_FAV_IDS = "favorite_team_ids"
        private const val KEY_QUICK_LANG = "app_language_code"
    }

    private val quickBootPrefs by lazy {
        context.getSharedPreferences(QUICK_BOOT_PREFS, Context.MODE_PRIVATE)
    }

    private object Keys {
        val PRIMARY_TEAM_ID = stringPreferencesKey("primary_team_id")
        val FAVORITE_TEAM_IDS = stringSetPreferencesKey("favorite_team_ids")
        val TIP_OFF_ALARM = booleanPreferencesKey("tip_off_alarm_enabled")
        val HALFTIME_FINAL_SCORE = booleanPreferencesKey("halftime_final_score_enabled")
        val CLOSE_GAME_ALERT = booleanPreferencesKey("close_game_alert_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language_code")
    }

    fun getSavedPrimaryTeamImmediate(): String? =
        quickBootPrefs.getString(KEY_QUICK_TEAM_ID, null)?.takeIf { it.isNotBlank() }

    fun getSavedFavoriteTeamsImmediate(): Set<String>? =
        quickBootPrefs.getStringSet(KEY_QUICK_FAV_IDS, null)?.toSet()

    fun getSavedLanguageImmediate(): AppLanguage? =
        quickBootPrefs.getString(KEY_QUICK_LANG, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { AppLanguage.fromCode(it) }

    /**
     * Speichert das gewählte "My Team" sofort synchron auf dem Gerät, damit die Auswahl selbst dann
     * erhalten bleibt, wenn die App unmittelbar nach dem Antippen geschlossen oder beendet wird.
     */
    fun savePrimaryTeamImmediate(teamId: String) {
        if (teamId.isBlank()) return
        val existingFavs = getSavedFavoriteTeamsImmediate() ?: setOf("MUN", "PAN")
        val updatedFavs = (existingFavs + teamId).toSet()
        quickBootPrefs.edit()
            .putString(KEY_QUICK_TEAM_ID, teamId)
            .putStringSet(KEY_QUICK_FAV_IDS, updatedFavs)
            .commit()
    }

    fun saveLanguageImmediate(language: AppLanguage) {
        quickBootPrefs.edit()
            .putString(KEY_QUICK_LANG, language.code)
            .commit()
    }

    val userPreferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val resolvedTeamId = prefs[Keys.PRIMARY_TEAM_ID]
            ?: getSavedPrimaryTeamImmediate()
            ?: "MUN"
        val resolvedFavs = (prefs[Keys.FAVORITE_TEAM_IDS]
            ?: getSavedFavoriteTeamsImmediate()
            ?: setOf(resolvedTeamId)) + resolvedTeamId
        val resolvedLangCode = prefs[Keys.APP_LANGUAGE]
            ?: quickBootPrefs.getString(KEY_QUICK_LANG, null)

        // Synchronisiere DataStore-Werte in den Quick-Boot-Speicher für den nächsten Kaltstart
        if (prefs[Keys.PRIMARY_TEAM_ID] != null && getSavedPrimaryTeamImmediate() == null) {
            quickBootPrefs.edit()
                .putString(KEY_QUICK_TEAM_ID, resolvedTeamId)
                .putStringSet(KEY_QUICK_FAV_IDS, resolvedFavs)
                .apply()
        }

        UserPreferencesState(
            primaryTeamId = resolvedTeamId,
            favoriteTeamIds = resolvedFavs,
            tipOffAlarmEnabled = prefs[Keys.TIP_OFF_ALARM] ?: true,
            halftimeFinalScoreEnabled = prefs[Keys.HALFTIME_FINAL_SCORE] ?: true,
            closeGameAlertEnabled = prefs[Keys.CLOSE_GAME_ALERT] ?: true,
            language = AppLanguage.fromCode(resolvedLangCode)
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        saveLanguageImmediate(language)
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.code
        }
    }

    suspend fun setPrimaryTeam(teamId: String) {
        savePrimaryTeamImmediate(teamId)
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIMARY_TEAM_ID] = teamId
            val currentFavs = prefs[Keys.FAVORITE_TEAM_IDS] ?: setOf("MUN", "PAN")
            prefs[Keys.FAVORITE_TEAM_IDS] = currentFavs + teamId
        }
    }

    suspend fun toggleFavoriteTeam(teamId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_TEAM_IDS] ?: setOf("MUN", "PAN")
            val updated = if (current.contains(teamId) && current.size > 1) {
                current - teamId
            } else {
                current + teamId
            }
            prefs[Keys.FAVORITE_TEAM_IDS] = updated
            val nextPrimary = if (!updated.contains(prefs[Keys.PRIMARY_TEAM_ID])) {
                updated.first()
            } else {
                prefs[Keys.PRIMARY_TEAM_ID] ?: updated.first()
            }
            prefs[Keys.PRIMARY_TEAM_ID] = nextPrimary
            quickBootPrefs.edit()
                .putString(KEY_QUICK_TEAM_ID, nextPrimary)
                .putStringSet(KEY_QUICK_FAV_IDS, updated)
                .apply()
        }
    }

    suspend fun updateNotificationChannels(
        tipOff: Boolean,
        halftimeFinal: Boolean,
        closeGame: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIP_OFF_ALARM] = tipOff
            prefs[Keys.HALFTIME_FINAL_SCORE] = halftimeFinal
            prefs[Keys.CLOSE_GAME_ALERT] = closeGame
        }
    }
}
