package it.polito.grouptasksscreen.model

import java.time.LocalDateTime

data class History (
    var text: String = "",
    var creationDate: LocalDateTime = LocalDateTime.MIN
)