package it.polito.grouptasksscreen.team

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.login.LoginViewModel
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.team.utils.BottomAppBar
import it.polito.grouptasksscreen.team.utils.TeamFiltersBottomSheet
import it.polito.grouptasksscreen.teamcard.TeamCard
import it.polito.grouptasksscreen.teamcardmemberview.TeamCardMemberView
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RequiresApi(Build.VERSION_CODES.O)
class TeamListViewModel(private val model: Model) : ViewModel() {

    fun getTeams(userId: String) = model.getTeams(userId)
    fun getAllMembers() = model.getAllMembers()
    fun getMembers(userIds: List<String>) = model.getMembers(userIds)

    val userfilterStates = mutableStateListOf<Boolean>()
    val categoryfilterStates = mutableStateListOf<Boolean>()

    fun filteringMode(): Boolean{
        val booleanList = userfilterStates.filter{ it }.toMutableList()
        booleanList += categoryfilterStates.filter{ it }
        return booleanList.isEmpty()
    }

    private val _filteredTeamList = MutableStateFlow<List<Team>>(emptyList())
    val filteredTeamList: StateFlow<List<Team>> = _filteredTeamList

    fun addFilter(team: Team) {
        val updatedList = _filteredTeamList.value.toMutableList()
        if(!_filteredTeamList.value.contains(team)){
            updatedList.add(team)
        }

        _filteredTeamList.value = updatedList
    }

    fun removeFilter(team: Team) {
        val updatedList = _filteredTeamList.value.toMutableList()

        if(_filteredTeamList.value.contains(team)){
            updatedList.remove(team)
        }
        _filteredTeamList.value = updatedList
    }

