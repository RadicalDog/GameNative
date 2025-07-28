package app.gamenative.service.appsource

import app.gamenative.PrefManager
import app.gamenative.data.LibraryItem
import app.gamenative.data.SteamApp
import app.gamenative.enums.Source
import app.gamenative.service.DaoService
import app.gamenative.service.SteamService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object SteamSource : AppSourceInterface {
    override val source = Source.STEAM
    override val sourceName = "Steam"
    override val lastSync = 0
    override val iconUrl = ""

    override fun syncAppsToDAO() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.tag(sourceName).d("Syncing $source to AppDao")

            val timeStarted = System.currentTimeMillis()
            // Get what's currently in the DB so we don't overwrite
            val existingRows = DaoService.db.appDao().getAllAppsFromSource(source)
            val mapRows: Map<Int, LibraryItem> = existingRows.associateBy{ it.appId }
            Timber.d("Found ${existingRows.count()} existing rows (${System.currentTimeMillis() - timeStarted}ms)")

            val steamApps = DaoService.db.steamAppDao().getAllApps()

            if (PrefManager.steamUserAccountId == 0) {
                Timber.tag("Sync").e("Cannot sync while Steam ID isn't valid")
                return@launch
            }

            Timber.d("Checking ${steamApps.count()} Steam apps (${System.currentTimeMillis() - timeStarted}ms)")
            val latestList = steamAppsToGeneric(steamApps)

            // List to populate
            val toBeAdded = mutableListOf<LibraryItem>()
            val toBeUpdated = mutableListOf<LibraryItem>()

            latestList.forEach() {
                if (! mapRows.containsKey(it.appId)){
                    Timber.d(it.name)
                    // New
                    toBeAdded.add(it)
                } else {
                    // Existing app
                    if (it != mapRows[it.appId]) {
                        // Different, so we update, presumably

                        // Only things that are likely to change in normal usage
                        val existingApp = mapRows.get(it.appId)!!.copy(
                            iconHash = it.iconHash,
                            isShared = it.isShared,
                        )

                        toBeUpdated.add(existingApp)

                        Timber.d("Could update ${existingApp!!.name}")
                    }
                }
            }

            DaoService.db.appDao().insert(toBeAdded)
            Timber.d("Inserted ${toBeAdded.count()} apps (${System.currentTimeMillis() - timeStarted}ms)")

            DaoService.db.appDao().update(toBeUpdated)
            Timber.d("Updated ${toBeUpdated.count()} apps (${System.currentTimeMillis() - timeStarted}ms)")
        }
    }

    fun steamAppsToGeneric(steamApps: List<SteamApp>): List<LibraryItem> {
        if (PrefManager.steamUserAccountId == 0) {
            Timber.tag("Sync").e("Cannot save family share status while Steam ID isn't valid")
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
                downloadFolderName = SteamService.getAppDirName(steamApp),
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
}
