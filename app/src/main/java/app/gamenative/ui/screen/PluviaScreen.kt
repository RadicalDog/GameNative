package app.gamenative.ui.screen

/**
 * Destinations for top level screens, excluding home screen destinations.
 */
sealed class PluviaScreen(val route: String) {
    data object LoginUser : PluviaScreen("login")
    data object Library : PluviaScreen("library")
    data object XServer : PluviaScreen("xserver")
    data object Settings : PluviaScreen("settings")
    data object Overview : PluviaScreen("overview")
    data object AddGame : PluviaScreen("addgame")
    data object Chat : PluviaScreen("chat/{id}") {
        fun route(id: Long) = "chat/$id"
        const val ARG_ID = "id"
    }
}
