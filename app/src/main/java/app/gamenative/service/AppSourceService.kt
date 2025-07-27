package app.gamenative.service

import app.gamenative.enums.Source
import app.gamenative.service.appsource.AppSourceInterface
import app.gamenative.service.appsource.SteamSource
import timber.log.Timber

object AppSourceService {
    val appSources: Map<Source, AppSourceInterface> = mapOf(
    //    Source.CUSTOM to ...(),
        Source.STEAM to SteamSource(),
    )

    fun getSource(source: Source): AppSourceInterface {
        if (appSources.containsKey(source)) {
            return appSources[source]!!
        } else {
            Timber.e("Source $source not configured")
            return appSources[Source.STEAM]!!
        }
    }
}
