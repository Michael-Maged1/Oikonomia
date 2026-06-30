package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oikonomia_settings")

class PreferenceHelper(private val context: Context) {

    companion object {
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_DEFAULT_REMINDER_TIME = stringPreferencesKey("default_reminder_time")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_NOTIFICATION_VOLUME = intPreferencesKey("notification_volume")
        private val KEY_SNOOZE_DURATION = intPreferencesKey("snooze_duration")
    }

    var isFirstLaunch: Boolean
        get() = runBlocking {
            context.dataStore.data.first()[KEY_FIRST_LAUNCH] ?: true
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_FIRST_LAUNCH] = value
            }
            Unit
        }

    var vibrationEnabled: Boolean
        get() = runBlocking {
            context.dataStore.data.first()[KEY_VIBRATION_ENABLED] ?: true
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_VIBRATION_ENABLED] = value
            }
            Unit
        }

    var soundEnabled: Boolean
        get() = runBlocking {
            context.dataStore.data.first()[KEY_SOUND_ENABLED] ?: true
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_SOUND_ENABLED] = value
            }
            Unit
        }

    var notificationVolume: Int
        get() = runBlocking {
            context.dataStore.data.first()[KEY_NOTIFICATION_VOLUME] ?: 100
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_NOTIFICATION_VOLUME] = value
            }
            Unit
        }

    var snoozeDuration: Int
        get() = runBlocking {
            context.dataStore.data.first()[KEY_SNOOZE_DURATION] ?: 10
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_SNOOZE_DURATION] = value
            }
            Unit
        }

    var nickname: String
        get() = runBlocking {
            context.dataStore.data.first()[KEY_NICKNAME] ?: ""
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_NICKNAME] = value
            }
            Unit
        }

    var language: String
        get() = runBlocking {
            context.dataStore.data.first()[KEY_LANGUAGE] ?: "ar"
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_LANGUAGE] = value
            }
            Unit
        }

    var theme: String
        get() = runBlocking {
            context.dataStore.data.first()[KEY_THEME] ?: "dark"
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_THEME] = value
            }
            Unit
        }

    var defaultReminderTime: String
        get() = runBlocking {
            context.dataStore.data.first()[KEY_DEFAULT_REMINDER_TIME] ?: "12:00"
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_DEFAULT_REMINDER_TIME] = value
            }
            Unit
        }
}
