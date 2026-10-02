package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val date: String? = null, // Associated date YYYY-MM-DD or null
    val tag: String = "Учеба", // "Учеба", "Д/З", "Экзамен", "Личное", "Важное"
    val colorHex: String = "#6366F1",
    val isPinned: Boolean = false,
    val checklistJson: String = "", // JSON or formatted tasks
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
