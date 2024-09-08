package it.polito.grouptasksscreen.model

import java.time.LocalDateTime

data class Notification(
    val id: String = "",
    val text: String = "",
    val readFlag: Boolean = false,
    val creationDate: LocalDateTime = LocalDateTime.MIN
)