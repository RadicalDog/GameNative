package app.gamenative.service

import app.gamenative.enums.Source
import app.gamenative.service.appsource.AppSourceInterface
import app.gamenative.service.appsource.SteamSource
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppSourceService {
    val appSources: Map<Source, AppSourceInterface> = mapOf(
    //    Source.CUSTOM to ...(),
        Source.STEAM to SteamSource,
    )

    fun getSourceClass(source: Source): AppSourceInterface {
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

    fun timestampToHumanReadable(lastSyncTime: Long?): String {
        if (lastSyncTime != null && lastSyncTime > 0) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            val timeStr = formatter.format(Instant.ofEpochMilli(lastSyncTime))
            return timeStr
        }
        return "None"
    }
}
