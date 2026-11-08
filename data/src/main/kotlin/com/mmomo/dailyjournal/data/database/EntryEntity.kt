package com.mmomo.dailyjournal.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One journal entry row (data model §1). The unique day key enforces one entry per day. */
@Entity(
    tableName = "entries",
    indices = [Index(value = ["day_key"], unique = true), Index(value = ["day_number"])],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "day_key") val dayKey: String,
    @ColumnInfo(name = "day_number") val dayNumber: Int,
    val text: String,
    @ColumnInfo(name = "prompt_id") val promptId: String,
    @ColumnInfo(name = "prompt_text") val promptText: String,
    /** UTC milliseconds since 1970. */
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** UTC milliseconds since 1970. */
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
