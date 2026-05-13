package com.hasiru.usiru.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TreeTag::class], version = 1)
@TypeConverters(Converters::class)
abstract class HasiruDatabase : RoomDatabase() {
    abstract fun treeTagDao(): TreeTagDao

    companion object {
        @Volatile private var instance: HasiruDatabase? = null

        fun get(context: Context): HasiruDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HasiruDatabase::class.java,
                    "hasiru-usiru.db"
                ).build().also { instance = it }
            }
        }
    }
}
