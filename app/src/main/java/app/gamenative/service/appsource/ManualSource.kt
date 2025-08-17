package app.gamenative.service.appsource

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import app.gamenative.enums.Source

object ManualSource : AppSourceInterface {
    override val source: Source = Source.MANUAL
    override val sourceName: String = "Manual"
    override val lastSync: Int = 0
    override val iconUrl: String = "" // Obviously needs updating

    override fun syncAppsToDAO() {
        // Maybe something to scan if files have been deleted since adding?
    }

    override val sourceMostRecentStatusText: MutableState<String> = mutableStateOf("")
    override val lastSyncTimeHumanReadable: MutableState<String> = mutableStateOf("")
    override val connectedText: MutableState<String> = mutableStateOf("Add game files on device to your library")

    override fun isValidToDownload(appId: Int): Boolean {
        return false
    }
}
