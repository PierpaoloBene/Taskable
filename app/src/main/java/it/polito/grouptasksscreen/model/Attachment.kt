package it.polito.grouptasksscreen.model


data class Attachment(
        val name: String,
        val uri: String,
){
        constructor() : this ("", "")
}