    fun resetFilter() {
        _filteredTeamList.value = emptyList()
    }
    
}


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamListPane(
    vm: TeamListViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    login: LoginViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext))
) {
    val context = LocalContext.current

    //navigation
    val actions = remember(navController) {
        Actions(navController = navController)
    }

    Log.d("LOGGED USER", loggedUserId)
    //lists
    val teamList by vm.getTeams(loggedUserId).collectAsState(initial = null)
    val userList by vm.getAllMembers().collectAsState(initial = emptyList())
    val sheetState = rememberModalBottomSheetState()
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val filteredTeamList by vm.filteredTeamList.collectAsState()
    var ownerFilterSelected by remember { mutableStateOf(false) }
    var memberFilterSelected by remember { mutableStateOf(false) }

    //pane

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color("#FFFAFA".toColorInt()),
                    titleContentColor = Color.Black
                ),
                title = {
                    Column {
                        Text(
                            text = "My Teams",
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = FontFamily(
                                Font(R.font.relay_inter_bold)
                            )
                        )
                    }
                },
                actions = {
                    MultiChoiceSegmentedButtonRow {
                        IconButton(onClick = { showFilterBottomSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.filter_list),
                                contentDescription = "Filters Options",
                                tint = Purple40
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        login.signOut(context) {
                            actions.navigateToLogin()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = Purple40
                        )
                    }
                }
            )
        },
        bottomBar = { BottomAppBar(navController, loggedUserId) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { actions.navigateToNewTeam(loggedUserId) },
                containerColor = Purple40
            ) {
                Icon(Icons.Default.Add, "Add new Team Button", tint = Color.White)
            }
        }

    ) {
        //FILTERING BOTTOM SHEET
        if (showFilterBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFilterBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                val categoryList = teamList!!.map { team -> team.category }
                TeamFiltersBottomSheet(vm, userList, teamList!!, categoryList)
            }

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)  // Imposta lo sfondo grigio chiaro
                .verticalScroll(rememberScrollState())
                .padding(it),
        ) {

            //CHIPS
            Row (
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 3.dp, end = 3.dp, bottom = 0.dp)){

                //OWNER FILTERCHIP
                FilterChip(
                    onClick = {
                        ownerFilterSelected = !ownerFilterSelected
                        memberFilterSelected = false
                    },
                    colors = FilterChipDefaults.filterChipColors().copy(leadingIconColor = Purple40, labelColor = Color.Black),
                    shape = MaterialTheme.shapes.small.copy(),
                    border =  BorderStroke(1.dp, Color.Black),
                    selected = ownerFilterSelected,
                    label = { Text("Owner") } ,
                    leadingIcon = if (ownerFilterSelected) {
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
                                painter = painterResource(id = R.drawable.id_crown_icon),
                                contentDescription = "Crown icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    },
                )


                //Member FILTERCHIP
                FilterChip(
                    modifier = Modifier.padding(start = 5.dp),
                    onClick = {
                        memberFilterSelected = !memberFilterSelected
                        ownerFilterSelected = false
                    },
                    colors = FilterChipDefaults.filterChipColors().copy(leadingIconColor = Purple40, labelColor = Color.Black),
                    shape = MaterialTheme.shapes.small.copy(),
                    border =  BorderStroke(1.dp, Color.Black),
                    selected = memberFilterSelected,
                    label = { Text("Member") } ,
                    leadingIcon = if (memberFilterSelected) {
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
                                imageVector = Icons.Filled.SupervisorAccount,
                                contentDescription = "Crown icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    },
                )
            }

            if (teamList == null) {
                //TEAMS NOT RETRIEVED
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(80.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.width(128.dp),
                        color = Color(0xFF7F56D9),
                        trackColor = Color(0xFFB4B5EC),
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                }
            } else if (teamList!!.isEmpty()) {
                //TEAMS RETRIEVED AND EMPTY
                Spacer(Modifier.height(100.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "No teams available \n \n \n Create a new team now!",
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    fontFamily = FontFamily(
                        Font(R.font.relay_inter_bold)
                    )
                )
            } else {
                //TEAMS RETRIEVED AND NOT EMPTY
                if(vm.filteringMode()) {
                    teamList!!.sortedByDescending{team -> team.ownerId == loggedUserId
                    }.filter {team -> if (ownerFilterSelected) team.ownerId == loggedUserId else true
                    }.filter {team -> if (memberFilterSelected) team.ownerId != loggedUserId else true
                    }.forEach {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TeamCardBuilder(it, vm, navController, loggedUserId)
                        }
                    }
                } else if (!vm.filteringMode()) {
                    filteredTeamList.sortedByDescending{team -> team.ownerId == loggedUserId
                    }.filter {team -> if (ownerFilterSelected) team.ownerId == loggedUserId else true
                    }.filter {team -> if (memberFilterSelected) team.ownerId != loggedUserId else true
                    }.forEach {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TeamCardBuilder(it, vm, navController, loggedUserId)
                        }
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeamCardBuilder(
    team: Team,
    vm: TeamListViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String
) {
    val actions = remember(navController) {
        Actions(navController = navController)
    }

    val members by vm.getMembers(team.members).collectAsState(initial = emptyList())


    if (members.isEmpty()) {
        TeamCardWithLoading(team, actions, loggedUserId)
    } else {
        TeamCardWithMembers(team, members.sortedWith(compareByDescending { it.id == team.ownerId }), actions, loggedUserId)
    }

}





@Composable
fun ClickhereToAssignUserIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7879F1),
                        Color(0xFFB4B5EC)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Default.Add,
            contentDescription = "Add new user",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.background(Color.Transparent)
        )
    }
}

@Composable
fun AsyncImageRenderer(uri: Uri) =
    AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(uri)
        .build(),
    contentDescription = "",
    modifier = Modifier
        .size(150.dp)
        .clip(CircleShape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7879F1),
                    Color(0xFFB4B5EC)
                )
            )
        ),
    contentScale = ContentScale.Crop
)

