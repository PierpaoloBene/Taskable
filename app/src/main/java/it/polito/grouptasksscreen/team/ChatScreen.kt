package it.polito.grouptasksscreen.team

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.relay.compose.RowScopeInstanceImpl.weight
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Comment
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Notification
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.Purple80
import it.polito.grouptasksscreen.ui.theme.PurpleGrey80
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)

class ChatViewModel(private val model: Model) : ViewModel() {

    /***
     *  FUNCTIONS FOR DB CONNECTIONS
     */
    fun getChatById(chatId: String) = model.getChatById(chatId)
    fun getChatIdByTeamId(teamId: String) = model.getChatIdByTeamId(teamId)
    fun addMessage(chatId: String, message: Comment) = model.addMessageToChat(chatId, message)
    fun getTeamMembers(teamId: String) = model.getMembersByTeam(teamId)
    fun getTeamName(teamId: String) = model.getTeamName(teamId)
    fun getAllMembers() = model.getAllMembers()
    fun addNotificationToUser(userIds: List<String>, notification: Notification) = model.addNotificationToUsers(userIds , notification)

    /***
     * FUNCTIONS FOR PERSONAL CHAT
     */
    fun getPrivateChatId(users: List<String>): Flow<String> = model.getPrivateChatId(users)


    var newMessage by mutableStateOf("")
        private set
    var newMessageError by mutableStateOf("")
        private set

    fun updateNewMessage(newText: String){
        newMessage = newText
    }

    fun checkNewMessage() {
        if (newMessage.isBlank()) {
            newMessageError = "Please enter a message"
        } else {
            newMessageError = ""
        }
    }

