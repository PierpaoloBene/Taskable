package it.polito.grouptasksscreen.profile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
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
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.team.utils.BottomAppBar
import it.polito.grouptasksscreen.ui.theme.Purple80
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

const val primary_color = 0xFF061E51
const val secondary_color = 0xFF7879F1

class UserViewModel(private val model: Model) : ViewModel() {
    fun getMemberById(userId: String) = model.getMemberById(userId)
    private fun updateUser(userId: String, updatedUser: User) =
        model.updateUser(userId, updatedUser)

    fun getTeamByTeamId(teamId: String) = model.getTeamById(teamId)
    fun getTaskByUserId(userId: String) = model.getTasksByUser(userId)
    private fun uploadUserProfilePhoto(userId: String, photoUri: Uri) = model.uploadProfilePhoto(userId, photoUri)

    //region EDIT UTILITIES

    var updatedUser: User by mutableStateOf(User())
        private set

    fun setUpdateUser(user: User) {
        updatedUser = user
    }

    var isEditing by mutableStateOf(false)
        private set

    fun edit() {
        isEditing = true
    }


    fun validate() {
        checkFullName()
        checkNickname()
        checkEmail()
        checkTelephone()
        checkLocation()
        checkDescription()
        checkRole()
        checkMean()


        val errors = listOf(
            fullNameError, nicknameError, emailError, telephoneError,
            locationError, descriptionError, meanError, roleError, skillsError, achievementsError
        )
        Log.d("xxx", errors.toString())
        if (errors.all { it.isBlank() }) {
            skillsError = ""
            achievementsError = ""
            updateUser(updatedUser.id, updatedUser)
            isEditing = false
        }

    }
    //endregion


    //region PROFILE PHOTO


    private var permissionCheckResult by mutableIntStateOf(0)
    fun setpermissionCheckResult(perm: Int) {
        permissionCheckResult = perm
    }

    fun setProfilePhoto(photoUri: Uri?) {
        if (photoUri == null) {
            Log.d("xxxxxxx", "no photo selected")
        } else {
            uploadUserProfilePhoto(updatedUser.id, photoUri)
            updatedUser = updatedUser.copy(profilePhotoUri = photoUri)
        }

    }


    //endregion


    //region FULLNAME
    var fullNameError by mutableStateOf("")
        private set

    fun setFullName(fullName: String) {
        updatedUser = updatedUser.copy(fullName = fullName)
    }

