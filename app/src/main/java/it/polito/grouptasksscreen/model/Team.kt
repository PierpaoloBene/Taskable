package it.polito.grouptasksscreen.model

import android.net.Uri
import java.time.LocalDateTime

data class Team(
    val id: String = "",
    val chatId: String = "",
    val name: String = "",
    val groupImageId: String = "",
    val groupImageUri: Uri = Uri.EMPTY,
    val category: String = "",
    val description: String = "",
    val members: List<String> = listOf(),
    var creationDate: LocalDateTime = LocalDateTime.now(),
    val invitationLink: String = "",
    val ownerId: String = "",
)