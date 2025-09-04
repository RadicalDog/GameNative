package app.gamenative.service.appsource

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import app.gamenative.data.LibraryItem
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService
import app.gamenative.service.DaoService
import timber.log.Timber

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

    private fun folderNameToId (folderName: String): Int {
        /* Just needs to be unique, but it is also helpful to generate this way in case someone goes
        back and forwards trying to select one file so we don't end up with lots of pointless DAO entries */
        return AppSourceService.createAppId(folderName.hashCode(), source)
    }

    suspend fun addManualApp (fullFilePath: String) {
        val workingDirectory = fullFilePath.split('/').dropLast(1).joinToString("/")
        val pathToExe = fullFilePath.split('/').last()

        // User can change it, but nice to have a guess
        val guessGameName = fullFilePath.split('/').last().replace(".exe", "")

        // For now, just a frame
        val lib = LibraryItem(
            name = guessGameName,
            appId = folderNameToId(fullFilePath),
            source = Source.MANUAL,
            iconHash = "",
            isShared = false,
            pathToExe = pathToExe,
            workingDirectory = workingDirectory,
            downloadProgress = 1f,
            isInstalled = true,
        )
        DaoService.db.appDao().insert(lib)
        Timber.d("Added template manual app in $workingDirectory at $pathToExe")
    }
}
