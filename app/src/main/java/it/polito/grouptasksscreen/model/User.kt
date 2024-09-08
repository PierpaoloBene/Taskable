package it.polito.grouptasksscreen.model

import android.net.Uri

data class User(
    val id: String = "",
    val nickname: String = "",
    val fullName: String = "",
    val email: String = "",
    val telephone: String = "",
    val location: String = "",
    val description: String = "",
    val role: String = "",
    val mean: Int = 7,
    val skills: List<String> = listOf("Javascript", "Jetpack Compose"),
    val achievements: List<String> = listOf("First at coding class", "Mathematician"),
    val profilePhotoId: String = "profile1.png",
    val profilePhotoUri: Uri = Uri.EMPTY,
    val teams: List<String> = listOf(),
    val nOfTaskCompleted: Int = 0,
    val reviews: List<String> = emptyList()
)

