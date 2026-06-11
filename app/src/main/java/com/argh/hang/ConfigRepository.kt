package com.argh.hang

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "protection_config")

data class ProtectionConfig(
    val protectedPackages: Set<String>,
    val protectedActions: Set<ProtectedAction>,
    val passage: String,
    val caseSensitive: Boolean,
)

class ConfigRepository(private val context: Context) {

    companion object {
        private val KEY_PACKAGES = stringSetPreferencesKey("protected_packages")
        private val KEY_ACTIONS = stringSetPreferencesKey("protected_actions")
        private val KEY_PASSAGE = stringPreferencesKey("passage")
        private val KEY_CASE_SENSITIVE = booleanPreferencesKey("case_sensitive")

        const val DEFAULT_PASSAGE =
            "I am making this decision deliberately and understand the consequences " +
                "of modifying this protected application."
    }

    /** Synchronous snapshot for use from the accessibility service hot path. */
    fun snapshot(): ProtectionConfig = runBlocking {
        val prefs = context.dataStore.data.first()
        ProtectionConfig(
            protectedPackages = prefs[KEY_PACKAGES] ?: emptySet(),
            protectedActions = (prefs[KEY_ACTIONS] ?: ProtectedAction.entries.map { it.name }.toSet())
                .mapNotNull { ProtectedAction.fromName(it) }
                .toSet(),
            passage = prefs[KEY_PASSAGE] ?: DEFAULT_PASSAGE,
            caseSensitive = prefs[KEY_CASE_SENSITIVE] ?: true,
        )
    }

    suspend fun setProtectedPackages(packages: Set<String>) {
        context.dataStore.edit { it[KEY_PACKAGES] = packages }
    }

    suspend fun setProtectedActions(actions: Set<ProtectedAction>) {
        context.dataStore.edit { prefs -> prefs[KEY_ACTIONS] = actions.map { it.name }.toSet() }
    }

    suspend fun setPassage(passage: String) {
        context.dataStore.edit { it[KEY_PASSAGE] = passage }
    }

    suspend fun setCaseSensitive(caseSensitive: Boolean) {
        context.dataStore.edit { it[KEY_CASE_SENSITIVE] = caseSensitive }
    }
}
