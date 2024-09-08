package it.polito.grouptasksscreen.task


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Status
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.task.utils.FilterBottomSheet
import it.polito.grouptasksscreen.task.utils.SortingBottomSheet
import it.polito.grouptasksscreen.taskcard.TaskCard
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.LocalDateTime


@RequiresApi(Build.VERSION_CODES.O)
class TaskListViewModel(private val model: Model) : ViewModel() {

    //apis
    fun getTasksByTeam(teamId: String,sorting: String?=null) = model.getTasksByTeam(teamId,sorting)

    fun getMembers(userIds: List<String>) = model.getMembers(userIds)
    fun getAllMembers() = model.getAllMembers()
    fun getAllTag() = model.getAllTag()
    fun removeTask(taskId: String) = model.removeTask(taskId)

    fun removeReviewFromUsers(userIds: List<String>, taskId: String) = model.removeReviewFromUsers(userIds, taskId)


    //UTILS FOR FILTERING
    val userfilterStates = mutableStateListOf<Boolean>()
    val statusfilterStates = mutableStateListOf<Boolean>()
    val tagfilterStates = mutableStateListOf<Boolean>()


    fun filteringMode(): Boolean{
        val booleanList = userfilterStates.filter{ it }.toMutableList()
        booleanList += statusfilterStates.filter{ it }
        booleanList += tagfilterStates.filter{ it }
        return booleanList.isEmpty()
    }


    private val _filteredTaskList = MutableStateFlow<List<Task>>(emptyList())
    val filteredTaskList: StateFlow<List<Task>> = _filteredTaskList

    fun addFilter(task: Task) {
        val updatedList = _filteredTaskList.value.toMutableList()
        if(!_filteredTaskList.value.contains(task)){
            updatedList.add(task)
        }

        _filteredTaskList.value = updatedList
    }

    fun removeFilter(task: Task) {
        val updatedList = _filteredTaskList.value.toMutableList()

        if(_filteredTaskList.value.contains(task)){
            updatedList.remove(task)
        }
        _filteredTaskList.value = updatedList
    }

    fun resetFilter() {
        _filteredTaskList.value = emptyList()
    }

    fun removeTaskFromList(taskId: String) {
        val updatedList = _filteredTaskList.value.toMutableList()
        val task = updatedList.find { it.id == taskId }
        if (task != null) {
            updatedList.remove(task)
        }
        _filteredTaskList.value = updatedList
    }


    //UTILS FOR SORTING
    private val _sorting = MutableStateFlow("")
    val sorting: StateFlow<String> = _sorting

