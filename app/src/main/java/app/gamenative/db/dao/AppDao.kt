package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.gamenative.data.AppBase
import app.gamenative.data.SteamApp
import app.gamenative.enums.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(apps: AppBase)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(apps: List<AppBase>)

    @Update
    suspend fun update(app: AppBase)

    @Update
    suspend fun update(app: List<AppBase>)

    @Query(
        "SELECT * FROM app_base"
    )
    fun getAllApps(): Flow<List<AppBase>>

    @Query("SELECT * FROM app_base WHERE source = :source")
    suspend fun getAllAppsFromSource(source: Source): List<AppBase>


    @Query("SELECT * FROM app_base WHERE sourceId = :appId AND source = :source")
    suspend fun findApp(appId: Int, source: Source): AppBase?

    @Query("DELETE from app_base")
    suspend fun deleteAll()
}