@Composable
fun TeamCardWithMembers(team: Team, members: List<User>, actions: Actions, loggedUserId: String) {
    if(loggedUserId == team.ownerId) {
        TeamCard(
            nameText = team.name,
            teamProfileIcon = {
                AsyncImageRenderer(team.groupImageUri)
            },
            userOneIcon = {
                if (members.isNotEmpty()) AsyncImageRenderer(members[0].profilePhotoUri)
                else ClickhereToAssignUserIcon()
            },
            userTwoIcon = {
                if (members.size > 1) AsyncImageRenderer(members[1].profilePhotoUri)
                else ClickhereToAssignUserIcon()
            },
            avatarNumberText = if (members.size > 2) "+" + (members.size - 2).toString() else "+",
            categoryText = team.category,
            descriptionText = team.description,
            onClick = { actions.navigateToTaskList(loggedUserId, team.id) },
            chatButtonHandler = { actions.navigateToTeamChat(loggedUserId, team.id) },
            kpiButtonHandler = { actions.navigateToTeamAchievement(team.id) },
            editButtonHandler = {
                    actions.navigateToEditTeam(team.id, loggedUserId)
            },
            infoButtonHandler = { actions.navigateToTeamDetails(loggedUserId, team.id, "false") },
            assignedUserButton = { actions.navigateToTeamDetails(loggedUserId, team.id,"false") },
            memberButton = { actions.navigateToTeamDetails(loggedUserId, team.id,"false") },
            modifier = Modifier
        )
    }else{
        TeamCardMemberView(
            nameText = team.name,
            teamProfileIcon = {
                AsyncImageRenderer(team.groupImageUri)
            },
            userOneIcon = {
                if (members.isNotEmpty()) AsyncImageRenderer(members[0].profilePhotoUri)
                else ClickhereToAssignUserIcon()
            },
            userTwoIcon = {
                if (members.size > 1) AsyncImageRenderer(members[1].profilePhotoUri)
                else ClickhereToAssignUserIcon()
            },
            avatarNumberText = if (members.size > 2) "+" + (members.size - 2).toString() else "+",
            categoryText = team.category,
            descriptionText = team.description,
            onClick = { actions.navigateToTaskList(loggedUserId,team.id) },
            chatButtonHandler = { actions.navigateToTeamChat(loggedUserId,team.id) },
            kpiButtonHandler = { actions.navigateToTeamAchievement(team.id) },
            infoButtonHandler = { actions.navigateToTeamDetails(loggedUserId,team.id,"false") },
            assignedUserButton = { actions.navigateToTeamDetails(loggedUserId,team.id,"false") },
            memberButton = { actions.navigateToTeamDetails(loggedUserId,team.id,"false") },
            modifier = Modifier
        )
    }
}

@Composable
fun TeamCardWithLoading(team: Team, actions: Actions, loggedUserId: String) {
    val progressIndicator =  @Composable {
        CircularProgressIndicator(
            modifier = Modifier.width(64.dp),
            color = Color(0xFF7F56D9),
            trackColor = Color(0xFFB4B5EC),
            strokeCap = StrokeCap.Round
        )
    }


    TeamCardMemberView(
        nameText = team.name,
        teamProfileIcon = {progressIndicator()},
        userOneIcon = {progressIndicator()},
        userTwoIcon = {progressIndicator()},
        avatarNumberText = " ",
        categoryText = team.category,
        descriptionText = team.description,
        onClick = { actions.navigateToTaskList(loggedUserId,team.id) },
        chatButtonHandler = { actions.navigateToTeamChat(loggedUserId, team.id) },
        kpiButtonHandler = { actions.navigateToTeamAchievement(team.id) },
        infoButtonHandler = { actions.navigateToTeamDetails(loggedUserId,team.id,"false") },
        assignedUserButton = { actions.navigateToTeamDetails(loggedUserId,team.id,"false")},
        memberButton = { actions.navigateToTeamDetails(loggedUserId,team.id,"false")} ,
        modifier = Modifier
    )
}