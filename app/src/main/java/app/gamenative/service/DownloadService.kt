package app.gamenative.service

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.gamenative.data.LibraryItem
import app.gamenative.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object DownloadService {

    private var lastUpdateTime: Long = 0
    private var downloadDirectoryApps: MutableList<String>? = null

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

        return downloadDirectoryApps ?: mutableListOf()
    }

    private fun getSubdirectories (path: String): MutableList<String> {
        // Names of immediate subdirectories
        val subDir = File(path).list() { dir, name -> File(dir, name).isDirectory}
        if (subDir == null) {
            return emptyList<String>().toMutableList()
        }
        return subDir.toMutableList()
    }

    fun getSizeFromStoreDisplay (appId: Int): String {
        // How big is the game? The store should know. Human readable.
        val depots = SteamService.getDownloadableDepots(appId)
        val installBytes = depots.values.sumOf { it.manifests["public"]?.size ?: 0L }
        return StorageUtils.formatBinarySize(installBytes)
    }

    suspend fun getSizeOnDiskDisplay (libraryItem: LibraryItem, setResult: (String) -> Unit) {
        // Outputs "3.76GiB" etc to the result lambda without locking up the main thread
        withContext(Dispatchers.IO) {
            // Set from what we know, if it's been set before
            if (libraryItem.sizeOnDisk > 0) {
                setResult(StorageUtils.formatBinarySize(libraryItem.sizeOnDisk))
            }

            if (libraryItem.downloadProgress > 0) {
                // Has downloaded more than nothing, so count the size as it is now
                val bytes = StorageUtils.getFolderSize(libraryItem.workingDirectory)
                val updatedItem = libraryItem.copy(sizeOnDisk = bytes)
                DaoService.db.appDao().update(updatedItem)

                // Update the output again
                val appSizeText = StorageUtils.formatBinarySize(bytes)
                setResult(appSizeText)
                Timber.d("Finding ${libraryItem.name} size on disk $appSizeText")
            }
        }
    }

    fun getInternalStorageBase(): String {
        val path = DaoService.getContext().filesDir?.path
        return path ?: ""
    }

    fun isInternetConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun setDownloadProgress(libraryItem: LibraryItem, progress: Float) {
        val installed: Boolean = (progress >= 1f)
        val updatedItem = libraryItem.copy(downloadProgress = progress, isInstalled = installed)
        DaoService.db.appDao().update(updatedItem)
    }
}
