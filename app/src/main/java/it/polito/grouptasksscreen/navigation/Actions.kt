package it.polito.grouptasksscreen.navigation

import androidx.navigation.NavController

class Actions(navController: NavController) {

    val navigateToTeamAchievement: (String) -> Unit = { teamId ->
        navController.navigate("teamAchievement/$teamId")
    }
    val navigateToTeamChat: (String, String) -> Unit = {loggedUserId, teamId ->
        navController.navigate("teamChat/$loggedUserId/$teamId")
    }
    val navigateToPersonalChat: (String, String) -> Unit = {loggedUserId, userId ->
        navController.navigate("personalChat/$loggedUserId/$userId")
    }
    val navigateToTaskList: (String, String) -> Unit = { loggedUserId,teamId ->
        navController.navigate("taskList/$loggedUserId/$teamId")
    }
    val navigateToTeamDetails: (String, String, String) -> Unit = { loggedUserId,teamId,invited ->
        navController.navigate("teamDetails/$loggedUserId/$teamId/$invited")
    }

    val navigateToTaskDetail: (String, String) -> Unit = { loggedUserId,taskId ->
        navController.navigate("taskDetail/$loggedUserId/$taskId")
    }

    val navigateToCommentSection: (String, String) -> Unit = {loggedUserId, taskId ->
        navController.navigate("commentSection/$loggedUserId/$taskId")
    }

    val navigateToAddTask: (String, String) -> Unit = { teamId, taskId ->
        navController.navigate("addTask/$teamId/$taskId")
    }
    val navigateToEditTeam: (String, String) -> Unit = {teamId, loggedUserId ->
        navController.navigate("editTeam/$teamId/$loggedUserId")
    }
    val navigateToTeamList: (String) -> Unit = { teamId ->
        navController.navigate("teamList/$teamId")
    }
    val navigateToNewTeam: (String) -> Unit = {loggedUserId ->
        navController.navigate("newTeam/$loggedUserId")
    }

    val navigateToProfile: (String, String) -> Unit = { loggedUserId, userId ->
        navController.navigate("profile/$loggedUserId/$userId")
    }

    val navigateToChatList: (String) -> Unit = {loggedUserId ->
        navController.navigate("chatList/$loggedUserId")
    }

    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

    val navigateToNotifications: (String) -> Unit = { loggedUserId ->
        navController.navigate("notifications/$loggedUserId")
    }

    val navigateToLogin: () -> Unit = {
        navController.navigate("Login")
    }
}