    fun manageNotifications(userIds: List<String>, loggedUser: User, teamName: String){

        val notificationList = userIds.filter { it!=loggedUser.id }

        if(userIds.size==2 && teamName==""){    //personal chat
            addNotificationToUser(
                notificationList,
                Notification(
                    text = "New message from ${loggedUser.nickname}",
                    creationDate = LocalDateTime.now()
                )
            )
        }else{  //team chat
            addNotificationToUser(
                notificationList,
                Notification(
                    text = "New message from ${loggedUser.nickname} in chat $teamName",
                    creationDate = LocalDateTime.now()
                )
            )
        }
    }

}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun PersonalChatScreen(
    vm: ChatViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    userId: String
) {

    var user by remember { mutableStateOf(User()) }
    var loggedUser by remember { mutableStateOf(User()) }
    var chatId by remember { mutableStateOf("") }
    var chatList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    val userList by vm.getAllMembers().collectAsState(initial = emptyList())
    var chatMembers by remember { mutableStateOf<List<User>>(emptyList()) }


    LaunchedEffect(userList) {
        if (loggedUserId.isNotEmpty() && userList.isNotEmpty()) {
            user = userList.find { user -> user.id == userId }!!
            loggedUser = userList.find { user -> user.id == loggedUserId }!!
            chatMembers = listOf( user, loggedUser)
        }
    }
    LaunchedEffect(user) {
        if (user.id.isNotEmpty() && user.id.isNotBlank()) {
            vm.getPrivateChatId(listOf(userId, loggedUserId)).collect { id ->
                chatId = id
            }
        }
    }
    LaunchedEffect(chatId) {
        if (chatId.isNotEmpty()) {
            vm.getChatById(chatId).collect { chats ->
                chatList = chats
            }
        }
    }

    if(chatId.isNotBlank()) {
        Chat(
            actions = remember(navController) { Actions(navController = navController) },
            chatName = user.fullName,
            chatId = chatId,
            loggedUserId = loggedUserId,
            chatList = chatList,
            usersList = chatMembers
        )
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun TeamChatScreen(
    vm: ChatViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    teamId: String
) {

    var chatList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var usersList by remember { mutableStateOf<List<User>>(emptyList()) }
    val teamName by vm.getTeamName(teamId).collectAsState(initial = "")
    val chatId by vm.getChatIdByTeamId(teamId).collectAsState(initial = "")

    LaunchedEffect(teamId) {
        if(teamId.isNotEmpty()){
            vm.getTeamMembers(teamId).collect{users ->
                usersList = users
            }
        }
    }
    LaunchedEffect(chatId) {
        if (chatId.isNotEmpty()) {
            vm.getChatById(chatId).collect { chats ->
                chatList = chats
            }
        }
    }

    if(chatId.isNotBlank()) {
        Chat(
            actions = remember(navController) { Actions(navController = navController) },
            chatName = teamName,
            chatId = chatId,
            loggedUserId = loggedUserId,
            chatList = chatList,
            usersList = usersList,
            teamName = teamName
        )
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun Chat(
    actions: Actions,
    chatName: String,
    chatId: String,
    loggedUserId: String,
    chatList: List<Comment>,
    usersList: List<User>,
    teamName: String = "",
    vm: ChatViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext))
) {

   // val loggedUser by vm.getLoggedUserObject().collectAsState(initial = User())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(
                            text = chatName,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            actions.navigateBack()
                        },
                        modifier = Modifier.weight(0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Purple40,
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = vm.newMessage,
                            onValueChange = {vm.updateNewMessage(it)},
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = PurpleGrey80,
                                focusedContainerColor = PurpleGrey80,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(5f)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            containerColor = Color("#A5A6F6".toColorInt()),
                            shape = FloatingActionButtonDefaults.largeShape,
                            onClick = {
                                vm.updateNewMessage(
                                    vm.newMessage
                                )
                                if (vm.newMessage.isNotBlank()) {
                                    vm.addMessage(
                                        chatId,
                                        Comment(
                                            vm.newMessage,
                                            loggedUserId,
                                            LocalDateTime.now()
                                        )
                                    )

                                    vm.manageNotifications(
                                        usersList.map { it.id },
                                        usersList.first { it.id == loggedUserId },
                                        teamName
                                    )

                                    val regex = "@\\w+".toRegex()
                                    val matches = regex.findAll(vm.newMessage)
                                    var taggedUserIdList = emptyList<String>()
                                    var lastIndex = 0
                                    matches.forEach { matchResult ->
                                        val startIndex = matchResult.range.first
                                        val endIndex = matchResult.range.last + 1

                                        (vm.newMessage.substring(lastIndex, startIndex))

                                        val taggedUser =
                                            vm.newMessage.substring(startIndex, endIndex)
                                        val userId =
                                            usersList.find { user -> taggedUser.lowercase() == "@${user.nickname.lowercase()}" }?.id
                                        if (userId != null && userId.isNotBlank() && taggedUserIdList.indexOf(
                                                userId
                                            ) == -1
                                        ) {
                                            taggedUserIdList += userId
                                        }
                                    }

                                    if (taggedUserIdList.isNotEmpty()) {
                                        val Notification = Notification(
                                            text = "You have been tagged from ${usersList.find { user -> user.id == loggedUserId }?.nickname}",
                                            creationDate = LocalDateTime.now()
                                        )

                                        vm.addNotificationToUser(
                                            userIds = taggedUserIdList,
                                            notification = Notification
                                        )
                                    }

                                    vm.updateNewMessage("")
                                }

                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Chat Icon"
                            )
                        }
                    }
                }
            )
        }
    ) { it ->
        ChatContent(
            actions = actions,
            paddingValues = it,
            chatList = chatList,
            usersList = usersList,
            loggedUserId = loggedUserId
        )
    }
}

@Composable
fun ChatContent(
    actions: Actions,
    paddingValues: PaddingValues,
    chatList: List<Comment>,
    usersList: List<User>,
    loggedUserId: String
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(5.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

            chatList.forEach { message ->

                val isFromMe = message.author == loggedUserId
                Spacer(modifier = Modifier.height(4.dp))
                if (isFromMe) {
                    MessageBubble(
                        actions = actions,
                        message = message,
                        usersList = usersList,
                        timeFormatter = timeFormatter,
                        isFromMe = true,
                        loggedUserId = loggedUserId
                    )
                } else {
                    MessageBubble(
                        actions = actions,
                        message = message,
                        usersList = usersList,
                        timeFormatter = timeFormatter,
                        isFromMe = false,
                        loggedUserId = loggedUserId
                    )
                }
            }

    }
}

