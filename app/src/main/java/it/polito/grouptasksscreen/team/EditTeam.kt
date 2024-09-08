package it.polito.grouptasksscreen.team

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Notification
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.profile.createImageFile
import it.polito.grouptasksscreen.ui.theme.Purple40
import java.time.LocalDateTime
import java.util.Hashtable
import java.util.Objects


@RequiresApi(Build.VERSION_CODES.O)
class EditTeamViewModel(private val model: Model) : ViewModel() {
    //region TEAM APIs
    fun getTeamById(teamId: String) = model.getTeamById(teamId)
    fun getAllMembers() = model.getAllMembers()
    fun editTeam(teamId: String, teamobj: Team) = model.editTeam(teamId, teamobj)
    fun removeTeam(teamId: String) = model.removeTeam(teamId)
    private fun updateTeamPhoto(teamId: String, photoUri: Uri) = model.updateTeamPhoto(teamId, photoUri)

    fun addNotificationToUsers(userIds: List<String>, notification: Notification) = model.addNotificationToUsers(userIds, notification)
    //endregion

    //region PROFILE PHOTO
    private var permissionCheckResult by mutableIntStateOf(0)
    fun setpermissionCheckResult(perm: Int) {
        permissionCheckResult = perm
    }

    fun setProfilePhoto(teamId: String, photoUri: Uri?) {
        if (photoUri == null) {
            Log.d("xxxxxxx", "no photo selected")
        } else {
            updateTeamPhoto(teamId, photoUri)
        }
    }
    //endregion

    //region TEAM PROPERTIES
    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var category by mutableStateOf("")
    private var chatId by mutableStateOf("")
    private var nOfCompletedTask: Int = 0
    private var nOfTotalTask: Int = 0
    private var groupImageId: String = "profile1.png"
    private var userIds: List<String> by mutableStateOf(emptyList())
    var id: String = ""
    var creationDate: LocalDateTime = LocalDateTime.now()
    //endregion

    //region TEAM OBJECT INITIALIZATION
    var teamobj: Team = Team(
        id = "",
        name = "",
        category = "",
        description = "",
        groupImageId = "profile1.png",
        members = emptyList(),
        creationDate = LocalDateTime.MIN
    )

    fun initEditTeam(team: Team) {
        id = team.id
        name = team.name
        description = team.description
        category = team.category
        groupImageId = team.groupImageId
        userIds = team.members
        creationDate = team.creationDate
        chatId = team.chatId
    }
    //endregion


    //region USER LIST
    var userList: List<User> by mutableStateOf(emptyList())
    var initialMembers: List<String> by mutableStateOf(emptyList())

    fun initUserList(initUserlist: List<User>){
        userList = initUserlist
        initialMembers = initUserlist.map{ it.id }
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
        this.usersToRemove = emptyList()
    }


    //endregion

    //region REMOVE USER LIST
    var usersToRemove: List<User> by mutableStateOf(emptyList())
    fun addUserToRemove(userToAdd: User) {
        this.usersToRemove += userToAdd
    }

    fun removeUserToRemove(userId: String) {
        this.usersToRemove = this.usersToRemove.filter { it.id != userId }
    }
    //endregion

    //region ADD USER LIST
    var usersToAdd: List<User> by mutableStateOf(emptyList())

    fun addUsertoAdd(userToAdd: User) {
        this.usersToAdd += userToAdd
    }

    fun removeUsertoAdd(userId: String) {
        this.usersToAdd = this.usersToAdd.filter { it.id != userId }
    }
    //endregion


    //region UPDATE TEAM OBJECT
    fun updateTeamObj() {
        teamobj = Team(
            id = id, name = name, category = category, description = description,
            groupImageId = groupImageId, members = userIds,
            creationDate = creationDate, chatId = chatId
        )
    }
    //endregion

    fun manageNotifications() {

        val addNotification = mutableListOf<String>()
        val remNotification = mutableListOf<String>()

        userIds.forEach {
            if(!initialMembers.contains(it))
                addNotification.add(it)
        }

        initialMembers.forEach {
            if(!userIds.contains(it))
                remNotification.add(it)
        }

        addNotificationToUsers(
            addNotification,
            Notification(
                text = "You have just been added to team: ${teamobj.name}",
                creationDate = LocalDateTime.now()
            )
        )

        addNotificationToUsers(
            remNotification,
            Notification(
                text = "You have just been removed from team: ${teamobj.name}",
                creationDate = LocalDateTime.now()
            )
        )
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun EditTeamPane(
    vm: EditTeamViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    teamId: String,
    loggedUserId: String
) {

    //region INITIALIZATION
    val actions = remember(navController) {
        Actions(navController = navController)
    }

    val team by vm.getTeamById(teamId).collectAsState(initial = Team(id = "-1"))
    val allUsers by vm.getAllMembers().collectAsState(initial = emptyList())
    var showRemoveTeamDialog by remember { mutableStateOf(false) }
    //endregion



    LaunchedEffect (team,allUsers){
        if(team.id != "-1" && allUsers.isNotEmpty()) {
            vm.initEditTeam(team)


            val orderedUsers = allUsers.sortedWith(compareByDescending { it.id == team.id })
            vm.initUserList(orderedUsers.filter { it.id in team.members })
        }
    }

   



    //region PROFILE PHOTO UTILS
    //region PHOTO FROM GALLERY INITIALIZATIONS
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            vm.setProfilePhoto(teamId, it)
        }
    )
    //endregion

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
                vm.setProfilePhoto(teamId, uri)
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
    val photoSheetState = rememberModalBottomSheetState()

