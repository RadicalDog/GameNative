package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.gamenative.data.LibraryItem
import app.gamenative.enums.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(apps: LibraryItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(apps: List<LibraryItem>)

    @Update
    suspend fun update(app: LibraryItem)

    @Update
    suspend fun update(app: List<LibraryItem>)

    @Query(
        "SELECT * FROM app_base"
    )
    fun getAllApps(): Flow<List<LibraryItem>>

    @Query("SELECT * FROM app_base WHERE source = :source")
    suspend fun getAllAppsFromSource(source: Source): List<LibraryItem>


    @Query("SELECT * FROM app_base WHERE appId = :appId AND source = :source")
    suspend fun findApp(appId: Int, source: Source): LibraryItem?

    @Query("DELETE from app_base")
    suspend fun deleteAll()
}
