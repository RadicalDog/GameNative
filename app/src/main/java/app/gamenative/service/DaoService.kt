package app.gamenative.service

import android.content.Context
import androidx.room.Room
import app.gamenative.db.PluviaDatabase

object DaoService {
    // Now you can make queries with DaoService.db

    private lateinit var applicationContext: Context;

    val db: PluviaDatabase by lazy {
        Room.databaseBuilder(applicationContext, PluviaDatabase::class.java, "app").build()
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}