@Composable
fun MessageBubble(
    actions: Actions,
    message: Comment,
    usersList: List<User>,
    timeFormatter: DateTimeFormatter,
    isFromMe: Boolean,
    loggedUserId: String
) {
    val annotatedString = buildAnnotatedString {
        val messageText = message.text
        val regex = "@\\w+".toRegex()
        val matches = regex.findAll(messageText)

        var lastIndex = 0
        matches.forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1

            append(messageText.substring(lastIndex, startIndex))

            val taggedUser = messageText.substring(startIndex, endIndex)
            var user = usersList.find { user -> taggedUser.lowercase() == "@${user.nickname.lowercase()}" }

            if (user != null && user.id != loggedUserId) {
                pushStringAnnotation(tag = "userTag", annotation = user.id)
                withStyle(style = SpanStyle(color = Purple80, fontFamily = FontFamily(Font(R.font.relay_inter_regular)), fontSize = 16.sp)) {
                    append("@" + user.nickname)
                }
                pop()
            } else {
                append(taggedUser)
            }

            lastIndex = endIndex
        }

        append(messageText.substring(lastIndex))
    }
    if (isFromMe) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 48f,
                            topEnd = 48f,
                            bottomStart = 48f,
                            bottomEnd = 0f
                        )
                    )
                    .background(Color("#6B4EFF".toColorInt()))
                    .padding(1.dp)
                    .widthIn(min = 100.dp, max = 250.dp),

                ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 48f,
                                topEnd = 48f,
                                bottomStart = 48f,
                                bottomEnd = 0f
                            )
                        )
                        .background(Color("#6B4EFF".toColorInt()))
                        .padding(14.dp),){

                    ClickableText(
                        text = annotatedString,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 8.dp),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "userTag", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val userId = annotation.item
                                    actions.navigateToProfile(loggedUserId,userId)
                                }
                        }
                    )
                }

                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(0.dp, 10.dp, 2.dp, 0.dp),
                    text = message.date.format(timeFormatter),
                    color = Color.Gray,
                    fontFamily = FontFamily(
                        Font(
                            R.font.relay_inter_regular
                        )
                    ),
                    fontSize = 12.sp,
                )

            }
            val userToShow = usersList.find { user-> user.id == message.author }
            if(userToShow != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userToShow.profilePhotoUri)
                        .build(),
                    contentDescription = "",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val userToShow = usersList.find { user: User ->user.id == message.author }
            if(userToShow != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userToShow.profilePhotoUri)
                        .build(),
                    contentDescription = "",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 48f,
                            topEnd = 48f,
                            bottomStart = 0f,
                            bottomEnd = 48f
                        )
                    )
                    .background(Color("#A5A6F6".toColorInt()))
                    .padding(1.dp)
                    .widthIn(min = 100.dp, max = 250.dp),

                ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 48f,
                                topEnd = 48f,
                                bottomStart = 0f,
                                bottomEnd = 48f
                            )
                        )
                        .background(Color("#A5A6F6".toColorInt()))
                        .padding(14.dp),){
                    ClickableText(
                        text = annotatedString,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 8.dp),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "userTag", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val userId = annotation.item
                                    actions.navigateToProfile(loggedUserId,userId)
                                }
                        }
                    )
                }

                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(0.dp, 10.dp, 2.dp, 0.dp),
                    text = message.date.format(timeFormatter),
                    color = Color.Gray,
                    fontFamily = FontFamily(
                        Font(
                            R.font.relay_inter_regular
                        )
                    ),
                    fontSize = 12.sp,
                )

            }

        }
    }
}