    private fun checkFullName() {
        fullNameError = if (updatedUser.fullName.isBlank()) {
            "Name cannot be blank"
        } else if (updatedUser.fullName.contains("\n") || updatedUser.fullName.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]")) {
            "Name cannot contain special characters"
        } else ""
    }
    //endregion


    //region NICKNAME
    var nicknameError by mutableStateOf("")
        private set

    fun setNickName(nickname: String) {
        updatedUser = updatedUser.copy(nickname = nickname)
    }

    private fun checkNickname() {
        if (updatedUser.nickname.isBlank()) {
            nicknameError = "Nickname cannot be blank"
        } else if (updatedUser.nickname.contains("\n") || updatedUser.nickname.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]")) {
            fullNameError = "Nickname format not valid"
        } else nicknameError = ""

    }
    //endregion


    //region EMAIL
    var emailError by mutableStateOf("")
        private set

    fun setEmail(email: String) {
        updatedUser = updatedUser.copy(email = email)
    }

    private fun checkEmail() {
        emailError = if (updatedUser.email.isBlank()) {
            "Email cannot be blank"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(updatedUser.email).matches()) {
            "Invalid email format"
        } else ""
    }
    //endregion


    //region TELEPHONE
    var telephoneError by mutableStateOf("")
        private set

    fun setTelephone(telephone: String) {
        updatedUser = updatedUser.copy(telephone = telephone)
    }

    private fun checkTelephone() {
        telephoneError = if (updatedUser.telephone.isBlank()) {
            "Telephone cannot be blank"
        } else if (!Patterns.PHONE.matcher(updatedUser.telephone).matches()) {
            "Telephone number format not correct"
        } else if (updatedUser.telephone.length != 10) {
            "Telephone length is not correct"
        } else ""
    }
    //endregion


    //region LOCATION
    var locationError by mutableStateOf("")
        private set

    fun setLocation(location: String) {
        updatedUser = updatedUser.copy(location = location)
    }

    private fun checkLocation() {
        locationError = if (updatedUser.location.isBlank()) {
            "Location cannot be blank"
        } else ""
    }
    //endregion


    //region DESCRIPTION
    var descriptionError by mutableStateOf("")

    fun setDescription(newDescription: String) {
        updatedUser = updatedUser.copy(description = newDescription)
    }

    private fun checkDescription() {

        descriptionError = if (updatedUser.description.length > 500) {
            "Maximum length exceded"
        } else ""
    }
    //endregion


    //region ROLE
    var roleError by mutableStateOf("")
        private set

    fun setRole(role: String) {
        updatedUser = updatedUser.copy(role = role)
    }

    private fun checkRole() {
        roleError =
            if (updatedUser.role.contains("\n") || updatedUser.role.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]")) {
                "Role cannot contain special characters"
            } else ""
    }
    //endregion


    //region MEAN
    private var meanError by mutableStateOf("")

    private fun checkMean() {
        meanError = if (updatedUser.mean < 0) {
            "Mean cannot be a negative value"
        } else if (updatedUser.mean > 10) {
            "Mean cannot be greater than 10"
        } else ""
    }
    //endregion


    //region SKILLS
    var newskillValue by mutableStateOf("")
        private set
    var skillsError by mutableStateOf("")
        private set

    fun setSkillValue(value: String) {
        newskillValue = value
    }

    fun addSkill() {
        when {
            newskillValue.isBlank() -> skillsError = "Skill cannot be blank"
            updatedUser.skills.size >= 10 -> skillsError = "The maximum number of skill is 10"
            newskillValue.length > 50 -> skillsError =
                "Skill length cannot be greater than 50 characters"

            updatedUser.skills.contains(newskillValue) -> skillsError =
                "The Skill is already in the list"

            newskillValue.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]") -> skillsError =
                "Skill cannot contain special characters"

            newskillValue.contains("\n") -> skillsError = "Skill cannot contain new line"
            else -> {
                updatedUser = updatedUser.copy(skills = updatedUser.skills + newskillValue)
                updateUser(updatedUser.id, updatedUser)
                skillsError = ""
            }
        }
    }

    fun removeSkill(skillToRemove: String) {
        updatedUser = updatedUser.copy(skills = updatedUser.skills - skillToRemove)
        updateUser(updatedUser.id, updatedUser)

    }

    fun checkSkill() {
        skillsError =
            if (newskillValue.contains("\n") || newskillValue.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]")) {
                "Skill cannot contain special characters"
            } else {
                ""
            }
        Log.d("xxx", skillsError)
    }

    //endregion


    //region ACHIEVEMENTS
    var achiviementsValue by mutableStateOf("")
    var achievementsError by mutableStateOf("")
        private set

    fun setAchievementValue(value: String) {
        achiviementsValue = value
    }

    fun addAchievement() {
        when {
            achiviementsValue.isBlank() -> achievementsError = "Achievement cannot be blank"
            updatedUser.achievements.size >= 10 -> achievementsError =
                "The maximum number of achievements is 10"

            achiviementsValue.length > 50 -> achievementsError =
                "Achievement length cannot be greater than 50 characters"

            updatedUser.achievements.contains(achiviementsValue) -> achievementsError =
                "The achievement is already in the list"

            achiviementsValue.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]") -> achievementsError =
                "Skill cannot contain special characters"

            achiviementsValue.contains("\n") -> achievementsError = "Skill cannot contain new line"

            else -> {
                updatedUser =
                    updatedUser.copy(achievements = updatedUser.achievements + achiviementsValue)
                updateUser(updatedUser.id, updatedUser)
                achievementsError = ""
            }
        }
    }

    fun removeAchievement(achToRemove: String) {
        updatedUser = updatedUser.copy(achievements = updatedUser.achievements - achToRemove)
        updateUser(updatedUser.id, updatedUser)

    }

    fun checkAchivements() {
        achievementsError =
            if (achiviementsValue.contains("\n") || achiviementsValue.contains("^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð ,.'-]")) {
                "Skill cannot contain special characters"
            } else {
                ""
            }
        Log.d("xxx", achievementsError)
    }

    //endregion

}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfile(
    vm: UserViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    userId: String
) {

    //region INITIALIZATION
    val actions = remember(navController) {
        Actions(navController = navController)
    }

    val user by vm.getMemberById(userId).collectAsState(initial = User())
    val taskList by vm.getTaskByUserId(userId).collectAsState(initial = emptyList())
    var teamList: List<Team> = emptyList()

    user.teams.forEach {
        teamList = teamList + vm.getTeamByTeamId(it).collectAsState(initial = Team()).value
    }


    //endregion

    //region PROFILE VIEW OR CAMERA VIEW
    if (vm.updatedUser == User()) {
        vm.setUpdateUser(user)
    }
    if (user == User()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(128.dp),
                    color = Color(0xFF7F56D9),
                    trackColor = Color(0xFFB4B5EC),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            //region TOP BAR
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = user.nickname,
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
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    if (loggedUserId.isNotEmpty()) {
                        //region ICONA CHAT
                        if (loggedUserId != user.id) {
                            IconButton(
                                onClick = { actions.navigateToPersonalChat(loggedUserId, user.id) }
                                ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.team_card_chat_icon),
                                    contentDescription = "Chat",
                                    tint = Color.Black
                                )
                            }
                        }
                        //endregion
                        //region ICONA EDIT
                        else {
                            IconButton(onClick = {
                                if (vm.isEditing) {
                                    vm.validate()
                                } else {
                                    vm.edit()
                                }
                            }) {
                                Icon(
                                    imageVector = if (!vm.isEditing) Icons.Rounded.Edit else Icons.Default.Done,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Black
                                )
                            }
                        }
                        //endregion
                    }

                }
            )
            HorizontalDivider()
            //endregion

            if (vm.isEditing) {
                EditPane(vm, teamList, taskList)
            } else {
                PresentationPane(vm,navController,loggedUserId, vm.updatedUser, actions, teamList, taskList)
            }
        }

    }

    //endregion
}


