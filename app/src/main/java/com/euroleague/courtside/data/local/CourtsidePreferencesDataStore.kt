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
    val primaryTeamId: String = "BAY",
    val favoriteTeamIds: Set<String> = setOf("BAY", "PAO"),
    val tipOffAlarmEnabled: Boolean = true,
    val halftimeFinalScoreEnabled: Boolean = true,
    val closeGameAlertEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.DE
)

/**
 * Persistiert Favoriten-Teams, das primäre Vereins-Theme, die gewählte Sprache (EN, DE, FR, IT, ES, EL)
 * sowie die Benachrichtigungs-Einstellungen reaktiv via Jetpack DataStore.
 */
@Singleton
class CourtsidePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRIMARY_TEAM_ID = stringPreferencesKey("primary_team_id")
        val FAVORITE_TEAM_IDS = stringSetPreferencesKey("favorite_team_ids")
        val TIP_OFF_ALARM = booleanPreferencesKey("tip_off_alarm_enabled")
        val HALFTIME_FINAL_SCORE = booleanPreferencesKey("halftime_final_score_enabled")
        val CLOSE_GAME_ALERT = booleanPreferencesKey("close_game_alert_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language_code")
    }

    val userPreferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            primaryTeamId = prefs[Keys.PRIMARY_TEAM_ID] ?: "BAY",
            favoriteTeamIds = prefs[Keys.FAVORITE_TEAM_IDS] ?: setOf("BAY", "PAO"),
            tipOffAlarmEnabled = prefs[Keys.TIP_OFF_ALARM] ?: true,
            halftimeFinalScoreEnabled = prefs[Keys.HALFTIME_FINAL_SCORE] ?: true,
            closeGameAlertEnabled = prefs[Keys.CLOSE_GAME_ALERT] ?: true,
            language = AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE])
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.code
        }
    }

    suspend fun setPrimaryTeam(teamId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIMARY_TEAM_ID] = teamId
            val currentFavs = prefs[Keys.FAVORITE_TEAM_IDS] ?: setOf("BAY", "PAO")
            prefs[Keys.FAVORITE_TEAM_IDS] = currentFavs + teamId
        }
    }

    suspend fun toggleFavoriteTeam(teamId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_TEAM_IDS] ?: setOf("BAY", "PAO")
            val updated = if (current.contains(teamId) && current.size > 1) {
                current - teamId
            } else {
                current + teamId
            }
            prefs[Keys.FAVORITE_TEAM_IDS] = updated
            if (!updated.contains(prefs[Keys.PRIMARY_TEAM_ID])) {
                prefs[Keys.PRIMARY_TEAM_ID] = updated.first()
            }
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
