package com.example.placesproject.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.placesproject.data.model.Place
import com.example.placesproject.data.dao.PlaceDao
import com.example.placesproject.data.model.User
import com.example.placesproject.data.dao.UserDao
import com.example.placesproject.data.model.Comment
import com.example.placesproject.data.dao.CommentDao
@Database(
    entities = [Place::class, User::class, Comment::class],
    version = 5,
    exportSchema = false
)
abstract class PlaceDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao
    abstract fun userDao(): UserDao
    abstract fun commentDao(): CommentDao

    companion object {

        @Volatile
        private var INSTANCE: PlaceDatabase? = null

        fun getDatabase(context: Context): PlaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceDatabase::class.java,
                    "places_db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}