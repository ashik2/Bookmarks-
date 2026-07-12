package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Bookmark::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        fun getDatabase(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmark_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT INTO bookmarks (title, url, timestamp) VALUES ('Google', 'https://google.com', $now)")
                        db.execSQL("INSERT INTO bookmarks (title, url, timestamp) VALUES ('YouTube', 'https://youtube.com', $now)")
                        db.execSQL("INSERT INTO bookmarks (title, url, timestamp) VALUES ('GitHub', 'https://github.com', $now)")
                        db.execSQL("INSERT INTO bookmarks (title, url, timestamp) VALUES ('Wikipedia', 'https://wikipedia.org', $now)")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