    fun setSorting(sorting: String) {
        _sorting.value = sorting
    }
    fun resetSorting() {
        _sorting.value = ""
    }

}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListManager(
    vm: TaskListViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    teamId: String
) {

    val actions = remember(navController) {
        Actions(navController = navController)
    }


    val sorting by vm.sorting.collectAsState()
    val taskList by vm.getTasksByTeam(teamId, sorting.ifEmpty { null }).collectAsState(initial = emptyList())


    val userList by vm.getAllMembers().collectAsState(initial = emptyList())
    val tagList by vm.getAllTag().collectAsState(initial = emptyList())


    val sheetState = rememberModalBottomSheetState()

    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showSortingBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { actions.navigateToAddTask(teamId,"-1") },
                containerColor = Purple40
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = Color.White
                )

            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color("#FFFAFA".toColorInt()),
                ),
                title = {
                    Column {
                        Text(
                            text = "Task List",
                            style = typography.headlineLarge,
                            color = Color.Black

                            )
                    }
                },
                actions = {
                    MultiChoiceSegmentedButtonRow {
                        IconButton(onClick = { showFilterBottomSheet = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)) {

                            Icon(
                                painter = painterResource(R.drawable.filter_list),
                                contentDescription = "Filters Options",
                            )
                        }
                        IconButton(onClick = { showSortingBottomSheet = true },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)) {
                            Icon(
                                painter = painterResource(R.drawable.sort_list),
                                contentDescription = "Sorting Options"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        actions.navigateBack()
                    },    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .background(Color.White)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (taskList.isNotEmpty()) {
                TaskListPane(taskList, vm, navController, loggedUserId, teamId)
            }else{
                Box (contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()){

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Still nothing here... \n \n Create a new task!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 30.sp,
                    fontFamily = FontFamily(
                        Font(R.font.relay_inter_bold)
                    )
                )
                }
            }

        }
        //FILTERING BOTTOM SHEET
        if (showFilterBottomSheet) {
            ModalBottomSheet(
                containerColor = Color.White,
                onDismissRequest = {
                    showFilterBottomSheet = false
                },
                sheetState = sheetState
            ) {

                FilterBottomSheet(vm, userList, taskList, tagList)
            }


        }
        //SORTING BOTTOM SHEET
        if (showSortingBottomSheet) {
            ModalBottomSheet(
                containerColor = Color.White,
                onDismissRequest = {
                    showSortingBottomSheet = false
                },
                sheetState = sheetState
            ) {

                SortingBottomSheet(vm)

            }

        }
    }


}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskListPane(
    taskList: List<Task>,
    vm: TaskListViewModel,
    navController: NavController,
    loggedUserId: String,
    teamId: String
) {
    val filterState = vm.filteredTaskList.collectAsState()
    val filters = filterState.value
    var assignedTaskFilter by remember { mutableStateOf(false) }
    var unassignedTaskFilter by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 3.dp, end = 3.dp, bottom = 0.dp)){
            //Assigned FILTERCHIP
            FilterChip(
                onClick = {
                    assignedTaskFilter = !assignedTaskFilter
                    unassignedTaskFilter = false
                },
                colors = FilterChipDefaults.filterChipColors().copy(leadingIconColor = Purple40, labelColor = Color.Black),
                shape = MaterialTheme.shapes.small.copy(),
                border =  BorderStroke(1.dp, Color.Black),
                selected = assignedTaskFilter,
                label = { Text("Yours") } ,
                leadingIcon = if (assignedTaskFilter) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "Crown icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
            )
            //Unassigned FILTERCHIP
            FilterChip(
                modifier = Modifier.padding(start = 10.dp),
                onClick = {
                    unassignedTaskFilter = !unassignedTaskFilter
                    assignedTaskFilter = false
                },
                colors = FilterChipDefaults.filterChipColors().copy(leadingIconColor = Purple40, labelColor = Color.Black),
                shape = MaterialTheme.shapes.small.copy(),
                border =  BorderStroke(1.dp, Color.Black),
                selected = unassignedTaskFilter,
                label = { Text("Without you") } ,
                leadingIcon = if (unassignedTaskFilter) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = "Crown icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
            )
        }
        if (vm.filteringMode()) {

            taskList.sortedByDescending { it.assignedUser.contains(loggedUserId)
            }.filter {task -> if (assignedTaskFilter) task.assignedUser.contains(loggedUserId) else true
            }.filter {task -> if (unassignedTaskFilter) !task.assignedUser.contains(loggedUserId) else true
            }.forEach {
                Row(modifier = Modifier.padding(15.dp)) {
                    TaskCardComposable(task = it, vm, navController,loggedUserId, teamId)
                }
            }
        } else {

            filters.sortedByDescending { it.assignedUser.contains(loggedUserId)
            }.filter {task -> if (assignedTaskFilter) task.assignedUser.contains(loggedUserId) else true
            }.filter {task -> if (unassignedTaskFilter) !task.assignedUser.contains(loggedUserId) else true
            }.forEach {
                Row(modifier = Modifier.padding(15.dp)) {
                    TaskCardComposable(task = it, vm, navController, loggedUserId, teamId)
                }
            }
        }


    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskCardComposable(
    task: Task,
    vm: TaskListViewModel,
    navController: NavController,
    loggedUserId: String,
    teamId: String
) {
    val actions = remember(navController) {
        Actions(navController = navController)
    }


    val currentDateTime: LocalDateTime = LocalDateTime.now()

    // Calcola la differenza tra la data di creazione e la data di scadenza
    val totalDuration: Long = Duration.between(task.creationDate, task.dueDate).toMillis()

    // Calcola la differenza tra la data corrente e la data di creazione
    val elapsedDuration: Long = Duration.between(task.creationDate, currentDateTime).toMillis()

    // Calcola la percentuale di avanzamento
    val progress =
        if(task.status == Status.PENDING){
            0f
        }else if (task.status == Status.IN_PROGRESS) {
            ((elapsedDuration.toFloat()) / (totalDuration.toFloat())).coerceIn(0.2f, 0.8f)
        }else if (task.status == Status.CHECKING){
            0.8f
        }else if (task.status == Status.COMPLETED){
            1f
        }else{
            1f
        }


    //lista dei membri assegnati alla singola task
    val members by vm.getMembers(task.assignedUser).collectAsState(initial = null)

    if (members == null){
        //Loading
    }
    else {
        TaskCard(
            modifier = Modifier,
            nameText = task.title,
            descriptionText = task.description,
            dueDateText = task.dueDate.dayOfMonth.toString() + "/" + task.dueDate.monthValue.toString() + "/" + task.dueDate.year.toString(),
            onClick = {
                actions.navigateToTaskDetail(loggedUserId, task.id)
            },
            avatarNumberText = if (members!!.isEmpty()) "/"  else "+" + members!!.size.toString(),
            editTaskHandler = {
                actions.navigateToAddTask(teamId,task.id)
            },
            deleteTaskHandler = {
                vm.removeTask(task.id)
                vm.removeReviewFromUsers(task.assignedUser, task.id)
                vm.removeTaskFromList(task.id)
            },
            progressBar = {
                val progressColor = when (task.status) {
                    Status.COMPLETED -> Color(0xFF4CAF50) // Verde come colore di successo
                    Status.OVERDUE -> MaterialTheme.colorScheme.error
                    else -> Color(0xFF7F56D9)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = progressColor,
                    trackColor = Color(0xFFB4B5EC),
                    modifier = Modifier.height(5.dp),
                    strokeCap = StrokeCap.Round
                )
            },

            )
    }
}