package app.gamenative.service.appsource

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.net.Uri
import androidx.core.net.toUri
import app.gamenative.Constants
import app.gamenative.PrefManager
import app.gamenative.data.LibraryItem
import app.gamenative.data.SteamApp
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService
import app.gamenative.service.DaoService
import app.gamenative.service.SteamService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber

object SteamSource : AppSourceInterface {
    override val source = Source.STEAM
    override val sourceName = "Steam"
    override val lastSync = 0
    override val iconUrl = "https://store.steampowered.com/favicon.ico"

    // Displays what is happening to user. No problem to update it loads - users love seeing things progress
    override var sourceMostRecentStatusText: MutableState<String> = mutableStateOf("")
    override var lastSyncTimeHumanReadable: MutableState<String> = mutableStateOf("")
    override var connectedText: MutableState<String> = mutableStateOf("Not connected")

    var job: Job = Job()

    init {
        lastSyncTimeHumanReadable.value = AppSourceService.timestampToHumanReadable(getLastSyncTime())
    }

    override fun syncSource() {
        if (isReadyToSync()) {
            // With the way Steam's service specifically syncs, it is simpler to clear the last sync time
            // than try to access the coroutines from here
            sourceMostRecentStatusText.value = "Sync requested"
            setLastSyncTime(0)
        } else {
            sourceMostRecentStatusText.value = "Not able to sync"
        }
    }

    override fun isReadyToSync(): Boolean {
        return (SteamService.isConnected && SteamService.isLoggedIn)
    }
    override fun syncAppsToDAO() {
        job.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            Timber.tag("Sync $sourceName").d("Syncing $source to AppDao")

            val timeStarted = System.currentTimeMillis()
            // Get what's currently in the DB so we don't overwrite
            val existingRows = DaoService.db.appDao().getAllAppsFromSource(source)
            val mapRows: Map<Int, LibraryItem> = existingRows.associateBy{ it.appId }
            Timber.tag("Sync $sourceName").d("Found ${existingRows.count()} existing rows (${System.currentTimeMillis() - timeStarted}ms)")

            ensureActive()
            val steamApps = DaoService.db.steamAppDao().getAllApps()

            if (PrefManager.steamUserAccountId == 0) {
                Timber.tag("Sync $sourceName").e("Cannot sync while Steam ID isn't valid")
                sourceMostRecentStatusText.value = "Cannot sync while Steam ID isn't valid"
                return@launch
            }

            Timber.tag("Sync $sourceName").d("Checking ${steamApps.count()} Steam apps (${System.currentTimeMillis() - timeStarted}ms)")
            sourceMostRecentStatusText.value = "Getting existing Steam apps"
            val latestList = steamAppsToGeneric(steamApps)
            ensureActive()

            // List to populate
            val toBeAdded = mutableListOf<LibraryItem>()
            val toBeUpdated = mutableListOf<LibraryItem>()
            sourceMostRecentStatusText.value = "Comparing new to existing Steam apps"

            latestList.forEach() {
                if (! mapRows.containsKey(it.appId)){
                    // New
                    toBeAdded.add(it)
                } else {
                    ensureActive()
                    // Existing app
                    if (it != mapRows[it.appId]) {
                        // Different, so we update, presumably

                        // Only things that are likely to change in normal usage
                        val existingApp = mapRows.get(it.appId)!!.copy(
                            iconHash = it.iconHash,
                            isShared = it.isShared,
                        )

                        toBeUpdated.add(existingApp)
                    }
                }
            }

            ensureActive()
            DaoService.db.appDao().insert(toBeAdded)
            Timber.tag("Sync $sourceName").d("Inserted ${toBeAdded.count()} apps (${System.currentTimeMillis() - timeStarted}ms)")
            sourceMostRecentStatusText.value = "Inserted ${toBeAdded.count()} apps"

            ensureActive()
            DaoService.db.appDao().update(toBeUpdated)
            Timber.tag("Sync $sourceName").d("Updated ${toBeUpdated.count()} apps (${System.currentTimeMillis() - timeStarted}ms)")
            sourceMostRecentStatusText.value = "Inserted ${toBeAdded.count()} apps, updated ${toBeUpdated.count()} apps"

            delay(5000)

            sourceMostRecentStatusText.value = ""
        }
    }

    fun steamAppsToGeneric(steamApps: List<SteamApp>): List<LibraryItem> {
        if (PrefManager.steamUserAccountId == 0) {
            Timber.tag("Sync $sourceName").e("Cannot save family share status while Steam ID isn't valid")
            return emptyList()
        }
        val libraryItems = mutableListOf<LibraryItem>()
        steamApps.forEach() { steamApp ->
            val lib = LibraryItem(
                name = steamApp.name,
                appId = steamApp.id,
                source = Source.STEAM,
                iconHash = steamApp.clientIconHash,
                isShared = !steamApp.ownerAccountId.contains(PrefManager.steamUserAccountId),
                pathToExe = SteamService.getAppDirName(steamApp),
                workingDirectory = SteamService.getAppDirPath(steamApp.id),
                type = steamApp.type
            )
            libraryItems.add(lib)
        }

        // Can fill store's "reportedInstallSize" later as it's a faff and would slow things down currently
        return libraryItems
    }

    override fun preLaunchFunctions() {
        // Swap steam DLLs
    }

    override fun getUsername(): String {
        return PrefManager.steamUsername
    }

    override fun setConnectedText() {
        if (isReadyToSync()) {
            connectedText.value = "Connected"
        } else if(! SteamService.isConnected) {
            connectedText.value = "Disconnected"
        } else if (! SteamService.isLoggedIn) {
            connectedText.value = "Not logged in"
        } else {
            connectedText.value = "Disconnect unknown"
        }
    }

    override fun setLastSyncTime(timestamp: Long) {
        PrefManager.lastPICSSyncTime = timestamp
        super.setLastSyncTime(timestamp)
    }
    override fun getLastSyncTime(): Long {
        return PrefManager.lastPICSSyncTime
    }

    override fun isValidToDownload(appId: Int): Boolean {
        val appInfo = SteamService.getAppInfoOf(appId)
        if (appInfo == null) {
            return false
        }
        return (appInfo!!.branches.isNotEmpty() && appInfo.depots.isNotEmpty())
    }

    override fun getStoreLink(appId: Int): Uri {
        return (Constants.Library.STORE_URL + appId).toUri()
    }

    override fun getHeroUrl (appId: Int): String? {
        val appInfo = SteamService.getAppInfoOf(appId)
        return appInfo?.getHeroUrl()
    }
}
