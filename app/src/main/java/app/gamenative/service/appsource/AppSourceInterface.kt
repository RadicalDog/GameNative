package app.gamenative.service.appsource

import androidx.compose.runtime.MutableState
import android.net.Uri
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService

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
    fun setLastSyncTime(timestamp: Long) {
        lastSyncTimeHumanReadable.value = AppSourceService.timestampToHumanReadable(timestamp)
    }
    fun getLastSyncTime(): Long? {
        return null
    }
    val sourceMostRecentStatusText: MutableState<String>
    val lastSyncTimeHumanReadable: MutableState<String>

    // App specific questions
    fun isValidToDownload (appId: Int): Boolean
    fun getStoreLink (appId: Int): Uri {
        return Uri.EMPTY
    }
    fun getHeroUrl(appId: Int): String? {
        return null
    }
}
