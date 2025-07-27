package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "app_base",
    primaryKeys = ["source", "sourceId"] // Combo PK is unique
)
data class AppBase(
    /* What we save about each app regardless of source.
    Enough info to locate in the source DAO, not intended to duplicate.

    Also responsible for tracking files on disk, remember info for filtering, and
    provide user customisation e.g. favourites or title/icon overrides
    */

    // Essential
    val title: String,
    @ColumnInfo(index = true)
    val source: String,
    @ColumnInfo(index = true)
    val sourceId: Int, // to find original data

    // Nonessential
    val lastPlayed: Int = 0,
    val downloadedPercent: Int = 0,
    val downloadFolder: String = "",
    val downloadFolderName: String = "",
    val sizeOnDisk: Long = 0, // Assumed untrustworthy; fill this often

    // User created info
    val favourite: Boolean = false,
    val hidden: Boolean = false,

    // Info needed from sync
    val reportedInstallSize: Long = 0, // in bytes, according to the source
    val familyShare: Boolean = false,
    val type: Int = 0,
)
