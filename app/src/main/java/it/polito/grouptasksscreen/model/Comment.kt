package it.polito.grouptasksscreen.model

import java.time.LocalDateTime

data class Comment(
    val text: String = "",
    val author: String = "",
    val date: LocalDateTime = LocalDateTime.MIN
)