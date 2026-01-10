package com.piquecode.micromood.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.piquecode.micromood.util.Constants.DATABASE_NAME
import com.piquecode.micromood.util.DataConverters

// Migration from version 1 to 2 - adds notes column
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Mood ADD COLUMN notes TEXT DEFAULT NULL"
        )
    }
}

@Database(entities = [Mood::class], version = 2, exportSchema = false)
@TypeConverters(DataConverters::class)
abstract class MoodDatabase : RoomDatabase() {

    abstract fun getMoodDao(): MoodDao

    companion object {
        @Volatile 
        private var instance: MoodDatabase? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, MoodDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}