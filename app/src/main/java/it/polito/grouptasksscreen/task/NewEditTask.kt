package it.polito.grouptasksscreen.task

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.History
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Notification
import it.polito.grouptasksscreen.model.Priority
import it.polito.grouptasksscreen.model.Status
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.task.Utils.DateUtils
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class NewEditTaskViewModel(val model: Model) : ViewModel() {

    /***
     *  MODEL FUNCTIONS AND STATE VARIABLES
     */

    var toEdit by mutableStateOf(false)
    fun getTaskById(taskId: String) = model.getTaskById(taskId)
    private fun addTask(task: Task) = model.addTask(task)
    private fun editTask(task: Task) = model.editTask(task)
    fun removeTask(id: String) = model.removeTask(id)
    fun getMembersByTeam (teamId: String) = model.getMembersByTeam(teamId)
    fun getTaskMembers(taskId: String): Flow<List<User>> = model.getTaskMembers(taskId)

    fun addNotificationToUsers(userIds: List<String>, notification: Notification) = model.addNotificationToUsers(userIds, notification)

    fun addHistoryToTask(taskId: String, history: History) = model.addHistoryToTask(taskId, history)
    /***
     *   STATE PROPERTIES
     */

    var id by mutableStateOf("")
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var dueDate: LocalDateTime by mutableStateOf(LocalDateTime.now())
    var tagList by mutableStateOf(listOf<String>())
    var status by mutableStateOf(Status.PENDING)
    private var userIds by mutableStateOf(listOf<String>())
    var priority by mutableStateOf(Priority.NO_PRIORITY)
    var hours by mutableIntStateOf(dueDate.hour)
    var minutes by mutableIntStateOf(dueDate.minute)
    var teamId by mutableStateOf("")


    //functions to initialize the viewModel when adding or editing a task
    fun initNewTask() {
        toEdit = false
        id = "-1"
        title = ""
        description = ""
        dueDate = LocalDateTime.now()
        tagList = listOf()
        userIds = listOf()
        status = Status.PENDING
        priority = Priority.NO_PRIORITY
        hours = 23
        minutes = 59
    }

    fun initEditTask(taskToEdit: Task) {
        id = taskToEdit.id
        toEdit = true
        title = taskToEdit.title
        description = taskToEdit.description
        dueDate = taskToEdit.dueDate
        tagList = taskToEdit.tagList
        userIds = taskToEdit.assignedUser
        status = taskToEdit.status
        priority = taskToEdit.priority
        hours = taskToEdit.dueDate.hour
        minutes = taskToEdit.dueDate.minute
    }


    //region SETTER FUNCTIONS
    fun setTmpId(newId: String) {
        teamId = newId
    }
    fun setTmpTitle(newTitle: String) {
        title = newTitle
    }

    fun setTmpDescription(newDescription: String) {
        description = newDescription
    }

    fun setTmpDueDate(newDueDate: LocalDateTime) {
        if(hours < 0 || hours > 24 || minutes < 0 || minutes > 59){
            dueDate = newDueDate
            statusError = ""
        }else {
            dueDate = newDueDate.withHour(hours).withMinute(minutes).atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            statusError = ""
        }
        if(dueDate.isBefore(LocalDateTime.now())||dueDate.isEqual(LocalDateTime.now())){
            status = Status.OVERDUE
            statusError = "Your task is overdue"
        }
        if(status == Status.OVERDUE && newDueDate.withHour(hours).withMinute(minutes).isAfter(LocalDateTime.now())){
            status = Status.IN_PROGRESS
            statusError = "You task is no more overdue"
        }

    }

    fun setTmpPriority(newPriority: Priority) {
        priority = newPriority
    }

    fun setTmpStatus(newStatus: Status) {
        if(dueDate.withHour(hours).withMinute(minutes).isBefore(LocalDateTime.now()) || dueDate.withHour(hours).withMinute(minutes).isEqual(LocalDateTime.now())){
            status = Status.OVERDUE
            statusError = "Your task is overdue"
        }else {
            if (newStatus == Status.OVERDUE) {
                statusError = "You still have time to complete your task"
            }else{
                status = newStatus
                statusError = ""
            }
        }
    }


    fun addTag(newTag: String) {
        if (newTag.isNotBlank()){
            val newTagLower = newTag.lowercase(Locale.getDefault()).trim()
            if (!tagList.contains(newTagLower)) {
                tagList += newTagLower
                tagListError = ""
            }else {
                tagListError = "This tag already exists"
            }
        }else{
            tagListError = ""
        }
    }

    fun removeTag(oldTag: String) {
        if (oldTag.isNotBlank() && tagList.contains(oldTag)) {
            tagList -= oldTag
        }
    }

    fun setTmpHour(newHours: Int) {
        hours = newHours
        if (newHours in 1..23) {
            dueDate = dueDate.withHour(newHours).atZone(ZoneId.systemDefault()).toLocalDateTime()
            hoursError = ""
        }else{
            hoursError = "Not valid value for hours"
        }
    }

    fun setTmpMinutes(newMinutes: Int) {
        minutes = newMinutes
        if (newMinutes in 1..58) {
            dueDate = dueDate.withMinute(newMinutes).atZone(ZoneId.systemDefault()).toLocalDateTime()
            minutesError = ""
        }else{
            minutesError = "Not valid value for minutes"
        }
    }
    //endregion


    //region ERROR CHECKING
    var titleError by mutableStateOf("")
        private set
    var statusError by mutableStateOf("")
        private set

    private fun checkTitle() {
        titleError = if (title.isBlank()) {
            "Title cannot be blank"
        } else if (title.contains("\n")) {
            "Title format not valid"
        } else ""
    }

    var descriptionError by mutableStateOf("")
        private set

    private fun checkDescription() {
        descriptionError = if (description.isEmpty()) {
            "Description cannot be blank"
        } else if (description.length > 500) {
            "Maximum length exceded"
        } else ""
    }

    var dueDateError by mutableStateOf("")
        private set

    private fun checkDueDate() {
        val currentDateTime = LocalDateTime.now()
        checkHours()
        checkMinutes()
        if (hoursError.isBlank() && minutesError.isBlank()) {
            val dueDateTime = LocalDateTime.of(dueDate.toLocalDate(), LocalTime.of(hours, minutes))
            dueDateError =
                if (dueDateTime.isBefore(currentDateTime) || dueDateTime.isEqual(currentDateTime)) {
                    "You cannot do time travels"
                } else ""
        }else{
            dueDateError = "Change hours and/or minutes"
        }

    }

    var hoursError by mutableStateOf("")
        private set

    private fun checkHours() {
        hoursError = if (hours < 0 || hours > 24) {
            "Not valid value for hours"
        } else ""
    }

    var minutesError by mutableStateOf("")
        private set

    private fun checkMinutes() {
        minutesError = if (minutes < 0 || minutes > 59) {
            "Not valid value for minutes"
        } else ""
    }

    var tagListError by mutableStateOf("")
        private set

    var generalError by mutableStateOf("")
        private set
    //endregion

    //region USER LIST
    var userList: List<User> by mutableStateOf(emptyList())
    var initialMembers: List<String> by mutableStateOf(emptyList())


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


    var history = listOf( History("",LocalDateTime.now()))
    fun validate(): Boolean {
        checkTitle()
        checkDescription()
        checkHours()
        checkMinutes()
        checkDueDate()
        if(toEdit){

        }else{
            history[0].text="Task Created"
            history[0].creationDate = LocalDateTime.now()
        }
        tagListError = ""
        if (
            titleError.isBlank() &&
            descriptionError.isBlank() &&
            dueDateError.isBlank() &&
            hoursError.isBlank() &&
            minutesError.isBlank()
        ) {
            generalError = ""
            statusError = ""

            val newTask = Task(
                id,
                title,
                description,
                userIds,
                listOf(),
                LocalDateTime.now(),
                dueDate,
                status,
                tagList,
                priority,
                teamId,
                history
            )
            if ( toEdit ){
                editTask(newTask)
                manageNotifications(newTask)
            }else{
                addTask(newTask)
                addNotificationToUsers(
                    newTask.assignedUser,
                    Notification(
                        text = "You have just been added to the Task: ${newTask.title}",
                        creationDate = LocalDateTime.now()
                    )
                    )
            }
            return true
        }else {
            generalError = "Please correct the errors"
            return false
        }
    }

    fun manageNotifications(task: Task) {

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
                text = "You have just been added to the Task: ${task.title}",
                creationDate = LocalDateTime.now()
            )
        )

        addNotificationToUsers(
            remNotification,
            Notification(
                text = "You have just been removed from Task: ${task.title}",
                creationDate = LocalDateTime.now()
            )
        )
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("StateFlowValueCalledInComposition", "UnusedMaterial3ScaffoldPaddingParameter",
    "UnrememberedMutableState"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEditTaskScreen(
    vm: NewEditTaskViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext) ),
    navController: NavController,
    taskId: String,
    teamId: String
    ){

    //region INITIALIZATION
        var task by remember { mutableStateOf(Task("-1")) }
        val teamMember by vm.getMembersByTeam(teamId).collectAsState(initial = emptyList())
        val actions = remember(navController) { Actions(navController) }
        var showDateDialog by remember { mutableStateOf(false) }


    var expandedStatus by remember { mutableStateOf(false) }
        var expandedPriority by remember { mutableStateOf(false) }
        //var dateState = rememberDatePickerState(null)
        val initialDateMillis = vm.dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dateState = remember { mutableStateOf(DatePickerState(initialSelectedDateMillis = initialDateMillis, locale = Locale.getDefault())) }
        val millisToLocalDate = dateState.value.selectedDateMillis?.let {
        DateUtils().convertMillisToLocalDate(it)
    }
        val dateToString = millisToLocalDate?.let {
        DateUtils().dateToString(millisToLocalDate)
    } ?: "Choose Date"

        vm.setTmpId(teamId)

        //endregion

    //region TAG FIELD
        var tagField by  mutableStateOf("")

        fun setTmpTagField(newTagField: String) {
            tagField = newTagField
        }
        //endregion

    LaunchedEffect(taskId) {
            if (taskId != "-1") {
                val taskDeferred = async { vm.getTaskById(taskId).first() }
                val usersDeferred = async { vm.getTaskMembers(taskId).first() }

                task = taskDeferred.await()
                vm.userList = usersDeferred.await()
                vm.initialMembers = vm.userList.map{ it.id }

                vm.initEditTask(task)

                val newDateMillis = vm.dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                dateState.value = DatePickerState(initialSelectedDateMillis = newDateMillis, locale = Locale.getDefault())

            } else {
                vm.initNewTask()
            }
        }
        


    //region ASSIGN USER BOTTOM SHEET
    val assignUserBottomSheetState = rememberModalBottomSheetState()
    var showAssignUserBottomSheet by remember { mutableStateOf(false) }

    //endregion


        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color("#FFFAFA".toColorInt())),
                        title = {
                                Text(
                                    text = if (vm.toEdit) "Edit Task" else "Create a Task",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                        },
                        navigationIcon = {
                            IconButton(onClick = { actions.navigateBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                }
            }
        ) {

            Column(
                modifier = Modifier
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.padding(16.dp))
                //region TITLE
                Text(
                    "Title",
                    //textAlign = TextAlign.Left,
                    //style = MaterialTheme.typography.headlineMedium
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp,top = 48.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = vm.title,
                        onValueChange = vm::setTmpTitle,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        isError = vm.titleError.isNotBlank(),
                        //visualTransformation = VisualTransformation.None,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.White,
                            disabledIndicatorColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .height(height = 60.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
                if (vm.titleError.isNotBlank()) {
                    Text(vm.titleError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion

                //region DESCRIPTION
                Text(
                    "Description",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = vm.description,
                        onValueChange = vm::setTmpDescription,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        isError = vm.descriptionError.isNotBlank(),
                        //visualTransformation = VisualTransformation.None,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = Color.Black
                        ),
                        singleLine = false,
                        maxLines = 5,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .height(height = 150.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
                if (vm.descriptionError.isNotBlank()) {
                    Text(vm.descriptionError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion
                //region DUE DATE
                Text(
                    "Due Date",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Text(
                    text = dateToString,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                )

                if (showDateDialog) {
                    DatePickerDialog(
                        onDismissRequest = { showDateDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDateDialog = false
                                    vm.setTmpDueDate(millisToLocalDate?.atTime(vm.hours, vm.minutes)!!)
                                }
                            ) {
                                Text(text = "OK")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showDateDialog = false }
                            ) {
                                Text(text = "Cancel")
                            }
                        }
                    ) {
                        DatePicker(
                            state = dateState.value,
                            showModeToggle = true
                        )
                    }
                }
                if (vm.dueDateError.isNotBlank()) {
                    Text(vm.dueDateError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(onClick = { showDateDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_calendar),
                            contentDescription = "edit_calendar"
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = vm.hours.toString(),
                        onValueChange = { newValue ->
                            vm.setTmpHour(newValue.toIntOrNull() ?: 0)
                        },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        //visualTransformation = VisualTransformation.None,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
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

                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = vm.minutes.toString(),
                        onValueChange = { newValue ->
                            vm.setTmpMinutes(newValue.toIntOrNull() ?: 0)
                        },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        //visualTransformation = VisualTransformation.None,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
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
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (vm.hoursError.isNotBlank()) {
                        Text(vm.hoursError, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    if (vm.minutesError.isNotBlank()) {
                        Text(vm.minutesError, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion

                //region STATUS
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = vm.status.toString(),
                        onValueChange = {
                            val value: Status = when (it) {
                                "PENDING" -> {
                                    Status.PENDING
                                }
                                "CHECKING" -> {
                                    Status.CHECKING
                                }
                                "COMPLETED" -> {
                                    Status.COMPLETED
                                }
                                else -> {
                                    Status.OVERDUE
                                }
                            }
                            vm.setTmpStatus(value)
                        },
                        enabled = expandedStatus,
                        //visualTransformation = VisualTransformation.None,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Black,
                            unfocusedIndicatorColor = Color.White,
                            disabledIndicatorColor = Color.White,
                            cursorColor = Color.Black,
                            disabledContainerColor = Color.White
                        ),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier
                            .background(Color.White)
                            .weight(1f)
                            .padding(end = 8.dp)
                            .height(height = 50.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { expandedStatus = true }
                    )
                    DropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White),

                    ) {
                        Status.entries.forEach { item ->
                            DropdownMenuItem(
                                colors = MenuDefaults.itemColors(disabledTrailingIconColor = Color.White),
                                text = {
                                    when (item) {
                                        Status.PENDING -> {
                                            Text("PENDING")
                                        }
                                        Status.CHECKING -> {
                                            Text("CHECKING")
                                        }
                                        Status.IN_PROGRESS -> {
                                            Text("IN PROGRESS")
                                        }
                                        Status.COMPLETED -> {
                                            Text("COMPLETED")
                                        }
                                        Status.OVERDUE -> {
                                            Text("OVERDUE")
                                        }
                                    }
                                },
                                onClick = {
                                    vm.setTmpStatus(item)
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (vm.statusError.isNotBlank()) {
                    Text(vm.statusError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion

                //region PRIORITY
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = vm.priority.toString(),
                        onValueChange = {
                            val value: Priority = when (it) {
                                "NO PRIORITY" -> {
                                    Priority.NO_PRIORITY
                                }
                                "LOW PRIORITY" -> {
                                    Priority.LOW_PRIORITY
                                }
                                "MEDIUM PRIORITY" -> {
                                    Priority.MEDIUM_PRIORITY
                                }
                                else -> {
                                    Priority.HIGH_PRIORITY
                                }
                            }
                            vm.setTmpPriority(value)
                        },
                        enabled = expandedPriority,
                        //visualTransformation = VisualTransformation.None,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.White,
                            disabledIndicatorColor = Color.White,
                            disabledContainerColor = Color.White,
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
                            .clickable { expandedPriority = true }
                    )
                    DropdownMenu(
                        expanded = expandedPriority,
                        onDismissRequest = { expandedPriority = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Priority.entries.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    when (item) {
                                        Priority.NO_PRIORITY -> {
                                            Text("NO PRIORITY")
                                        }
                                        Priority.LOW_PRIORITY -> {
                                            Text("LOW PRIORITY")
                                        }
                                        Priority.MEDIUM_PRIORITY -> {
                                            Text("MEDIUM PRIORITY")
                                        }
                                        else -> {
                                            Text("HIGH PRIORITY")
                                        }
                                    }
                                },
                                onClick = {
                                    vm.setTmpPriority(item)
                                    expandedPriority = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion


                //region TASK MEMBERS TITLE AND DELETE BUTTON
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text(
                        text = "Task Members",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 8.dp, top = 16.dp)
                            .weight(1f)
                    )
                    if (vm.usersToRemove.isEmpty()) {
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
                    } else {
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

                //endregion

                //region TASK MEMBERS CARDS
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
                        onClick = { showAssignUserBottomSheet = true },
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
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                        )
                        //endregion
                    }
                    //endregion

                    //region  TEAM MEMBERS LIST
                    vm.userList.forEach { user ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = {
                                if (vm.usersToRemove.contains(user)) {
                                    vm.removeUserToRemove(user.id)
                                } else {
                                    vm.addUserToRemove(user)
                                }
                            },
                            shape = CardDefaults.shape,
                            colors =
                            CardDefaults.cardColors()
                                .copy(containerColor = Color("#FFFAFA".toColorInt())),
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
                            Text(
                                user.nickname,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                            )
                            //endregion
                        }
                    }
                    //endregion
                }

                //endregion

                //region ASSIGN USER BOTTOM SHEET
                if (showAssignUserBottomSheet) {
                    val memberList = teamMember.filter { !vm.userList.contains(it) }
                    ModalBottomSheet(
                        sheetState = assignUserBottomSheetState,
                        onDismissRequest = {
                            showAssignUserBottomSheet = false
                            vm.usersToAdd = emptyList()
                        },
                    ) {
                        AssignUserBottomSheet(
                            vm,
                            memberList
                        )
                    }
                }
                //endregion


                //region TAG LIST
                Text(
                    text = "Tag List",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .background(Color("#FFFAFA".toColorInt()))
                        .clip(RoundedCornerShape(16.dp))
                        .padding(16.dp)

                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .background(Color("#FFFAFA".toColorInt()))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.tag_icon),
                            contentDescription = "tag_icon"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = tagField,
                            modifier = Modifier.weight(1f),
                            onValueChange = ::setTmpTagField,
                            placeholder = { Text("Add tag...") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            isError = vm.tagListError.isNotBlank(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.LightGray,
                                disabledTrailingIconColor = Color.Transparent,
                                focusedTrailingIconColor = Color.Transparent,
                                unfocusedTrailingIconColor = Color.Transparent
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        IconButton(onClick = {
                            vm.addTag(tagField)
                            setTmpTagField("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "add_tag",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = Color.Gray,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    vm.tagList.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color.LightGray,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.tag_icon),
                                    contentDescription = "tag_icon"
                                )
                            }
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                vm.removeTag(tag)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                        HorizontalDivider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                }
                if (vm.tagListError.isNotBlank()) {
                    Text(vm.tagListError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                //endregion

                //region BUTTONS
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { vm.validate() }, colors = ButtonDefaults.buttonColors(containerColor = Purple40)) {
                        Button(onClick = {
                            if (vm.validate()) {
                                actions.navigateBack()
                            }

                        }, colors = ButtonDefaults.buttonColors(containerColor = Purple40)) {
                            if (taskId == "-1") {
                                Text("Create Task")
                            } else {
                                Text("Edit Task")
                            }
                        }
                    }
                    if (vm.generalError.isNotBlank()) {
                        Text(vm.generalError, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (taskId != "-1") {
                        Button(
                            onClick = {
                                vm.removeTask(task.id)
                                actions.navigateBack()
                            },
                            colors = ButtonColors(Color.Red, Color.White, Color.DarkGray, Color.Gray)
                        ) {
                            Text("Delete Task")
                        }
                    }
                }
                //endregion
            }
        }
    }


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AssignUserBottomSheet(
    vm: NewEditTaskViewModel,
    allTeamUser: List<User>){

    val checkedStateList = remember { mutableStateListOf<Boolean>() }

    allTeamUser.forEach { _ ->
        checkedStateList.add(false)
    }


    Column(
        modifier = Modifier
            .background(Color.White)
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

        //region USER LIST
        allTeamUser.forEachIndexed { index, user ->
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
                        .background(Color.White)
                        .padding(start = 3.dp)
                        .weight(1f)
                ) {
                    //region NICKNAME
                    Text(
                        text = user.nickname,
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
                    colors = CheckboxDefaults.colors(checkedColor = Purple40, checkmarkColor = Color.White),
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
