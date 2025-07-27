package app.gamenative.service.appsource

import app.gamenative.enums.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SteamSource : AppSourceInterface {
    override val source = Source.STEAM
    override val sourceName = "Steam"
    override val lastSync = 0
    override val iconUrl = ""

    override fun syncAppsToDAO() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.tag(sourceName).d("Syncing $source to AppDao")
//            val existingRows = AppSourceService.getAppDao().getAllAppsFromSource(source)

//            Timber.d("Found ${existingRows.count()} existing rows")


        }
    }

    override fun preLaunchFunctions() {
        // Swap steam DLLs
    }
}
