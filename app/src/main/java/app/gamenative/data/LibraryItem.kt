package app.gamenative.data

import app.gamenative.Constants

enum class GameSource {
    STEAM,
    // Add other platforms here..
}

/**
 * Data class for the Library list
 */
data class LibraryItem(
    val index: Int = 0,
    val appId: String = "",
    val name: String = "",
    val iconHash: String = "",
    val isShared: Boolean = false,
    val gameSource: GameSource = GameSource.STEAM,
) {
    val clientIconUrl: String
        get() = Constants.Library.ICON_URL + "${gameId}/$iconHash.ico"
    
    /**
     * Helper property to get the game ID as an integer
     * Extracts the numeric part by removing the gameSource prefix
     */
    val gameId: Int
        get() = appId.removePrefix("${gameSource.name}_").toInt()
}
