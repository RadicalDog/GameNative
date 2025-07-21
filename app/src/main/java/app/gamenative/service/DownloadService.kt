package app.gamenative.service

import app.gamenative.utils.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

object DownloadService {
    init {
        getDownloadDirectoryApps()
    }

    private var lastUpdateTime: Long = 0
    private lateinit var downloadDirectoryApps: MutableList<String>

    fun getDownloadDirectoryApps (): MutableList<String> {
        // What apps have folders in the download area?
        // Isn't checking for "complete" marker - incomplete is accepted

        // Only update if cache is over N milliseconds old
        val time = System.currentTimeMillis()
        if (lastUpdateTime < (time - 5 * 1000) || lastUpdateTime > time) {
            lastUpdateTime = time

            // For now, grab parent directories from SteamService
            val subDir = getSubdirectories(SteamService.internalAppInstallPath)
            subDir += getSubdirectories(SteamService.externalAppInstallPath)

            downloadDirectoryApps = subDir
        }

        return downloadDirectoryApps
    }

    private fun getSubdirectories (path: String): MutableList<String> {
        // Names of immediate subdirectories
        val subDir = File(path).list() { dir, name -> File(dir, name).isDirectory}
        if (subDir == null) {
            return emptyList<String>().toMutableList()
        }
        return subDir.toMutableList()
    }

    fun getSizeOnDiskDisplay (appId: Int, setResult: (String) -> Unit) {
        // Outputs "3.76GiB" etc to the result lambda without locking up the main thread
        if (SteamService.isAppInstalled(appId)) {
            // Slow operation, so done asynchronously with a placeholder
            var appSizeText = "..."
            setResult(appSizeText)
            CoroutineScope(Dispatchers.IO).launch {
                val job = CoroutineScope(Dispatchers.IO).async {
                    appSizeText = StorageUtils.formatBinarySize(
                        StorageUtils.getFolderSize(SteamService.getAppDirPath(appId))
                    )

                    Timber.d("Finding $appId size $appSizeText")
                    setResult(appSizeText)
                }
            }
        }
    }
}
