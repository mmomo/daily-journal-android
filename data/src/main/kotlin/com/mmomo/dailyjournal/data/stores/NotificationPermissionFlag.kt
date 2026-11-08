package com.mmomo.dailyjournal.data.stores

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/**
 * Whether the Android 13+ notification permission dialog has been shown (`notificationPermissionAsked.v1`).
 * Android doesn't report this itself; it separates "never asked" from "denied" (DATA-R1).
 */
class NotificationPermissionFlag(private val store: DataStore<Preferences>) {
    suspend fun wasAsked(): Boolean =
        JournalPreferences.safeData(store).first()[JournalPreferences.NOTIFICATION_PERMISSION_ASKED] ?: false

    /** Called by the App unit right after the permission dialog closes. */
    suspend fun markAsked() {
        store.edit { it[JournalPreferences.NOTIFICATION_PERMISSION_ASKED] = true }
    }
}
