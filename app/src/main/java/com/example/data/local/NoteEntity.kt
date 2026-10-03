package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["date"]),
        Index(value = ["tag"]),
        Index(value = ["isPinned"]),
        Index(value = ["classSlotId"]),
        Index(value = ["collegePairNumber"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val date: String? = null, // Associated date YYYY-MM-DD or null
    val tag: String = "Учеба", // "Учеба", "Д/З", "Экзамен", "Личное", "Важное"
    val colorHex: String = "#6366F1",
    val isPinned: Boolean = false,
    val checklistJson: String = "", // Serialized ChecklistItem list
    val imageUri: String? = null, // Local path or URI to attached user image
    val classSlotId: Long? = null, // Associated class slot / EventEntity ID
    val collegePairNumber: Int = 0, // Associated pair number (1, 2, 3...)
    val collegeSubject: String = "", // Associated college subject name
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getChecklistItems(): List<ChecklistItem> {
        if (checklistJson.isBlank()) return emptyList()
        val list = mutableListOf<ChecklistItem>()
        try {
            val jsonArray = JSONArray(checklistJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id", i.toString()),
                        text = obj.optString("text", ""),
                        isDone = obj.optBoolean("isDone", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun serializeChecklist(items: List<ChecklistItem>): String {
            if (items.isEmpty()) return ""
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isDone", item.isDone)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }
    }
}
