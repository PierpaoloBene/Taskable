package it.polito.grouptasksscreen.team.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.ui.theme.Purple40

@Composable
fun BottomAppBar(
    navController: NavController,
    loggedUserId: String
    ){


    var selectedItem by remember { mutableStateOf(0) }
    val actions = remember(navController) { Actions(navController) }

    Box {
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 1.dp),
            containerColor = Color.White,
            contentColor = Purple40
        ) {
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                selected = selectedItem == 0,
                onClick = {
                    selectedItem = 0
                    actions.navigateToTeamList(loggedUserId)
                          },
                selectedContentColor = Color.Gray
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                selected = selectedItem == 1,
                onClick = {
                    selectedItem = 1
                    actions.navigateToProfile(loggedUserId,loggedUserId)
              },
                selectedContentColor = Color.Gray
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat") },
                selected = selectedItem == 2,
                onClick = {
                    selectedItem = 2
                    actions.navigateToChatList(loggedUserId)

                          },
                selectedContentColor = Color.Gray
            )
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications"
                    )
                },
                selected = selectedItem == 3,
                onClick = {
                    selectedItem = 3
                    actions.navigateToNotifications(loggedUserId)
                          },
                selectedContentColor = Color.Gray
            )
        }
    }
}