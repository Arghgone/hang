package com.argh.hang

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "protection_config")

enum class EdgeSnap { NONE, LEFT, RIGHT, TOP, BOTTOM }

data class ProtectionConfig(
    val passage: String,
    val caseSensitive: Boolean,
    val authDurationMs: Long,
    // Overlay
    val overlayEnabled: Boolean,
    val overlayImagePath: String?,
    val overlayGridRow: Int,           // 0=top, 1=mid, 2=bottom
    val overlayGridCol: Int,           // 0=left, 1=center, 2=right
    val overlayOffsetXDp: Int,
    val overlayOffsetYDp: Int,
    val overlayWidthDp: Int,           // 24–320 dp
    val overlayOpacity: Float,         // 0.10–1.0
    val overlayVisiblePortrait: Boolean,
    val overlayVisibleLandscape: Boolean,
    val overlayEdgeSnap: EdgeSnap,
)

class ConfigRepository(private val context: Context) {

    companion object {
        private val KEY_PASSAGE = stringPreferencesKey("passage")
        private val KEY_CASE_SENSITIVE = booleanPreferencesKey("case_sensitive")
        private val KEY_AUTH_DURATION_MS = longPreferencesKey("auth_duration_ms")
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_OVERLAY_IMAGE_PATH = stringPreferencesKey("overlay_image_path")
        private val KEY_OVERLAY_GRID_ROW = intPreferencesKey("overlay_grid_row")
        private val KEY_OVERLAY_GRID_COL = intPreferencesKey("overlay_grid_col")
        private val KEY_OVERLAY_OFFSET_X = intPreferencesKey("overlay_offset_x")
        private val KEY_OVERLAY_OFFSET_Y = intPreferencesKey("overlay_offset_y")
        private val KEY_OVERLAY_WIDTH_DP = intPreferencesKey("overlay_width_dp")
        private val KEY_OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        private val KEY_OVERLAY_VISIBLE_PORTRAIT = booleanPreferencesKey("overlay_visible_portrait")
        private val KEY_OVERLAY_VISIBLE_LANDSCAPE = booleanPreferencesKey("overlay_visible_landscape")
        private val KEY_OVERLAY_EDGE_SNAP = stringPreferencesKey("overlay_edge_snap")

        const val DEFAULT_PASSAGE =
            "I am making this decision deliberately and understand the consequences " +
                "of modifying this protected application."
        const val DEFAULT_AUTH_DURATION_MS = 60_000L
    }

    /** Synchronous snapshot for use on service hot paths. */
    fun snapshot(): ProtectionConfig = runBlocking {
        val prefs = context.dataStore.data.first()
        ProtectionConfig(
            passage = prefs[KEY_PASSAGE] ?: DEFAULT_PASSAGE,
            caseSensitive = prefs[KEY_CASE_SENSITIVE] ?: true,
            authDurationMs = prefs[KEY_AUTH_DURATION_MS] ?: DEFAULT_AUTH_DURATION_MS,
            overlayEnabled = prefs[KEY_OVERLAY_ENABLED] ?: false,
            overlayImagePath = prefs[KEY_OVERLAY_IMAGE_PATH],
            overlayGridRow = prefs[KEY_OVERLAY_GRID_ROW] ?: 1,
            overlayGridCol = prefs[KEY_OVERLAY_GRID_COL] ?: 1,
            overlayOffsetXDp = prefs[KEY_OVERLAY_OFFSET_X] ?: 0,
            overlayOffsetYDp = prefs[KEY_OVERLAY_OFFSET_Y] ?: 0,
            overlayWidthDp = prefs[KEY_OVERLAY_WIDTH_DP] ?: 96,
            overlayOpacity = prefs[KEY_OVERLAY_OPACITY] ?: 1.0f,
            overlayVisiblePortrait = prefs[KEY_OVERLAY_VISIBLE_PORTRAIT] ?: true,
            overlayVisibleLandscape = prefs[KEY_OVERLAY_VISIBLE_LANDSCAPE] ?: true,
            overlayEdgeSnap = prefs[KEY_OVERLAY_EDGE_SNAP]
                ?.let { runCatching { EdgeSnap.valueOf(it) }.getOrNull() }
                ?: EdgeSnap.NONE,
        )
    }

    suspend fun setPassage(passage: String) =
        context.dataStore.edit { it[KEY_PASSAGE] = passage }

    suspend fun setCaseSensitive(value: Boolean) =
        context.dataStore.edit { it[KEY_CASE_SENSITIVE] = value }

    suspend fun setAuthDurationMs(ms: Long) =
        context.dataStore.edit { it[KEY_AUTH_DURATION_MS] = ms }

    suspend fun setOverlayEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }

    suspend fun setOverlayImagePath(path: String?) =
        context.dataStore.edit {
            if (path != null) it[KEY_OVERLAY_IMAGE_PATH] = path
            else it.remove(KEY_OVERLAY_IMAGE_PATH)
        }

    suspend fun setOverlayGridPosition(row: Int, col: Int) =
        context.dataStore.edit {
            it[KEY_OVERLAY_GRID_ROW] = row
            it[KEY_OVERLAY_GRID_COL] = col
        }

    suspend fun setOverlayOffset(xDp: Int, yDp: Int) =
        context.dataStore.edit {
            it[KEY_OVERLAY_OFFSET_X] = xDp
            it[KEY_OVERLAY_OFFSET_Y] = yDp
        }

    suspend fun setOverlayWidthDp(dp: Int) =
        context.dataStore.edit { it[KEY_OVERLAY_WIDTH_DP] = dp.coerceIn(24, 320) }

    suspend fun setOverlayOpacity(opacity: Float) =
        context.dataStore.edit { it[KEY_OVERLAY_OPACITY] = opacity.coerceIn(0.10f, 1.0f) }

    suspend fun setOverlayVisibility(portrait: Boolean, landscape: Boolean) =
        context.dataStore.edit {
            it[KEY_OVERLAY_VISIBLE_PORTRAIT] = portrait
            it[KEY_OVERLAY_VISIBLE_LANDSCAPE] = landscape
        }

    suspend fun setOverlayEdgeSnap(snap: EdgeSnap) =
        context.dataStore.edit { it[KEY_OVERLAY_EDGE_SNAP] = snap.name }
}
