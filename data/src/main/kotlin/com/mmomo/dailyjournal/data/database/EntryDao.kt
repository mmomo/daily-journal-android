package com.mmomo.dailyjournal.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/** Bound-parameter queries only (DATA-SEC-7). */
@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE day_key = :dayKey LIMIT 1")
    suspend fun byDayKey(dayKey: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE day_number BETWEEN :from AND :through")
    suspend fun inRange(from: Int, through: Int): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE day_number > :dayNumber")
    suspend fun after(dayNumber: Int): List<EntryEntity>

    @Query("SELECT * FROM entries")
    suspend fun all(): List<EntryEntity>

    /** Fails if the day already has another entry (unique day_key), never replaces (DATA-E1). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(entry: EntryEntity)
}
