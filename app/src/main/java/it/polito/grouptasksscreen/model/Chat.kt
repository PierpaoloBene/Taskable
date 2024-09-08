package it.polito.grouptasksscreen.model

data class Chat(
    val chatId: String,
    val teamId: String = "-1",
    val messagesList : List<Comment> = emptyList(),
    val userList : List<User> = emptyList(),

)

