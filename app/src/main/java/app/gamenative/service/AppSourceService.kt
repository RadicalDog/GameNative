package app.gamenative.service

import app.gamenative.data.LibraryItem
import app.gamenative.enums.Source
import app.gamenative.service.appsource.AppSourceInterface
import app.gamenative.service.appsource.ManualSource
import app.gamenative.service.appsource.SteamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppSourceService {
    fun getAppSources(): Map<Source, AppSourceInterface> {
        val appSources: Map<Source, AppSourceInterface> = mapOf(
            Source.STEAM to SteamSource,
            Source.MANUAL to ManualSource,
        )
        return appSources
    }

    fun getSourceClass(source: Source): AppSourceInterface {
        val appSources = getAppSources()
        if (appSources.containsKey(source)) {
            return appSources[source]!!
        } else {
            Timber.e("Source $source not configured")
            return appSources[Source.STEAM]!!
        }
    }

    fun getUniqueId(source: Source, appId: Int): String {
        return source.name+":"+appId
    }

    fun getApp(source: Source, appId: Int): LibraryItem {
        val item = runBlocking (Dispatchers.IO) { DaoService.db.appDao().findApp(appId, source) }
        if (item == null) {
            Timber.e("Item ${appId} in ${source.name} not found!")
            // Rather than null, have one that can show itself as an issue
            return LibraryItem(
                name = "Error",
                source = Source.STEAM,
                appId = 0
            )
        }
        return item
    }

    fun timestampToHumanReadable(lastSyncTime: Long?): String {
        if (lastSyncTime != null && lastSyncTime > 0) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            val timeStr = formatter.format(Instant.ofEpochMilli(lastSyncTime))
            return timeStr
        }
        return "None"
    }
}
