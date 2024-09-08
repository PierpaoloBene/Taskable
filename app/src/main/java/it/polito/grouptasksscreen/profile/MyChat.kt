package it.polito.grouptasksscreen.profile

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Chat
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.team.utils.BottomAppBar
import java.time.format.DateTimeFormatter

class ChatListViewModel(private val model: Model) : ViewModel(){
    fun getChatByChatId(chatId: String) = model.getChatByChatId(chatId)
    fun getChatIdsByUserId(userId: String) = model.getChatIdsByUserId(userId)
    fun getTeam(teamId: String) = model.getTeamById(teamId)

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ChatListPane(
    vm: ChatListViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String
) {

    val actions = remember(navController) {
        Actions(navController = navController)
    }

    //var chatList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    
    val chatIds by vm.getChatIdsByUserId(loggedUserId).collectAsState(initial = emptyList())


    Scaffold (
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color("#FFFAFA".toColorInt()),
                    titleContentColor = Color("#061E51".toColorInt()),
                ),
                title = {
                    Column {
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = FontFamily(
                                Font(
                                    R.font.relay_inter_bold
                                )
                            )

                        )
                    }
                },
            )
        },
        bottomBar = { BottomAppBar(navController, loggedUserId) }
    ) {

        Column (modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
            .padding(it)){
            chatIds.forEach{ chatId ->
                val chat by vm.getChatByChatId(chatId).collectAsState(initial = Chat("-1"))

                if(chat != Chat("-1")) {

                    if(chat.teamId == "-1"){
                        val recipientUser = chat.userList.filter { user -> user.id != loggedUserId }[0]
                        PersonalChatRow( chat, recipientUser, onClick = { actions.navigateToPersonalChat(loggedUserId, recipientUser.id) })
                    }else{
                        val team by vm.getTeam(chat.teamId).collectAsState(initial = Team("-1"))
                        TeamChatRow( chat, team, onClick = { actions.navigateToTeamChat(loggedUserId,chat.teamId) })
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp)
                }
            }

        }

    }

}

@Composable
fun PersonalChatRow(
    chat: Chat,
    recipientUser: User,
    onClick: () -> Unit,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")


    //region PrivateChat
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .background(Color.White)
    ) {
        //region ProfileImage
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(recipientUser.profilePhotoUri)
                .build(),
            contentDescription = "recipient profile image",
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        //endregion
        Column(modifier = Modifier.weight(1f)) {
            //region UserName

            Text(
                text = recipientUser.nickname, // Replace with chat name
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            //endregion

            //region LastMessage
            //region No Message Yet
            if (chat.messagesList.isEmpty()) {
                Text(
                    text = "Chat ", // Replace with last message preview
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                //endregion
                //region Last message Text
            } else {
                Text(
                    text = chat.messagesList.last().text, // Replace with last message preview
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            //endregion
            //endregion
        }
        //region Timestamp
        if (chat.messagesList.isNotEmpty()) {
            Text(
                text = chat.messagesList.last().date.format(timeFormatter), // Replace with timestamp of the last message
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        //endregion
    }
    //endregion
}

@Composable
fun TeamChatRow(
    chat: Chat,
    team: Team,
    onClick: () -> Unit,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")


    if (team.id != "-1") {
        //region TeamChat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
                .background(Color.White)
        ) {
            //region TeamImage
            if (team.groupImageUri != Uri.EMPTY) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(team.groupImageUri)
                        .build(),
                    contentDescription = "recipient profile image",
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "recipient profile image",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            //endregion
            Column(modifier = Modifier.weight(1f)) {
                //region TeamName

                    Text(
                        text = team.name, // Replace with chat name
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                //endregion

                //region LastMessage
                //region No Message Yet
                if (chat.messagesList.isEmpty()) {
                    Text(
                        text = "No message yet. Say hi to your teamMate!", // Replace with last message preview
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    //endregion
                    //region Last message Text
                } else {
                    Text(
                        text = chat.messagesList.last().text, // Replace with last message preview
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                //endregion
                //endregion
            }
            //region Timestamp
            if (chat.messagesList.isNotEmpty()) {
                Text(
                    text = chat.messagesList.last().date.format(timeFormatter), // Replace with timestamp of the last message
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            //endregion
        }
        //endregion
    }


}
