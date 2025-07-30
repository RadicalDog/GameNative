package app.gamenative.service.appsource

import app.gamenative.enums.Source

interface AppSourceInterface {
    val source: Source
    val sourceName: String // Human readable name
    val lastSync: Int
    val iconUrl: String

    // Get data from source and add
    fun syncSource () {
        if (isReadyToSync()) {
            syncAppsToDAO()
        }
    }
    // This is also useful info for the UI
    fun isReadyToSync(): Boolean {
        return false
    }

    // Add data
    fun syncAppsToDAO ()

    // Install and run
    fun preLaunchFunctions () {}

    // Get info
    fun getUsername(): String {
        return ""
    }
    fun getConnectedText(): String {
        return "Not connected"
    }
    fun getLastSyncTime(): Long? {
        return null
    }
    var sourceStatusText: String?
}
