package it.polito.grouptasksscreen.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.polito.grouptasksscreen.login.LoginScreenPane
import it.polito.grouptasksscreen.login.LoginViewModel
import it.polito.grouptasksscreen.profile.ChatListPane
import it.polito.grouptasksscreen.profile.NotificationList
import it.polito.grouptasksscreen.profile.UserProfile
import it.polito.grouptasksscreen.task.CommentSection
import it.polito.grouptasksscreen.task.NewEditTaskScreen
import it.polito.grouptasksscreen.task.TaskDetail
import it.polito.grouptasksscreen.task.TaskListManager
import it.polito.grouptasksscreen.team.EditTeamPane
import it.polito.grouptasksscreen.team.NewTeamPane
import it.polito.grouptasksscreen.team.PersonalChatScreen
import it.polito.grouptasksscreen.team.TeamAchievementPane
import it.polito.grouptasksscreen.team.TeamChatScreen
import it.polito.grouptasksscreen.team.TeamDetailsPane
import it.polito.grouptasksscreen.team.TeamListPane

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationController (
    handleGoogleSignIn: (callback: (Boolean) -> Unit) -> Unit,
    loginViewModel: LoginViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "Login") {

        composable(
            "teamAchievement/{teamId}",
            arguments = listOf(navArgument("teamId") { type= NavType.StringType })
        ) {
            TeamAchievementPane(navController = navController, teamId = it.arguments?.getString("teamId")!!)
        }
        composable("teamChat/{loggedUserId}/{teamId}",
            arguments = listOf(
                navArgument("loggedUserId") { type = NavType.StringType },
                navArgument("teamId") { type = NavType.StringType }
            )
        ){
            TeamChatScreen(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!, teamId = it.arguments?.getString("teamId")!!)
        }
        composable("personalChat/{loggedUserId}/{userId}",
            arguments = listOf(
                navArgument("loggedUserId") { type = NavType.StringType },
                navArgument("userId") { type= NavType.StringType }
            )
        ){
            PersonalChatScreen(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!, userId = it.arguments?.getString("userId")!!)
        }
        composable("teamList/{loggedUserId}",
            arguments = listOf(navArgument("loggedUserId") { type= NavType.StringType })
            ) {
            TeamListPane(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!)
        }
        composable(
            "taskList/{loggedUserId}/{teamId}",
            arguments = listOf(
                navArgument("loggedUserId") { type= NavType.StringType },
                navArgument("teamId") { type= NavType.StringType }
            )
            ) {
            TaskListManager(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!,teamId = it.arguments?.getString("teamId")!!)
        }
        composable(
            "teamDetails/{loggedUserId}/{teamId}/{invited}",
            arguments = listOf(
                navArgument("loggedUserId") { type= NavType.StringType },
                navArgument("teamId") { type= NavType.StringType },
                navArgument("invited") { type= NavType.StringType },
            )
        ) {
            TeamDetailsPane(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!! ,teamId = it.arguments?.getString("teamId")!!, invited = it.arguments?.getString("invited")!!)
        }
        composable(
            "taskDetail/{loggedUserId}/{taskId}",
            arguments = listOf(
                navArgument("loggedUserId") { type=NavType.StringType},
                navArgument("taskId") { type=NavType.StringType}
            )
        ) {
            TaskDetail(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!, taskId = it.arguments?.getString("taskId")!!)
        }
        composable(
            "commentSection/{loggedUserId}/{taskId}",
            arguments = listOf(
                navArgument("loggedUserId") { type=NavType.StringType},
                navArgument("taskId") { type=NavType.StringType}
            )
        ) {
            CommentSection(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!, taskId = it.arguments?.getString("taskId")!!)
        }
        composable("addTask/{teamId}/{taskId}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType }
            )
        ) {
            NewEditTaskScreen(navController = navController, teamId = it.arguments?.getString("teamId")!!, taskId = it.arguments?.getString("taskId")!!)
        }
        composable("editTeam/{teamId}/{loggedUserId}",
            arguments = listOf(
                navArgument("teamId") { type=NavType.StringType},
                navArgument("loggedUserId") { type=NavType.StringType}
                )
            ) {
            EditTeamPane(navController = navController, teamId = it.arguments?.getString("teamId")!!, loggedUserId = it.arguments?.getString("loggedUserId")!!)
        }
        composable("newTeam/{loggedUserId}",
            arguments = listOf(navArgument("loggedUserId") { type=NavType.StringType} )
            ) {
            NewTeamPane(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!)
        }
        composable("profile/{loggedUserId}/{userId}",
            arguments = listOf(
                navArgument("loggedUserId") { type = NavType.StringType },
                navArgument("userId") { type=NavType.StringType}
            )
        ) {
            UserProfile(
                navController = navController,
                loggedUserId = it.arguments?.getString("loggedUserId")!!,
                userId = it.arguments?.getString("userId")!!
            )
        }

        composable("chatList/{loggedUserId}",
            arguments = listOf(navArgument("loggedUserId") { type=NavType.StringType} )
        ) {
            ChatListPane(
                navController = navController,
                loggedUserId = it.arguments?.getString("loggedUserId")!!
            )
        }
        composable("Login"){
            LoginScreenPane(
                navController = navController,
                handleGoogleSignIn = handleGoogleSignIn,
                vm = loginViewModel
            )
        }

        composable("notifications/{loggedUserId}",
            arguments = listOf(navArgument("loggedUserId") { type=NavType.StringType} )
        ) {
            NotificationList(navController = navController, loggedUserId = it.arguments?.getString("loggedUserId")!!)
        }

    }
}
