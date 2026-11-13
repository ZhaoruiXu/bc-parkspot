package ca.bcparkspot.app.data.local

import android.content.Context

/** Small device-only facts that must survive a restart. */
interface DevicePrefs {
    var deletionPendingUid: String?
    var lastServerSnapshotAt: Long?
    var lastSuccessfulSyncAt: Long?
}

class SharedDevicePrefs(context: Context) : DevicePrefs {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var deletionPendingUid: String?
        get() = prefs.getString(KEY_DELETION_PENDING, null)
        set(value) { prefs.edit().putString(KEY_DELETION_PENDING, value).commit() }

    override var lastServerSnapshotAt: Long?
        get() = if (prefs.contains(KEY_LAST_SNAPSHOT)) prefs.getLong(KEY_LAST_SNAPSHOT, 0L) else null
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_LAST_SNAPSHOT) else editor.putLong(KEY_LAST_SNAPSHOT, value)
            editor.apply()
        }

    override var lastSuccessfulSyncAt: Long?
        get() = if (prefs.contains(KEY_LAST_SYNC)) prefs.getLong(KEY_LAST_SYNC, 0L) else null
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_LAST_SYNC) else editor.putLong(KEY_LAST_SYNC, value)
            editor.apply()
        }

    private companion object {
        const val FILE = "device_state"
        const val KEY_DELETION_PENDING = "deletion_pending_uid"
        const val KEY_LAST_SNAPSHOT = "last_server_snapshot_at"
        const val KEY_LAST_SYNC = "last_successful_sync_at"
    }
}
