package app.gamenative.ui.data

import app.gamenative.enums.AppTheme
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService
import app.gamenative.service.appsource.AppSourceInterface
import app.gamenative.ui.screen.PluviaScreen
import com.materialkolor.PaletteStyle

data class MainState(
    val appTheme: AppTheme = AppTheme.NIGHT,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val resettedScreen: PluviaScreen? = null,
    val currentScreen: PluviaScreen? = null,
    val hasLaunched: Boolean = false,
    val loadingDialogVisible: Boolean = false,
    val loadingDialogProgress: Float = 0F,
    val annoyingDialogShown: Boolean = false,
    val hasCrashedLastStart: Boolean = false,
    val isSteamConnected: Boolean = false,
    val launchedAppId: Int = 0,
    val launchedAppSource: AppSourceInterface = AppSourceService.getSourceClass(Source.STEAM),
    val bootToContainer: Boolean = false,
    val showBootingSplash: Boolean = false,
)