    var showPhotoBottomSheet by remember { mutableStateOf(false) }
    //endregion

    //endregion

    //region PROFILE ICON BOTTOM SHEET
    if (showPhotoBottomSheet) {
        ModalBottomSheet(
            containerColor = Color.White,
            onDismissRequest = {
                showPhotoBottomSheet = false
            },
            sheetState = photoSheetState
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
                    //endregion

                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Choose From Gallery",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                    //endregion
                }

            }
            //endregion

            //region CAMERA BUTTON
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier
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
                    //endregion

                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Take a photo",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                    //endregion
                }

            }
            //endregion

            //region CLOSE MENU
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = {
                        showPhotoBottomSheet = false
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
                    //endregion

                    //region TEXT
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Close Menu",
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.relay_inter_semibold))
                    )
                    //endregion
                }

            }
            //endregion


        }
    }
    //endregion
    //endregion

    //region ADD MEMBER BOTTOM SHEET
    val addMemberSheetState = rememberModalBottomSheetState()
    var showAddMemberBottomSheet by remember { mutableStateOf(false) }


    //endregion

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                            Text(
                                "Edit Team",
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
                .padding(16.dp) // Padding interno per spaziatura
        ) {

            Spacer(modifier = Modifier.padding(32.dp))

            //region TEAM IMAGE
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
                    onClick = { showPhotoBottomSheet = true },
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
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Contenuto del Box

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(team.groupImageUri)
                                .build(),
                            contentDescription = "",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.camera_icon),
                            contentDescription = "Camera Icon"
                        )

                    }
                }
            }
            //endregion

            //region TEAM NAME
            Text(
                text = "Name",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp, top = 48.dp)
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
                        .height(height = 50.dp)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                )

                Text(
                    text = "${vm.name.length}/20",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            //endregion

            //region TEAM CATEGORY
            Text(
                text = "Category",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
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
                        .height(height = 50.dp)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                )

                Text(
                    text = "${vm.category.length}/60",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            //endregion

            //region TEAM DESCRIPTION
            Text(
                text = "Description",
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(8.dp))
            )
            //endregion

            //region TEAM MEMBERS TITLE AND DELETE BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Team Members",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    modifier = Modifier.weight(1f)
                )
                if (vm.usersToRemove.isEmpty()){
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        onClick = {
                        },
                        enabled = false,
                        colors = IconButtonDefaults.iconButtonColors()
                            .copy(contentColor = Color.LightGray),
                        content = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove selected Members"
                            )
                        }
                    )
                }else {
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        onClick = {
                            vm.removeUser()
                        },
                        colors = IconButtonDefaults.iconButtonColors()
                            .copy(contentColor = Color.Red),
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
                    onClick = { showAddMemberBottomSheet = true },
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
                    //endregion

                    //region Add New Member Text
                    Text(
                        "Add New Member",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                    )
                    //endregion
                }
                //endregion

                //region  TEAM MEMBERS LIST
                vm.userList.sortedByDescending { it.id == team.ownerId }.forEach { user ->
                    Card(
                        modifier = Modifier
                            .size(120.dp, 120.dp)
                            .padding(10.dp),
                        onClick = {
                            if(user.id != loggedUserId) {
                                if (vm.usersToRemove.contains(user)) {
                                    vm.removeUserToRemove(user.id)
                                } else {
                                    vm.addUserToRemove(user)
                                }
                            }
                        },
                        enabled = user.id != team.ownerId,
                        shape = CardDefaults.shape,
                        colors =
                        CardDefaults.cardColors()
                            .copy(
                                containerColor = Color("#FFFAFA".toColorInt()),
                                disabledContainerColor = Color("#FFFAFA".toColorInt()),
                                disabledContentColor = Color.Black),
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
                                    .data(user.profilePhotoUri)
                                    .build(),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                        }
                        //endregion

                        //region USER NAME
                        Row(modifier = Modifier.fillMaxWidth()){
                            if(user.id == team.ownerId) {
                                Image(
                                    painter = painterResource(id = R.drawable.id_crown_icon),
                                    contentDescription = "Owner",
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            Text(
                                user.nickname,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                            )

                        }

                        //endregion


                        //endregion
                    }
                }
                //endregion
            }

            //endregion

            //region ADD MEMBER BOTTOM SHEET
            if (showAddMemberBottomSheet) {
                val memberList = allUsers.filter { it -> it.id !in vm.userList.map { it.id } }.filter { it.id != loggedUserId }
                ModalBottomSheet(
                    sheetState = addMemberSheetState,
                    onDismissRequest = {
                        showAddMemberBottomSheet = false
                        vm.usersToAdd = emptyList()
                    },
                ) {
                    AddMemberBottomSheet(
                        vm,
                        memberList,
                        team
                    )
                }
            }
            //endregion


            //region CONFIRM CHANGES BUTTON
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
                    vm.updateTeamObj()
                    vm.editTeam(teamId, vm.teamobj)
                    vm.manageNotifications()
                    actions.navigateBack()

                },
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(Purple40)
            ) {
                Text(
                    text = "Confirm Changes",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            //endregion

            //region DELETE TEAM
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                onClick = {
                    //todo permettere solo all'owner di cancellare il team
                    showRemoveTeamDialog = true
                },
                colors = ButtonDefaults.buttonColors(Purple40)
            ) {
                Text(
                    text = "Delete Team",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }



            if (showRemoveTeamDialog) {
                Dialog(
                    onDismissRequest = { showRemoveTeamDialog = false },
                    properties = DialogProperties(dismissOnClickOutside = true)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Delete Team",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Are you sure you want to delete the selected team? All tasks associated with the team will be deleted too.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showRemoveTeamDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        showRemoveTeamDialog = false
                                        //todo aggiungere controllo che solo l'owner può cancellare
                                        vm.removeTeam(teamId)
                                        actions.navigateBack()
                                    }
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            //endregion

        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddMemberBottomSheet(
    vm: EditTeamViewModel,
    allMembers: List<User>,
    team: Team
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
            //endregion

            //region DONE BUTTON
            if (vm.usersToAdd.isNotEmpty()) {
                IconButton(
                    onClick = {
                        vm.addUser()
                        checkedStateList.clear()
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
            //endregion
        }
        //endregion

        HorizontalDivider()

        //region INVITATION LINK
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.Center
        ) {

            CopyInvitationLinkText(team)

        }

        //endregion

        //region TEXT "OR"
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "or",
                style =
                TextStyle(
                    fontFamily = FontFamily(
                        Font(
                            R.font.relay_inter_bold
                        )
                    ),
                    fontSize = 20.sp,

                    ),
            )
        }
        //endregion

        //region QR CODE
        val qrCodeBitmap = generateQRCodeBitmap(team.invitationLink)

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (qrCodeBitmap != null && !qrCodeBitmap.isEmpty()) {
                Image(bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "qr" ,
                    modifier = Modifier.size(250.dp))
            }else{
                Icon(
                    painter = painterResource(id = R.drawable.baseline_qr_code_24),
                    contentDescription = "qr Code",
                    modifier = Modifier.size(250.dp)
                )
            }



        }
        //endregion

        //region USER LIST
        allMembers.forEachIndexed { index, user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                //region PROFILE IMAGE
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
                                    Color(0xFF7879F1),
                                    Color(0xFFB4B5EC)
                                )
                            )
                        ),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.weight(0.05f))
                //endregion

                //region USER INFORMATION
                Column(
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .weight(1f)
                ) {
                    //region NICKNAME
                    Text(
                        text = user.nickname,
                        color = Color.Black,
                        modifier = Modifier,
                        fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    //endregion

                    //region ROLE
                    Text(
                        text = user.role,
                        fontFamily = FontFamily(Font(R.font.relay_inter_regular)),
                        color = Color("#A5A6F6".toColorInt()),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    //endregion
                }
                //endregion

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

            //endregion

        }
        //endregion


    }
}

