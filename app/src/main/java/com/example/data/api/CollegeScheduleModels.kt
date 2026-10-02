package com.example.data.api

data class CollegeLesson(
    val id: String = "",
    val subject: String,
    val teacher: String = "",
    val room: String = "",
    val lessonType: String = "Пара", // Лекция, Практика, Лабораторная, Семинар
    val pairNumber: Int = 1,
    val startTime: String = "08:30",
    val endTime: String = "10:00",
    val date: String = "", // YYYY-MM-DD
    val note: String = ""
)

data class CollegeProfile(
    val id: String,
    val collegeName: String,
    val groupName: String,
    val apiUrl: String,
    val description: String
)
