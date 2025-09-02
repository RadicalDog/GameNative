package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import app.gamenative.Constants
import app.gamenative.enums.AppType
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService

/**
 * Data class for the Library list
 */
@Entity(
    tableName = "app_base",
)
data class LibraryItem(
    /* What we save about each app regardless of source.
    Enough info to locate in the source DAO, not intended to duplicate.

    Also responsible for tracking files on disk, remember info for filtering, and
    provide user customisation e.g. favourites or title/icon overrides
    */

    // Essential
    val name: String,
    @ColumnInfo(index = true)
    val source: Source,
    @ColumnInfo(index = true)
    val appId: Int, // to find original data
    @PrimaryKey
    val uniqueID: String = AppSourceService.getUniqueId(source, appId),

    val iconHash: String = "", // More of an icon filename - url/[hash].jpg

    // Nonessential
    val lastPlayed: Int = 0,
    val isInstalled: Boolean = false,
    val downloadProgress: Float = 0f,
    val workingDirectory: String = "",
    val pathToExe: String = "",
    val onExternal: Boolean = false,
    val sizeOnDisk: Long = 0, // Assumed untrustworthy; fill this often

    // User created info
    val favourite: Boolean = false,
    val hidden: Boolean = false,

    // Info needed from sync
    val reportedInstallSize: Long = 0, // in bytes, according to the source
    val isShared: Boolean = false, // Family share
    val type: AppType = AppType.game,
) {
    // Display index, not important
    @Ignore
    var index: Int = 0
    val clientIconUrl: String
        get() = Constants.Library.ICON_URL + "$appId/$iconHash.ico"
}
