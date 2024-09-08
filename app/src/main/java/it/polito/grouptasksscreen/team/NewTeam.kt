package it.polito.grouptasksscreen.team

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Notification
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.profile.createImageFile
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Objects
@RequiresApi(Build.VERSION_CODES.O)
class NewTeamViewModel(private val model: Model) : ViewModel() {
    fun getAllMembers() = model.getAllMembers()
    fun addUsers(teamId: String, users: List<User>) = model.addUsers(teamId, users)
    private fun createChat(teamId: String, callback: (String?) -> Unit) {
        // Usa una coroutine per raccogliere il chatId dalla Flow
        CoroutineScope(Dispatchers.IO).launch {
            model.createTeamChat(teamId).collect { chatId ->
                callback(chatId)
            }
        }
    }
    private fun updateTeamWithChatId(teamId: String, chatId: String, callback: (Boolean) -> Unit) =
        model.updateTeamWithChatId(teamId, chatId, callback)

    fun createNewTeam(teamObj: Team, callback: (String?) -> Unit) {
        model.createNewTeam(teamObj) { newTeamId ->
            if (newTeamId != null) {
                // Chiama createChat con il teamId appena creato
                createChat(newTeamId) { chatId ->
                    if (chatId != null) {
                        // Aggiorna il team con il chatId di ritorno
                        updateTeamWithChatId(newTeamId, chatId) { success ->
                            if (success) {
                                // Invochiamo la lambda di callback con l'ID del nuovo team
                                callback(newTeamId)
                            } else {
                                callback(null)
                            }
                        }
                    } else {
                        callback(null)
                    }
                }
            } else {
                callback(null)
            }
        }
    }
    fun addNotificationToUsers(userIds: List<String>, notification: Notification) = model.addNotificationToUsers(userIds, notification)

    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var category by mutableStateOf("")
    private var nOfCompletedTask: Int = 0
    private var nOfTotalTask: Int = 0
    private var groupImageId: String = "profile1.png"
    var userIds: List<String> by mutableStateOf(emptyList())
    var id: String = "sono o non sono un id"
    var creationDate: LocalDateTime = LocalDateTime.now()
    private var teamPhotoUri by mutableStateOf<Uri>(Uri.EMPTY)
    var ownerId: String = ""
    // Inizializza teamobj con un valore predefinito
    var teamobj: Team = Team(
        id = "",
        name = "",
        category = "",
        description = "",
        groupImageId = "profile1.png",
        groupImageUri = teamPhotoUri,
        members = emptyList(),
        creationDate = LocalDateTime.now(),
        ownerId = ""
    )
    // Funzione per aggiornare teamobj
    fun updateTeamObj() {
        teamobj = Team(
            name = name,
            category = category,
            description = description,
            groupImageId = groupImageId,
            groupImageUri = teamPhotoUri,
            members = userIds,
            creationDate = creationDate,
            ownerId = ownerId
        )
    }
    //region PROFILE PHOTO
    private var permissionCheckResult by mutableIntStateOf(0)
    fun setpermissionCheckResult(perm: Int) {
        permissionCheckResult = perm
    }
    fun setProfilePhoto(photoUri: Uri?) {
        if (photoUri == null) {
            Log.d("xxxxxxx", "no photo selected")
        } else {
            teamobj = teamobj.copy(groupImageUri = photoUri)
            teamPhotoUri = photoUri
        }
    }
    //region USER LIST
    var userList: List<User> by mutableStateOf(emptyList())

