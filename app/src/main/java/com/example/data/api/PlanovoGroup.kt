package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class PlanovoGroup(
    val id: Int,
    val code: String, // e.g. "ои31-09/24"
    val course: Int, // 1, 2, 3, 4
    val direction: String // e.g. "Информационные системы и программирование"
) {
    val displayName: String
        get() = "$code • $course курс"

    val calendarIcsUrl: String
        get() = "https://planovo.pro/api/v1/public/groups/$id/calendar.ics"
}

object PlanovoGroupsCatalog {

    /**
     * Bundled catalogue of all 59 college groups from Planovo (КЭМС).
     * Provides instantaneous, zero-latency, 100% offline group selection.
     */
    val bundledGroups = listOf(
        // Информационные технологии & Программирование
        PlanovoGroup(41, "ои31-09/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(37, "ои21-09/25", 2, "Информационные системы и программирование"),
        PlanovoGroup(40, "ои21-11/25", 2, "Информационные системы и программирование"),
        PlanovoGroup(38, "ои22-09/25", 2, "Информационные системы и программирование"),
        PlanovoGroup(81, "ои31-11/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(79, "ои32-09/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(80, "ои33-09/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(82, "ои41-09/23", 4, "Информационные системы и программирование"),
        PlanovoGroup(75, "ор11-09/26", 1, "Разработка и управление программным обеспечением"),
        PlanovoGroup(78, "ор11-11/26", 1, "Разработка и управление программным обеспечением"),
        PlanovoGroup(94, "ви21-09/25", 2, "Информационные системы и программирование"),
        PlanovoGroup(95, "ви21-11/25", 2, "Информационные системы и программирование"),
        PlanovoGroup(96, "ви31-09/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(97, "ви31-11/24", 3, "Информационные системы и программирование"),
        PlanovoGroup(77, "вр11-09/26", 1, "Разработка и управление программным обеспечением"),
        PlanovoGroup(114, "вр11-11/26", 1, "Разработка и управление программным обеспечением"),
        PlanovoGroup(101, "зр11-09/26", 1, "Разработка и управление программным обеспечением"),
        PlanovoGroup(104, "зр11-11/26", 1, "Разработка и управление программным обеспечением"),

        // Экономика и бухгалтерский учет
        PlanovoGroup(13, "оэ11-09/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(17, "оэ11-11/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(14, "оэ12-09/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(15, "оэ13-09/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(70, "оэ21-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(84, "оэ21-11/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(71, "оэ22-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(87, "оэ22-11/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(72, "оэ23-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(73, "оэ24-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(85, "оэ31-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(86, "оэ32-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(25, "оэ33-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(26, "оэ34-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(42, "вэ11-09/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(112, "вэ11-11/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(88, "вэ21-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(89, "вэ21-11/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(90, "вэ31-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(99, "зэ11-09/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(102, "зэ11-11/26", 1, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(105, "зэ21-09/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(106, "зэ21-11/25", 2, "Экономика и бухгалтерский учет (по отраслям)"),
        PlanovoGroup(107, "зэ31-09/24", 3, "Экономика и бухгалтерский учет (по отраслям)"),

        // Юриспруденция
        PlanovoGroup(27, "ою11-09/26", 1, "Юриспруденция"),
        PlanovoGroup(29, "ою11-11/26", 1, "Юриспруденция"),
        PlanovoGroup(31, "ою21-09/25", 2, "Юриспруденция"),
        PlanovoGroup(30, "ою21-11/25", 2, "Юриспруденция"),
        PlanovoGroup(32, "ою22-09/25", 2, "Юриспруденция"),
        PlanovoGroup(33, "ою31-09/24", 3, "Юриспруденция"),
        PlanovoGroup(83, "ою32-09/24", 3, "Юриспруденция"),
        PlanovoGroup(44, "вю11-09/26", 1, "Юриспруденция"),
        PlanovoGroup(113, "вю11-11/26", 1, "Юриспруденция"),
        PlanovoGroup(91, "вю21-09/25", 2, "Юриспруденция"),
        PlanovoGroup(92, "вю21-11/25", 2, "Юриспруденция"),
        PlanovoGroup(93, "вю31-09/24", 3, "Юриспруденция"),
        PlanovoGroup(100, "зю11-09/26", 1, "Юриспруденция"),
        PlanovoGroup(103, "зю11-11/26", 1, "Юриспруденция"),
        PlanovoGroup(108, "зю21-09/25", 2, "Юриспруденция"),
        PlanovoGroup(109, "зю21-11/25", 2, "Юриспруденция"),
        PlanovoGroup(110, "зю31-09/24", 3, "Юриспруденция")
    )

    suspend fun fetchLiveGroups(client: OkHttpClient): List<PlanovoGroup> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://planovo.pro/api/v1/public/groups")
                .addHeader("User-Agent", "SIGEON-Calendar-Android/1.0")
                .addHeader("Accept", "application/json")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext bundledGroups

            val body = resp.body?.string() ?: return@withContext bundledGroups
            val root = JSONObject(body)
            val items = root.optJSONArray("items") ?: return@withContext bundledGroups

            val list = mutableListOf<PlanovoGroup>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val id = obj.optInt("id", 0)
                val code = obj.optString("code", "")
                val course = obj.optInt("course", 1)
                val dirObj = obj.optJSONObject("direction")
                val dirTitle = dirObj?.optString("title", "") ?: ""
                if (id > 0 && code.isNotBlank()) {
                    list.add(PlanovoGroup(id, code, course, dirTitle))
                }
            }
            if (list.isNotEmpty()) list else bundledGroups
        } catch (_: Exception) {
            bundledGroups
        }
    }
}
