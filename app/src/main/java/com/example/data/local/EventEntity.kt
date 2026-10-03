package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["googleEventId"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: String, // YYYY-MM-DD
    val startTime: String = "", // HH:mm
    val endTime: String = "", // HH:mm
    val location: String = "",
    val type: String = "EVENT", // "EVENT", "COLLEGE", "GOOGLE"
    val collegeSubject: String = "",
    val collegeTeacher: String = "",
    val collegeRoom: String = "",
    val collegeLessonType: String = "", // Лекция, Практика, Лабораторная, etc.
    val collegePairNumber: Int = 0,
    val googleEventId: Long? = null,
    val colorHex: String = "#6366F1",
    val isCompleted: Boolean = false,
    val isCustomEdited: Boolean = false,
    val imageUri: String? = null, // Local path or URI to user attached image/photo
    val createdAt: Long = System.currentTimeMillis()
)
