package com.mmomo.dailyjournal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Entries database, schema version 1 (data model §1). Schemas are exported to `data/schemas/`; later
 * versions must add explicit migrations, and destructive fallback is never enabled (NFR-7.2).
 */
@Database(entities = [EntryEntity::class], version = 1, exportSchema = true)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao

    companion object {
        const val FILE_NAME = "journal.db"

        /** The app's database in its private storage. Create once per process. */
        fun open(context: Context): JournalDatabase =
            Room.databaseBuilder(context.applicationContext, JournalDatabase::class.java, FILE_NAME).build()

        /** An in-memory database for tests and previews. */
        fun inMemory(context: Context): JournalDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, JournalDatabase::class.java).build()
    }
}
