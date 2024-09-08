package it.polito.grouptasksscreen.navigation

import it.polito.grouptasksscreen.team.TeamListViewModel
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.polito.grouptasksscreen.Application
import it.polito.grouptasksscreen.login.LoginViewModel
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.profile.ChatListViewModel
import it.polito.grouptasksscreen.profile.NotificationViewModel
import it.polito.grouptasksscreen.profile.UserViewModel
import it.polito.grouptasksscreen.task.CommentViewModel
import it.polito.grouptasksscreen.task.NewEditTaskViewModel
import it.polito.grouptasksscreen.task.TaskDetailViewModel
import it.polito.grouptasksscreen.task.TaskListViewModel
import it.polito.grouptasksscreen.team.ChatViewModel
import it.polito.grouptasksscreen.team.EditTeamViewModel
import it.polito.grouptasksscreen.team.NewTeamViewModel
import it.polito.grouptasksscreen.team.TeamAchievementViewModel
import it.polito.grouptasksscreen.team.TeamDetailsViewModel

@Suppress("UNCHECKED_CAST")
@RequiresApi(Build.VERSION_CODES.O)
class Factory(context: Context): ViewModelProvider.Factory {

    val model : Model = (context.applicationContext as? Application)?.model ?: throw IllegalArgumentException("Bad application class")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if(modelClass.isAssignableFrom(CommentViewModel::class.java))
            CommentViewModel(model) as T
        else if (modelClass.isAssignableFrom(TaskListViewModel::class.java))
            TaskListViewModel(model) as T
        else if (modelClass.isAssignableFrom(TeamDetailsViewModel::class.java))
            TeamDetailsViewModel(model) as T
        else if (modelClass.isAssignableFrom(NewEditTaskViewModel::class.java))
            NewEditTaskViewModel(model) as T
        else if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java))
            TaskDetailViewModel(model) as T
        else if (modelClass.isAssignableFrom(TeamListViewModel::class.java))
            TeamListViewModel(model) as T
        else if (modelClass.isAssignableFrom(EditTeamViewModel::class.java))
            EditTeamViewModel(model) as T
        else if (modelClass.isAssignableFrom(NewTeamViewModel::class.java))
            NewTeamViewModel(model) as T
        else if (modelClass.isAssignableFrom(TeamAchievementViewModel::class.java))
            TeamAchievementViewModel(model) as T
        else if (modelClass.isAssignableFrom(ChatViewModel::class.java))
            ChatViewModel(model) as T
        else if (modelClass.isAssignableFrom(UserViewModel::class.java))
            UserViewModel(model) as T
        else if (modelClass.isAssignableFrom(ChatListViewModel::class.java))
            ChatListViewModel(model) as T
        else if (modelClass.isAssignableFrom(LoginViewModel::class.java))
            LoginViewModel(model) as T
        else if (modelClass.isAssignableFrom(NotificationViewModel::class.java))
            NotificationViewModel(model) as T
        else throw IllegalArgumentException("Unknown model class")
    }
}