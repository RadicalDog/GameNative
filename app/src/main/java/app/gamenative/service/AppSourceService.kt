package app.gamenative.service

import app.gamenative.data.AppSource

object AppSourceService {
    fun getAppSources (): List<AppSource> {
        val steam = AppSource(
            name = "Steam"
        )
        val epic = AppSource(
            name = "Epic"
        )
        val sourceList = mutableListOf(steam, epic)
        return sourceList
    }
}
