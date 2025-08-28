package app.gamenative

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.OrientationEventListener
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import app.gamenative.events.AndroidEvent
import app.gamenative.service.SteamService
import app.gamenative.ui.PluviaMain
import app.gamenative.ui.enums.Orientation
import app.gamenative.utils.AnimatedPngDecoder
import app.gamenative.utils.IconDecoder
import app.gamenative.utils.IntentLaunchManager
import com.posthog.PostHog
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import com.winlator.core.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.EnumSet
import kotlin.math.abs
import okio.Path.Companion.toOkioPath
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private var totalIndex = 0

        private var currentOrientationChangeValue: Int = 0
        private var availableOrientations: EnumSet<Orientation> = EnumSet.of(Orientation.UNSPECIFIED)

        // Store pending launch request to be processed after UI is ready
        @Volatile
        private var pendingLaunchRequest: IntentLaunchManager.LaunchRequest? = null

        // Atomically get and clear the pending launch request
        fun consumePendingLaunchRequest(): IntentLaunchManager.LaunchRequest? {
            synchronized(this) {
                val request = pendingLaunchRequest
                pendingLaunchRequest = null
                return request
            }
        }

        // Atomically set a new pending launch request
        fun setPendingLaunchRequest(request: IntentLaunchManager.LaunchRequest) {
            synchronized(this) {
                pendingLaunchRequest = request
            }
        }
    }

    private val onSetSystemUi: (AndroidEvent.SetSystemUIVisibility) -> Unit = {
        AppUtils.hideSystemUI(this, !it.visible)
    }

    private val onSetAllowedOrientation: (AndroidEvent.SetAllowedOrientation) -> Unit = {
        // Log.d("MainActivity", "Requested allowed orientations of $it")
        availableOrientations = it.orientations
        setOrientationTo(currentOrientationChangeValue, availableOrientations)
    }

    private val onStartOrientator: (AndroidEvent.StartOrientator) -> Unit = {
        // TODO: When rotating the device on login screen:
        //  StrictMode policy violation: android.os.strictmode.LeakedClosableViolation: A resource was acquired at attached stack trace but never released. See java.io.Closeable for information on avoiding resource leaks.
        startOrientator()
    }

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = {
        finishAndRemoveTask()
    }

    private var index = totalIndex++

    // Add a property to keep a reference to the orientation sensor listener
    private var orientationSensorListener: OrientationEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(30, 30, 30)),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        handleLaunchIntent(intent)

        // Prevent device from sleeping while app is open
        AppUtils.keepScreenOn(this)

        // startOrientator() // causes memory leak since activity restarted every orientation change
        PluviaApp.events.on<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        PluviaApp.events.on<AndroidEvent.StartOrientator, Unit>(onStartOrientator)
        PluviaApp.events.on<AndroidEvent.SetAllowedOrientation, Unit>(onSetAllowedOrientation)
        PluviaApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

        setContent {
            var hasNotificationPermission by remember { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            LaunchedEffect(Unit) {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val context = LocalContext.current
            val imageLoader = remember {
                val memoryCache = MemoryCache.Builder(context)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()

                val diskCache = DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .build()

                // val logger = if (BuildConfig.DEBUG) DebugLogger() else null

                ImageLoader.Builder(context)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache(memoryCache)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCache(diskCache)
                    .components {
                        add(IconDecoder.Factory())
                        add(AnimatedPngDecoder.Factory())
                    }
                    // .logger(logger)
                    .build()
            }

            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                PluviaMain()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }
    private fun handleLaunchIntent(intent: Intent) {
        Timber.d("[MainActivity]: handleLaunchIntent called with action=${intent.action}")
        try {
            val launchRequest = IntentLaunchManager.parseLaunchIntent(intent)
            if (launchRequest != null) {
                Timber.d("[MainActivity]: Received external launch intent for app ${launchRequest.appId}")

                // If already logged in, emit event immediately
                // Otherwise store for processing after login
                if (SteamService.isLoggedIn) {
                    Timber.d("[MainActivity]: User already logged in, emitting ExternalGameLaunch event immediately")
                    lifecycleScope.launch {
                        PluviaApp.events.emit(AndroidEvent.ExternalGameLaunch(launchRequest.appId))
                    }

                    // Apply config override if present
                    launchRequest.containerConfig?.let { config ->
                        IntentLaunchManager.applyTemporaryConfigOverride(this, launchRequest.appId, config)
                    }
                } else {
                    // Store the launch request to be processed after login
                    setPendingLaunchRequest(launchRequest)
                    Timber.d("[MainActivity]: User not logged in, stored pending launch request for app ${launchRequest.appId}")
                }
            } else {
                Timber.d("[MainActivity]: parseLaunchIntent returned null")
            }
        } catch (e: Exception) {
            Timber.e(e, "[MainActivity]: Failed to handle launch intent")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        PluviaApp.events.emit(AndroidEvent.ActivityDestroyed)

        PluviaApp.events.off<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        PluviaApp.events.off<AndroidEvent.StartOrientator, Unit>(onStartOrientator)
        PluviaApp.events.off<AndroidEvent.SetAllowedOrientation, Unit>(onSetAllowedOrientation)
        PluviaApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)

        Timber.d(
            "onDestroy - Index: %d, Connected: %b, Logged-In: %b, Changing-Config: %b",
            index,
            SteamService.isConnected,
            SteamService.isLoggedIn,
            isChangingConfigurations,
        )

        if (SteamService.isConnected && !SteamService.isLoggedIn && !isChangingConfigurations) {
            Timber.i("Stopping Steam Service")
            SteamService.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        // disable auto-stop when returning to foreground
        SteamService.autoStopWhenIdle = false
        PostHog.capture(event = "app_foregrounded")
    }

    override fun onPause() {
        PostHog.capture(event = "app_backgrounded")
        super.onPause()
    }

    // Add cleanup when app is backgrounded
    override fun onStop() {
        super.onStop()
        orientationSensorListener?.disable()
        orientationSensorListener = null
        // enable auto-stop behavior if backgrounded
        SteamService.autoStopWhenIdle = true

        // stop SteamService only if no downloads or sync are in progress
        if (!isChangingConfigurations &&
            SteamService.isConnected &&
            !SteamService.hasActiveOperations() &&
            !SteamService.isLoginInProgress
        ) {
            Timber.i("Stopping SteamService - no active operations")
            SteamService.stop()
        }
    }

    // override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    //     // Log.d("MainActivity$index", "onKeyDown($keyCode):\n$event")
    //     if (keyCode == KeyEvent.KEYCODE_BACK) {
    //         PluviaApp.events.emit(AndroidEvent.BackPressed)
    //         return true
    //     }
    //     return super.onKeyDown(keyCode, event)
    // }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Log.d("MainActivity$index", "dispatchKeyEvent(${event.keyCode}):\n$event")

        var eventDispatched = PluviaApp.events.emit(AndroidEvent.KeyEvent(event)) { keyEvent ->
            keyEvent.any { it }
        } == true

        // TODO: Temp'd removed this.
        //  Idealy, compose handles back presses automaticially in which we can override it in certain composables.
        //  Since LibraryScreen uses its own navigation system, this will need to be re-worked accordingly.
//        if (!eventDispatched) {
//            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
//                PluviaApp.events.emit(AndroidEvent.BackPressed)
//                eventDispatched = true
//            }
//        }

        return if (!eventDispatched) super.dispatchKeyEvent(event) else true
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        // Log.d("MainActivity$index", "dispatchGenericMotionEvent(${ev?.deviceId}:${ev?.device?.name}):\n$ev")

        val eventDispatched = PluviaApp.events.emit(AndroidEvent.MotionEvent(ev)) { event ->
            event.any { it }
        } == true

        return if (!eventDispatched) super.dispatchGenericMotionEvent(ev) else true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Log.d("MainActivity", "Requested orientation: $requestedOrientation => ${Orientation.fromActivityInfoValue(requestedOrientation)}")
    }

    private fun startOrientator() {
        // Log.d("MainActivity$index", "Orientator starting up")

        // create and register the orientation listener
        orientationSensorListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                currentOrientationChangeValue = if (orientation != ORIENTATION_UNKNOWN) {
                    orientation
                } else {
                    currentOrientationChangeValue
                }
                setOrientationTo(currentOrientationChangeValue, availableOrientations)
            }
        }

        // enable if possible
        orientationSensorListener?.takeIf { it.canDetectOrientation() }?.enable()
    }

    private fun setOrientationTo(orientation: Int, conformTo: EnumSet<Orientation>) {
        // Log.d("MainActivity$index", "Setting orientation to conform")

        // reverse direction of orientation
        val adjustedOrientation = 360 - orientation

        // if our available orientations are empty then assume unspecified
        val orientations = conformTo.ifEmpty { EnumSet.of(Orientation.UNSPECIFIED) }

        var inRange = orientations
            .filter { it.angleRanges.any { it.contains(adjustedOrientation) } }
            .toTypedArray()

        if (inRange.isEmpty()) {
            // none of the available orientations conform to the reported orientation
            // so set it to the original orientations in preparation for finding the
            // nearest conforming orientation
            inRange = orientations.toTypedArray()
        }

        // find the nearest orientation to the reported
        val distances = orientations.map {
            it to it.angleRanges.minOf { angleRange ->
                angleRange.minOf { angle ->
                    // since 0 can be represented as 360 and vice versa
                    if (adjustedOrientation == 0 || adjustedOrientation == 360) {
                        minOf(abs(angle), abs(angle - 360))
                    } else {
                        abs(angle - adjustedOrientation)
                    }
                }
            }
        }

        val nearest = distances.minBy { it.second }

        // set the requested orientation to the nearest if it is not already as long as it is nearer than what is currently set
        val currentOrientationDist = distances
            .firstOrNull { it.first.activityInfoValue == requestedOrientation }
            ?.second
            ?: Int.MAX_VALUE

        if (requestedOrientation != nearest.first.activityInfoValue && currentOrientationDist > nearest.second) {
            Timber.d(
                "$adjustedOrientation => currentOrientation(" +
                    "${Orientation.fromActivityInfoValue(requestedOrientation)}) " +
                    "!= nearestOrientation(${nearest.first}) && " +
                    "currentDistance($currentOrientationDist) > nearestDistance(${nearest.second})",
            )

            requestedOrientation = nearest.first.activityInfoValue
        }
    }
}
