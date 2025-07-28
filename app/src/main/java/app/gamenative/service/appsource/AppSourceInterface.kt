package app.gamenative.service.appsource

import app.gamenative.enums.Source

interface AppSourceInterface {
    val source: Source
    val sourceName: String // Human readable name
    val lastSync: Int
    val iconUrl: String

    // Get data from source and add
    fun syncSource () {
        syncAppsToDAO()
    }

    // Add data
    fun syncAppsToDAO ()

    // Install and run
    fun preLaunchFunctions () {}
}
