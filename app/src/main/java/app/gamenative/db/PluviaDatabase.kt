package app.gamenative.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.gamenative.data.ChangeNumbers
import app.gamenative.data.Emoticon
import app.gamenative.data.FileChangeLists
import app.gamenative.data.FriendMessage
import app.gamenative.data.LibraryItem
import app.gamenative.data.SteamApp
import app.gamenative.data.SteamFriend
import app.gamenative.data.SteamLicense
import app.gamenative.db.converters.AppConverter
import app.gamenative.db.converters.ByteArrayConverter
import app.gamenative.db.converters.FriendConverter
import app.gamenative.db.converters.LicenseConverter
import app.gamenative.db.converters.PathTypeConverter
import app.gamenative.db.converters.UserFileInfoListConverter
import app.gamenative.db.dao.AppDao
import app.gamenative.db.dao.ChangeNumbersDao
import app.gamenative.db.dao.EmoticonDao
import app.gamenative.db.dao.FileChangeListsDao
import app.gamenative.db.dao.FriendMessagesDao
import app.gamenative.db.dao.SteamAppDao
import app.gamenative.db.dao.SteamFriendDao
import app.gamenative.db.dao.SteamLicenseDao

const val DATABASE_NAME = "pluvia.db"

@Database(
    entities = [
        LibraryItem::class,
        SteamApp::class,
        SteamLicense::class,
        SteamFriend::class,
        ChangeNumbers::class,
        FileChangeLists::class,
        FriendMessage::class,
        Emoticon::class,
    ],
    version = 8,
    exportSchema = false, // Should export once stable.
)
@TypeConverters(
    AppConverter::class,
    ByteArrayConverter::class,
    FriendConverter::class,
    LicenseConverter::class,
    PathTypeConverter::class,
    UserFileInfoListConverter::class,
)
abstract class PluviaDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    abstract fun steamLicenseDao(): SteamLicenseDao

    abstract fun steamAppDao(): SteamAppDao

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun appChangeNumbersDao(): ChangeNumbersDao

    abstract fun appFileChangeListsDao(): FileChangeListsDao

    abstract fun friendMessagesDao(): FriendMessagesDao

    abstract fun emoticonDao(): EmoticonDao
}