    fun initUserList(loggedUser: User){
        this.userList += loggedUser
    }
    fun addUser() {
        this.userList += usersToAdd
        this.userIds += usersToAdd.map { it.id }
        this.usersToAdd = emptyList()
    }
    fun removeUser() {
        this.usersToRemove.forEach { userToRemove ->
            this.userList -= userToRemove
            this.userIds -= usersToRemove.map { it.id }
        }
        this.userList = this.userList
        this.usersToRemove = emptyList()
    }
    //region REMOVE USER LIST
    var usersToRemove: List<User> by mutableStateOf(emptyList())
    fun addUserToRemove(userToAdd: User) {
        this.usersToRemove += userToAdd
    }
    fun removeUserToRemove(userId: String) {
        this.usersToRemove = this.usersToRemove.filter { it.id != userId }
    }
    //region ADD USER LIST
    var usersToAdd: List<User> by mutableStateOf(emptyList())
    fun addUsertoAdd(userToAdd: User) {
        this.usersToAdd += userToAdd
    }
    fun removeUsertoAdd(userId: String) {
        this.usersToAdd = this.usersToAdd.filter { it.id != userId }
    }
}
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun NewTeamPane(
    vm: NewTeamViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String
) {
    vm.ownerId = loggedUserId

    val actions = remember(navController) {
        Actions(navController = navController)
    }
    //region PHOTO FROM GALLERY INITIALIZATIONS
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            vm.setProfilePhoto(it)
        }
    )
    //region IMAGE CAPTURING UTILITES
    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        context.packageName + ".provider", file
    )
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                vm.setProfilePhoto(uri)
            } else {
                Toast.makeText(context, "Error ", Toast.LENGTH_SHORT).show()
            }
        }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            vm.setpermissionCheckResult(PackageManager.PERMISSION_GRANTED)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            vm.setpermissionCheckResult(PackageManager.PERMISSION_DENIED)
        }
    }
    vm.setpermissionCheckResult(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    )
    //region BOTTOM SHEET INITIALIZATION
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    //region PROFILE ICON BOTTOM SHEET
    if (showBottomSheet) {
        ModalBottomSheet(
            containerColor = Color.White,
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            //region GALLERY BUTTON
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    //region ICON
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "gallery",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(5.dp, 1.dp, 5.dp, 1.dp),
                    )
                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Choose From Gallery",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                }
            }
            //region CAMERA BUTTON
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    //region ICON
                    Icon(
                        painter = painterResource(id = R.drawable.camera_icon),
                        contentDescription = "gallery",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(5.dp, 1.dp, 5.dp, 1.dp),
                    )
                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Take a photo",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                }
            }
            //region CLOSE MENU
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = {
                        showBottomSheet = false
                    },
                    colors = ButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Red,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    //region ICON
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close menu",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(5.dp, 1.dp, 5.dp, 1.dp),
                    )
                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Close Menu",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                }
            }
        }
    }

    //region ADD MEMBER BOTTOM SHEET
    val userBottomSheet = rememberModalBottomSheetState()
    var showUserBottomSheet by remember { mutableStateOf(false) }
    val allMembers by vm.getAllMembers().collectAsState(initial = emptyList())

    LaunchedEffect(allMembers) {
        if(allMembers.isNotEmpty()) {
            vm.initUserList(allMembers.first { it.id == loggedUserId })
        }
    }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                            Text(
                                "Create new Team",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = {
                            actions.navigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp),
                                tint = Color.Black
                            )
                        }
                    },
                )
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .background(color = Color.White)
                .padding(16.dp)
        ) {

            //region TEAM PROFILE ICON
            Spacer(modifier = Modifier.padding(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp), // Puoi modificare l'altezza secondo le tue esigenze
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.size(100.dp, 100.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { showBottomSheet = true },
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF7879F1),//PURPLE
                                        Color(0xFFB4B5EC)
                                    )
                                ), shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Contenuto del Box
                        if (vm.teamobj.groupImageUri != Uri.EMPTY) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(vm.teamobj.groupImageUri)
                                    .build(),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.camera_icon),
                            contentDescription = "Camera Icon"
                        )
                    }
                }
            }
            //region TEAM NAME
            Text(
                text = "Name",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, top = 48.dp),
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextField(
                    value = vm.name,
                    onValueChange = {
                        if (it.length <= 20) {
                            vm.name = it
                        }
                    },
                    visualTransformation = VisualTransformation.None,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        disabledIndicatorColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(height = 60.dp)
                        .border(
                            width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(8.dp)
                        )
                )
                Text(
                    text = "${vm.name.length}/20",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            //region TEAM CATEGORY
            Text(
                text = "Category",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextField(
                    value = vm.category,
                    onValueChange = {
                        if (it.length <= 60) {
                            vm.category = it
                        }
                    },
                    visualTransformation = VisualTransformation.None,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        disabledIndicatorColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(height = 60.dp)
                        .border(
                            width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(8.dp)
                        )
                )

                Text(
                    text = "${vm.category.length}/60",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            //region DESCRIPTION
            Text(
                text = "Description",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                color = Color.Black
            )

            TextField(
                value = vm.description,
                onValueChange = { newValue -> vm.description = newValue },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White,
                    disabledIndicatorColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black
                ),
                singleLine = false,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(8.dp))
            )
            //region TEAM MEMBERS TITLE AND DELETE BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Team Members",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    modifier = Modifier.weight(1f),
                    color = Color.Black
                )
                if (vm.usersToRemove.isEmpty()){
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        onClick = {
                        },
                        enabled = false,
                        colors = IconButtonDefaults.iconButtonColors().copy(contentColor = Color.LightGray),
                        content = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove selected Members"
                            )
                        }
                    )
                }else{
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        onClick = {
                            vm.removeUser()
                        },
                        colors = IconButtonDefaults.iconButtonColors().copy(contentColor = Color.Red),
                        content = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove selected Members"
                            )
                        }
                    )
                }
            }
            //region TEAM MEMBERS CARDS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start
            ) {
                //region ADD NEW MEMBER CARD
                Card(
                    modifier = Modifier
                        .size(120.dp, 120.dp)
                        .padding(10.dp),
                    onClick = { showUserBottomSheet = true },
                    shape = CardDefaults.shape,
                    colors = CardDefaults.cardColors()
                        .copy(containerColor = Color("#FFFAFA".toColorInt())),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    //region ADD NEW BUTTON
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.simple_add_icon),
                            contentDescription = "Add Member",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    //region Add New Member Text
                    Text(
                        "Add New Member",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )
                }
                //region  TEAM MEMBERS LIST
                vm.userList.sortedByDescending { it.id == loggedUserId }.forEach { user ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = {
                                if (user.id != loggedUserId) {
                                    if (vm.usersToRemove.contains(user)) {
                                        vm.removeUserToRemove(user.id)
                                    } else {
                                        vm.addUserToRemove(user)
                                    }
                                }
                            },
                            enabled = user.id != loggedUserId,
                            shape = CardDefaults.shape,
                            colors =
                            CardDefaults.cardColors()
                                .copy(
                                    containerColor = Color("#FFFAFA".toColorInt()),
                                    disabledContainerColor = Color("#FFFAFA".toColorInt()),
                                    disabledContentColor = Color.Black
                                ),
                            border =
                            if (!vm.usersToRemove.contains(user)) {
                                BorderStroke(1.dp, Color.LightGray)
                            } else {
                                BorderStroke(2.dp, Color("#7879F1".toColorInt()))
                            },
                            elevation =
                            if (!vm.usersToRemove.contains(user)) {
                                CardDefaults.elevatedCardElevation()
                            } else {
                                CardDefaults.cardElevation(defaultElevation = 15.dp)
                            },
                            ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            //region USER PROFILE ICON
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user.profilePhotoUri).build(),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            //region USER NAME
                            Row(modifier = Modifier.fillMaxWidth()) {
                            if (user.id == loggedUserId) {
                                Image(
                                    painter = painterResource(id = R.drawable.id_crown_icon),
                                    contentDescription = "Owner",
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            Text(
                                user.nickname,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                            )
                        }
                        }
                }
            }
            //region USER BOTTOM SHEET
            if (showUserBottomSheet) {
                val memberList = allMembers.filter { !vm.userList.contains(it) }.filter{it.id != loggedUserId}
                ModalBottomSheet(
                    sheetState = userBottomSheet,
                    onDismissRequest = {
                        showUserBottomSheet = false
                        vm.usersToAdd = emptyList()
                    },
                ) {
                    UserBottomSheet(
                        vm,
                        memberList
                    )
                }
            }
            //region CREATE TEAM BUTTON
            //region CREATE TEAM BUTTON
            Button(
                onClick = {
                    // Verifica se il nome o la descrizione sono vuoti
                    if (vm.name.isBlank() || vm.description.isBlank() || vm.category.isBlank()){
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Verifica se la lista degli utenti è vuota
                    if (vm.userList.isEmpty()) {
                        Toast.makeText(context, "Please add team members", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Tutti i controlli passati, procedi con la creazione del team
                    vm.updateTeamObj()
                    vm.createNewTeam(vm.teamobj) { newTeamId ->
                        if (newTeamId != null) {
                            vm.addUsers(newTeamId, vm.userList)
                            actions.navigateBack()
                        } else {
                            // caso in cui la creazione del team abbia avuto esito negativo
                        }
                    }
                    vm.addNotificationToUsers(
                        vm.userIds,
                        Notification(
                            text = "You have just been added to the Team: ${vm.teamobj.name}",
                            readFlag = false,
                            creationDate = LocalDateTime.now()
                        )
                    )

                },
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Purple40)) {
                Text(
                    text = "Create Team",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White // Imposta il colore del testo
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UserBottomSheet(
    vm: NewTeamViewModel,
    allMembers: List<User>
) {
    val checkedStateList = remember { mutableStateListOf<Boolean>() }

    allMembers.forEach { _ ->
        checkedStateList.add(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        //region TITLE AND DONE BUTTON

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //region ADD NEW MEMBES TITLE
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),  // Aggiungo padding sinistro per centrare il testo
                text = "Add new members",
                color = Color.Black,
                fontSize = 20.sp,
                fontFamily = FontFamily(
                    Font(
                        R.font.relay_inter_bold
                    )
                )
            )

            //region DONE BUTTON
            if (vm.usersToAdd.isNotEmpty()) {
                IconButton(
                    onClick = {
                        vm.addUser()
                        checkedStateList.clear()
                        vm.usersToAdd = emptyList()
                    },
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add users",
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF7879F1),//PURPLE
                                        Color(0xFFB4B5EC)
                                    )
                                )
                            )
                    )
                }
            } else {
                IconButton(
                    onClick = {
                    },
                    enabled = false,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add users",
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFD3D3D3), //GRAY
                                        Color(0xFFA9A9A9)
                                    )
                                )
                            )
                    )
                }
            }
        }
        HorizontalDivider()

        //User List
        allMembers.forEachIndexed { index, user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(modifier = Modifier.weight(0.05f))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profilePhotoUri).build(),
                    contentDescription = "",
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF7879F1), Color(0xFFB4B5EC)
                                )
                            )
                        ),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.weight(0.05f))

                Column(
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = user.nickname,
                        modifier = Modifier,
                        fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                        fontSize = 20.sp,
                        maxLines = 1,
                        color = Color.Black,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.role,
                        fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                        color = Color("#A5A6F6".toColorInt()),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                //region CHECKBOX
                Checkbox(
                    modifier = Modifier.graphicsLayer(shape = CircleShape),
                    checked = checkedStateList[index],
                    colors = CheckboxDefaults.colors().copy(
                        checkedBoxColor = Purple40,
                        checkedCheckmarkColor = Color.White
                    ),
                    onCheckedChange = {
                        if (vm.usersToAdd.contains(user)) {
                            vm.removeUsertoAdd(user.id)
                        } else {
                            vm.addUsertoAdd(user)
                        }
                        checkedStateList[index] = it
                    })
                //endregion
            }
            //region HORIZONTAL DIVIDER
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp, color = Color.Gray
            )
        }
    }
}