@Composable
fun CopyInvitationLinkText(team: Team) {
    val context = LocalContext.current

    // AnnotatedString per gestire lo stile del testo
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF7879F1))) {
            append("Copy invitation link")
        }
    }

    // Texto cliccabile
    ClickableText(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 16.sp,
        ),
        onClick = {
            // Copia il link di invito nella clipboard
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Invitation Link", team.invitationLink)
            clipboardManager.setPrimaryClip(clip)

            // Mostra un messaggio Toast per confermare la copia
            Toast.makeText(context, "Invitation link copied to clipboard", Toast.LENGTH_SHORT).show()
        },
    )
}


private fun generateQRCodeBitmap(invitationLink: String): Bitmap? {
    val hints = Hashtable<EncodeHintType, Any>()
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
    val writer = MultiFormatWriter()
    val bitMatrix: BitMatrix
    try {
        bitMatrix = writer.encode(invitationLink, BarcodeFormat.QR_CODE, 300, 300, hints)
    } catch (e: WriterException) {
        e.printStackTrace()
        return null
    }

    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

fun Bitmap.isEmpty(): Boolean {
    for (x in 0 until width) {
        for (y in 0 until height) {
            if (getPixel(x, y) != android.graphics.Color.TRANSPARENT) {
                return false
            }
        }
    }
    return true
}