@Composable
fun PresentationPane(
    vm: UserViewModel,
    navController: NavController,
    loggedUserId: String,
    user: User,
    actions: Actions,
    teamList: List<Team>,
    taskList: List<Task>
) {

    Scaffold(
        bottomBar = { BottomAppBar(navController, loggedUserId) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .background(color = Color.White)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            //region PROFILE ICON
            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .fillMaxHeight(0.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ProfileIcon(
                    firstName = user.fullName.substringBefore(' ').trim(),
                    lastName = user.fullName.substringAfter(' ').trim(),
                    size = DpSize(100.dp, 100.dp),
                    user.profilePhotoUri,
                    false
                )
            }
            //endregion

            //region FULL NAME
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    user.fullName,
                    fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                    fontSize = 35.sp,
                    modifier = Modifier.padding(16.dp, 0.dp),
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            //endregion

            //region TITLE "PERSONAL INFO"
            Text(
                text = "PERSONAL INFO",
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                color = Color.Black
            )
            //endregion

            //region PERSONAL INFO CARD
            Card(
                modifier = Modifier
                    .padding(16.dp, 10.dp)
                    .fillMaxWidth()
                    .shadow(0.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                border = BorderStroke(1.dp, Color.LightGray),
            ) {
                //region NICKNAME
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "nickname",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )
                    Text(
                        user.nickname,
                        modifier = Modifier.padding(16.dp, 0.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region EMAIL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = "nickname",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )
                    Text(
                        user.email,
                        modifier = Modifier.padding(16.dp, 0.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region TELEPHONE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "telephone",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )
                    Text(
                        user.telephone,
                        modifier = Modifier.padding(16.dp, 0.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region LOCATION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "location",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )
                    Text(
                        user.location,
                        modifier = Modifier.padding(16.dp, 16.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                //endregion
            }
            //endregion

            //region TITLE "DESCRIPTION"
            if (user.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DESCRIPTION",
                    fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                    color = Color.Black
                )

                Card(
                    modifier = Modifier
                        .padding(16.dp, 10.dp)
                        .fillMaxWidth()
                        .shadow(0.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    ),
                    border = BorderStroke(1.dp, Color.LightGray),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 5.dp, 0.dp, 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "description",
                            modifier = Modifier.size(40.dp),
                            tint = Color("#7879F1".toColorInt())
                        )
                        Text(
                            user.description,
                            modifier = Modifier.padding(16.dp, 0.dp),
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                            color = Color.Black
                        )
                    }
                }

            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ACTIVITY",
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                color = Color.Black
            )
            //endregion

            //region ACTIVITY CARD
            Card(
                modifier = Modifier
                    .padding(16.dp, 10.dp)
                    .fillMaxWidth()
                    .shadow(0.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                border = BorderStroke(1.dp, Color.LightGray),
            ) {
                //region ROLE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.work_icon),
                        contentDescription = "role",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )
                    Text(
                        user.role,
                        modifier = Modifier.padding(16.dp, 16.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region TEAMS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        painter = painterResource(id = R.drawable.teams_icon),
                        contentDescription = "Teams",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )

                    Text(
                        "Teams: " + user.teams.size,
                        modifier = Modifier.padding(16.dp, 16.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region TASKS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.task_icon),
                        contentDescription = "Tasks percentage",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())
                    )

                    Text(
                        "Assigned Tasks : " + taskList.size,
                        modifier = Modifier.padding(16.dp, 16.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )

                }
                HorizontalDivider()
                //endregion

                //region RATINGS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_bar_chart_24),
                        contentDescription = "Ratings",
                        modifier = Modifier.size(40.dp),
                        tint = Color("#7879F1".toColorInt())

                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Rating :",
                        modifier = Modifier.padding(16.dp, 16.dp),
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                        color = Color.Black
                    )
                    //var flag = true
                    for (i in 1..5) {
                        if (i <= vm.updatedUser.mean /*/ 2*/) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "fill star $i",
                                modifier = Modifier.size(32.dp),
                                Color(secondary_color)
                            )
                        }/* else if (user.mean % 2 != 0 && flag) {
                            flag = false
                            Icon(
                                painter = painterResource(id = R.drawable.half_star),
                                contentDescription = "half star $i",
                                modifier = Modifier.size(32.dp),
                                Color(secondary_color)
                            )
                        }*/ else if (i > vm.updatedUser.mean /*/ 2*/) {
                            Icon(
                                painter = painterResource(id = R.drawable.empty_star),
                                contentDescription = "empty star $i",
                                modifier = Modifier.size(32.dp),
                                Color(secondary_color)
                            )
                        }
                    }
                }
                HorizontalDivider()
                //endregion
            }
            Spacer(modifier = Modifier.height(10.dp))
            //endregion

            //region SKILLS
            if (user.skills.isNotEmpty()) {

                //region SKILLS TITLE
                Text(
                    text = "SKILLS", // Il titolo della sezione
                    fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(start = 32.dp, bottom = 8.dp)
                        .align(Alignment.Start),
                    color = Color.Black
                )
                //endregion

                //region SKILLS CARD
                Card(
                    modifier = Modifier
                        .padding(16.dp, 10.dp)
                        .fillMaxWidth()
                        .shadow(0.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    ),
                    border = BorderStroke(1.dp, Color.LightGray),
                ) {


                    for (skill in user.skills) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 5.dp, 0.dp, 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.skill_icon),
                                contentDescription = "role",
                                modifier = Modifier.size(40.dp),
                                tint = Color("#7879F1".toColorInt())
                            )
                            Text(
                                skill,
                                modifier = Modifier.padding(16.dp, 16.dp),
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                                color = Color.Black
                            )

                        }
                        HorizontalDivider()

                    }
                }
                //endregion

            }
            Spacer(modifier = Modifier.height(10.dp))
            //endregion

            //region ACHIEVEMENTS
            if (user.achievements.isNotEmpty()) {

                //region ACHIEVEMENT TITLE
                Text(
                    text = "ACHIEVEMENT",
                    fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                    color = Color.Black
                )
                //endregion

                //region ACHIEVEMENT CARD
                Card(
                    modifier = Modifier
                        .padding(16.dp, 10.dp)
                        .fillMaxWidth()
                        .shadow(0.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    ),
                    border = BorderStroke(1.dp, Color.LightGray),
                ) {

                    for (ach in user.achievements) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 5.dp, 0.dp, 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.ach_icon),
                                contentDescription = "role",
                                modifier = Modifier.size(40.dp),
                                tint = Color("#7879F1".toColorInt())
                            )
                            Text(
                                ach,
                                modifier = Modifier.padding(16.dp, 16.dp),
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                                color = Color.Black
                            )

                        }
                        HorizontalDivider()
                    }
                }
                //endregion


            }
            Spacer(modifier = Modifier.height(10.dp))
            //endregion

            //region TEAMS

            //region TITLE "TEAMS"
            Text(
                text = "TEAMS",
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                color = Color.Black
            )
            //endregion

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start
            ) {
                if(teamList.isNotEmpty()){
                    teamList.forEach { team ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = { actions.navigateToTeamDetails(loggedUserId,team.id,"false") },
                            shape = CardDefaults.shape,
                            colors = CardDefaults.cardColors()
                                .copy(containerColor = Color("#FFFAFA".toColorInt())),
                            border = BorderStroke(1.dp, Color.LightGray),
                            elevation = CardDefaults.elevatedCardElevation()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            //region TEAM PROFILE ICON
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(team.groupImageUri)
                                        .build(),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            //endregion

                            //region TEAM NAME
                            Text(
                                team.name,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                                color = Color.Black
                            )
                            //endregion
                        }
                    }
                }
                else{
                    Text(
                        text = "No teams for now",
                        fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                        color = Color.Black
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            //endregion

            //region TASKS

            //region TITLE "TASKS"
            Text(
                text = "TASKS",
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                color = Color.Black
            )
            //endregion


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start
            ) {
                if(taskList.isNotEmpty()) {
                    taskList.forEach { task ->
                        //region TASKS CARD
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = { actions.navigateToTaskDetail(loggedUserId, task.id) },
                            shape = CardDefaults.shape,
                            colors = CardDefaults.cardColors()
                                .copy(containerColor = Color("#FFFAFA".toColorInt())),
                            border = BorderStroke(1.dp, Color.LightGray),
                            elevation = CardDefaults.elevatedCardElevation()
                        ) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                //region TASK TITLE
                                Text(
                                    task.title,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                                    color = Color.Black
                                )
                                //endregion
                            }

                        }
                        //endregion
                    }
                }else{
                    Text(
                        text = "No tasks assigned for now",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                        color = Color.Black
                    )
                }
            }
            //endregion


        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPane(vm: UserViewModel, teamList: List<Team>, taskList: List<Task>) {

    //region PHOTO FROM GALLERY INITIALIZATIONS
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            vm.setProfilePhoto(it)
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
    //endregion

    //endregion

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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        //region PROFILE ICON
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(0.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.size(100.dp, 100.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = { showBottomSheet = true },
                shape = CircleShape
            ) {
                ProfileIcon(
                    firstName = vm.updatedUser.fullName.substringBefore(' ').trim(),
                    lastName = vm.updatedUser.fullName.substringAfter(' ').trim(),
                    size = DpSize(100.dp, 100.dp),
                    vm.updatedUser.profilePhotoUri,
                    true
                )
            }

        }
        //endregion

        //region FULL NAME
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            TextField(
                value = vm.updatedUser.fullName,
                onValueChange = { vm.setFullName(it) },
                isError = vm.fullNameError.isNotBlank(),
                placeholder = {
                    if (vm.fullNameError.isNotBlank())
                        Text(vm.fullNameError)
                    else
                        Text("Full Name")
                },
                textStyle = TextStyle.Default.copy(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.relay_inter_bold))
                ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color("#FFFFFF".toColorInt()),
                    focusedContainerColor = Color("#E7EAEE".toColorInt()),
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        //endregion

        //region TITLE "PERSONAL INFO"
        Text(
            text = "PERSONAL INFO",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            color = Color.Gray
        )
        //endregion

        //region PERSONAL INFO CARD
        Card(
            modifier = Modifier
                .padding(16.dp, 10.dp)
                .fillMaxWidth()
                .shadow(0.dp)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color("#FFFAFA".toColorInt())
            ),
            border = BorderStroke(1.dp, Color.LightGray),
        ) {
            //region NICKNAME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                //region ICON
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "nickname",
                    modifier = Modifier.size(40.dp),
                    tint = if (vm.nicknameError.isNotBlank()) Color.Red else Color("#7879F1".toColorInt())
                )
                //endregion

                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.nickname,
                    onValueChange = { vm.setNickName(it) },
                    isError = vm.nicknameError.isNotBlank(),
                    placeholder = {
                        if (vm.nicknameError.isNotBlank())
                            Text(vm.nicknameError)
                        else
                            Text("Nickname")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    )
                )
                //endregion

            }
            HorizontalDivider()
            //endregion

            //region EMAIL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //region ICON
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = "nickname",
                    modifier = Modifier.size(40.dp),
                    tint = if (vm.emailError.isNotBlank()) Color.Red else Color("#7879F1".toColorInt())
                )
                //endregion

                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.email,
                    onValueChange = { vm.setEmail(it) },
                    isError = vm.emailError.isNotBlank(),
                    placeholder = {
                        if (vm.emailError.isNotBlank())
                            Text(vm.emailError)
                        else
                            Text("Email")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    )
                )
                //endregion


            }
            HorizontalDivider()
            //endregion

            //region TELEPHONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                //region ICON
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "telephone",
                    modifier = Modifier.size(40.dp),
                    tint = if (vm.telephoneError.isNotBlank()) Color.Red else Color("#7879F1".toColorInt())
                )
                //endregion

                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.telephone,
                    onValueChange = { vm.setTelephone(it) },
                    isError = vm.telephoneError.isNotBlank(),
                    placeholder = {
                        if (vm.telephoneError.isNotBlank())
                            Text(vm.telephoneError)
                        else
                            Text("Telephone")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    )
                )
                //endregion

            }
            HorizontalDivider()
            //endregion

            //region LOCATION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                //region ICON
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(40.dp),
                    tint = if (vm.locationError.isNotBlank()) Color.Red else Color("#7879F1".toColorInt())
                )
                //endregion

                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.location,
                    onValueChange = { vm.setLocation(it) },
                    isError = vm.locationError.isNotBlank(),
                    placeholder = {
                        if (vm.locationError.isNotBlank())
                            Text(vm.locationError)
                        else
                            Text("Location")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    )
                )
                //endregion

            }
            //endregion
        }
        //endregion

        //region DESCRIPTION
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DESCRPITION",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            color = Color.Gray
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp, 10.dp)
                    .fillMaxWidth()
                    .shadow(0.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                border = BorderStroke(1.dp, Color.LightGray),
            ) {
                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.description,
                    onValueChange = { vm.setDescription(it) },
                    modifier = Modifier
                        .padding(16.dp, 16.dp),
                    isError = vm.descriptionError.isNotBlank(),
                    placeholder = {
                        if (vm.descriptionError.isNotBlank())
                            Text(vm.descriptionError)
                        else
                            Text("Description")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    ),
                )
                //endregion
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ACTIVITY",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            color = Color.Gray
        )
        //endregion

        //region ACTIVITY CARD
        Card(
            modifier = Modifier
                .padding(16.dp, 10.dp)
                .fillMaxWidth()
                .shadow(0.dp)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color("#FFFAFA".toColorInt())
            ),
            border = BorderStroke(1.dp, Color.LightGray),
        ) {
            //region ROLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                //region ICON
                Icon(
                    painter = painterResource(id = R.drawable.work_icon),
                    contentDescription = "role",
                    modifier = Modifier.size(40.dp),
                    tint = if (vm.nicknameError.isNotBlank()) Color.Red else Color("#7879F1".toColorInt())
                )
                //endregion

                //region TEXTFIELD
                TextField(
                    value = vm.updatedUser.role,
                    onValueChange = { vm.setRole(it) },
                    isError = vm.roleError.isNotBlank(),
                    placeholder = {
                        if (vm.roleError.isNotBlank())
                            Text(vm.roleError)
                        else
                            Text("Role")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                        focusedContainerColor = Color("#E7EAEE".toColorInt()),
                        errorCursorColor = Color.Red
                    )
                )
                //endregion

            }
            HorizontalDivider()
            //endregion

            //region TEAMS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    painter = painterResource(id = R.drawable.teams_icon),
                    contentDescription = "Teams",
                    modifier = Modifier.size(40.dp),
                    tint = Color("#7879F1".toColorInt())
                )

                Text(
                    "Teams: " + vm.updatedUser.teams.size,
                    modifier = Modifier.padding(16.dp, 16.dp),
                    fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                )

            }
            HorizontalDivider()
            //endregion

            //region TASKS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.task_icon),
                    contentDescription = "Tasks percentage",
                    modifier = Modifier.size(40.dp),
                    tint = Color("#7879F1".toColorInt())
                )

                Text(
                    "Assigned Tasks : " + taskList.size,
                    modifier = Modifier.padding(16.dp, 16.dp),
                    fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                )

            }
            HorizontalDivider()
            //endregion

            //region RATINGS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 5.dp, 0.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_bar_chart_24),
                    contentDescription = "Ratings",
                    modifier = Modifier.size(40.dp),
                    tint = Color("#7879F1".toColorInt())

                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Rating :",
                    modifier = Modifier.padding(16.dp, 16.dp),
                    fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                )
                //var flag = true
                for (i in 1..5) {
                    if (i <= vm.updatedUser.mean /*/ 2*/) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "fill star $i",
                            modifier = Modifier.size(32.dp),
                            Color(secondary_color)
                        )
                    }/* else if (vm.updatedUser.mean % 2 != 0 && flag) {
                        flag = false
                        Icon(
                            painter = painterResource(id = R.drawable.half_star),
                            contentDescription = "half star $i",
                            modifier = Modifier.size(32.dp),
                            Color(secondary_color)
                        )
                    }*/ else if (i > vm.updatedUser.mean /*/ 2*/) {
                        Icon(
                            painter = painterResource(id = R.drawable.empty_star),
                            contentDescription = "empty star $i",
                            modifier = Modifier.size(32.dp),
                            Color(secondary_color)
                        )
                    }
                }
            }
            HorizontalDivider()
            //endregion
        }
        Spacer(modifier = Modifier.height(10.dp))
        //endregion

        //region SKILLS

            //region SKILLS TITLE
            Text(
                text = "SKILLS", // Il titolo della sezione
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(start = 32.dp, bottom = 8.dp)
                    .align(Alignment.Start),
                color = Color.Gray
            )
            //endregion

            //region SKILLS CARD
            Card(
                modifier = Modifier
                    .padding(16.dp, 10.dp)
                    .fillMaxWidth()
                    .shadow(0.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                border = BorderStroke(1.dp, Color.LightGray),
            ) {

                //region USER SKILL LOOP
                for (skill in vm.updatedUser.skills) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 5.dp, 0.dp, 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        //region ICON BUTTON
                        IconButton(onClick = { vm.removeSkill(skill) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.remove_icon),
                                contentDescription = "role",
                                modifier = Modifier.size(40.dp),
                                tint = Color("#7879F1".toColorInt())
                            )
                        }

                        //endregion

                        //region TEXT
                        Text(
                            skill,
                            modifier = Modifier.padding(16.dp, 16.dp),
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                        )
                        //endregion

                    }
                    HorizontalDivider()
                }

                //endregion

                //region NEW SKILL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    //region ICON Button
                    IconButton(onClick = {
                        vm.checkSkill()
                        if (vm.skillsError.isBlank()) {
                            vm.addSkill()
                            vm.setSkillValue("")
                        }

                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "role",
                            modifier = Modifier.size(40.dp),
                            tint = Color("#7879F1".toColorInt())
                        )
                    }
                    //endregion

                    //region TEXTFIELD
                    TextField(
                        value = vm.newskillValue,
                        onValueChange = { vm.setSkillValue(it) },
                        isError = vm.skillsError.isNotBlank(),
                        placeholder = {
                            if (vm.skillsError.isNotBlank())
                                Text(vm.skillsError)
                            else
                                Text("Add a new skill")
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                            focusedContainerColor = Color("#E7EAEE".toColorInt()),
                            errorCursorColor = Color.Red
                        )
                    )
                    //endregion

                }
                HorizontalDivider()
                //endregion
            }
            //endregion

        Spacer(modifier = Modifier.height(10.dp))
        //endregion

        //region ACHIEVEMENTS

            //region ACHIEVEMENT TITLE
            Text(
                text = "ACHIEVEMENT",
                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                color = Color.Gray
            )
            //endregion

            //region ACHIEVEMENT CARD
            Card(
                modifier = Modifier
                    .padding(16.dp, 10.dp)
                    .fillMaxWidth()
                    .shadow(0.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                border = BorderStroke(1.dp, Color.LightGray),
            ) {

                //region USER ACHIEVEMENT
                for (ach in vm.updatedUser.achievements) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 5.dp, 0.dp, 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        //region ICON BUTTON
                        IconButton(
                            onClick = {
                                vm.removeAchievement(ach)
                            }) {
                            Icon(
                                painter = painterResource(id = R.drawable.remove_icon),
                                contentDescription = "role",
                                modifier = Modifier.size(40.dp),
                                tint = Color("#7879F1".toColorInt())
                            )
                        }
                        //endregion

                        //region TEXT
                        Text(
                            ach,
                            modifier = Modifier.padding(16.dp, 16.dp),
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                        )
                        //endregion

                    }
                    HorizontalDivider()
                }
                //endregion

                //region NEW ACHIEVEMENT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 5.dp, 0.dp, 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    //region ICON Button
                    IconButton(onClick = {
                        vm.checkAchivements()
                        if (vm.achievementsError.isBlank()) {
                            vm.addAchievement()
                            vm.setAchievementValue("")
                        }

                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "role",
                            modifier = Modifier.size(40.dp),
                            tint = Color("#7879F1".toColorInt())
                        )
                    }
                    //endregion

                    //region TEXTFIELD
                    TextField(
                        value = vm.achiviementsValue,
                        onValueChange = { vm.setAchievementValue(it) },
                        isError = vm.achievementsError.isNotBlank(),
                        placeholder = {
                            if (vm.achievementsError.isNotBlank())
                                Text(vm.achievementsError)
                            else
                                Text("Add a new achievement")
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color("#FFFAFA".toColorInt()),
                            focusedContainerColor = Color("#E7EAEE".toColorInt()),
                            errorCursorColor = Color.Red
                        )
                    )
                    //endregion

                }
                HorizontalDivider()
                //endregion
            }
            //endregion

        Spacer(modifier = Modifier.height(10.dp))
        //endregion

        //region TEAMS

        //region TITLE "TEAMS"
        Text(
            text = "TEAMS",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            color = Color.Gray
        )
        //endregion

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            teamList.forEach { team ->
                Card(
                    modifier = Modifier
                        .size(120.dp, 120.dp)
                        .padding(10.dp),
                    shape = CardDefaults.shape,
                    colors = CardDefaults.cardColors()
                        .copy(containerColor = Color("#FFFAFA".toColorInt())),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    //region TEAM PROFILE ICON
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(team.groupImageUri)
                                .build(),
                            contentDescription = "",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    //endregion

                    //region TEAM NAME
                    Text(
                        team.name,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                    )
                    //endregion
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        //endregion

        //region TASKS

        //region TITLE "TASKS"
        Text(
            text = "TASKS",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            color = Color.Gray
        )
        //endregion


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            taskList.forEach { task ->
                //region TASKS CARD
                Card(
                    modifier = Modifier
                        .size(120.dp, 120.dp)
                        .padding(10.dp),
                    shape = CardDefaults.shape,
                    colors = CardDefaults.cardColors()
                        .copy(containerColor = Color("#FFFAFA".toColorInt())),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        //region TASK TITLE
                        Text(
                            task.title,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily(Font(R.font.relay_inter_bold))
                        )
                        //endregion
                    }

                }
                //endregion
            }
        }
        Spacer(Modifier.height(10.dp))
        //endregion


    }


}


@SuppressLint("SimpleDateFormat")
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH:mm:ss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    return File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )

}

