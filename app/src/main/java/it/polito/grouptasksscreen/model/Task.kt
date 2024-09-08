package it.polito.grouptasksscreen.model

import java.time.LocalDateTime

enum class Status {
    PENDING, IN_PROGRESS, CHECKING, COMPLETED, OVERDUE
}

enum class Priority{
   NO_PRIORITY, LOW_PRIORITY, MEDIUM_PRIORITY, HIGH_PRIORITY
}

data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedUser: List<String> = listOf(),
    val comments: List<Comment> = listOf(),
    var creationDate: LocalDateTime = LocalDateTime.now(),
    var dueDate: LocalDateTime = LocalDateTime.now(),
    val status: Status = Status.PENDING,
    val tagList: List<String> = listOf(),
    val priority: Priority = Priority.NO_PRIORITY,
    val team: String = "",
    val history: List<History> = emptyList(),
    val attachments: List<String> = listOf(),
) {

    fun setId (newId: String) : Task{
        return this.copy(id = newId)
    }

    fun setTitle(newTitle: String) : Task{
        return this.copy(title = newTitle)
    }

    fun setDescription(newDescription: String): Task{
        return this.copy(description = newDescription)
    }

    /*fun addUser(newUser: User) : Task{
        if (assignedUser.indexOf(newUser) == -1){ // add only if the user is not already in the list
            return this.copy(assignedUser = assignedUser + newUser)
        }
        return this
    }

    fun rmUser(oldUser: User) : Task {
        if (assignedUser.indexOf(oldUser) != -1) { // rm only if the user is in the list
            return this.copy(assignedUser = assignedUser - oldUser)
        }
        return this
    }*/

    fun addComment(comment: Comment) : Task {
        return this.copy(comments = comments + comment)
    }

    fun setDueDate(newDueDate: LocalDateTime): Task{
        return this.copy(dueDate = newDueDate)
    }

    fun setStatus(newStatus: Status): Task{
        return this.copy(status = newStatus)
    }

    fun addTag(newTag: String): Task{
        if ( tagList.size < 10 ) { // maximum number of tag is 10
            if (newTag.length < 50) { // maximum length of tag is 50
                if (tagList.indexOf(newTag) == -1){ // no duplicate tags
                    return this.copy(tagList = tagList + newTag)
                }
            }
        }
        return this
    }

    fun rmTag(oldTag: String): Task{
        if (tagList.indexOf(oldTag) != -1) { // rm only tag already in the list
            return this.copy(tagList = tagList - oldTag)
        }
        return this
    }

    fun setPriority(newPriority: Priority): Task{
        return this.copy(priority = newPriority)
    }

}