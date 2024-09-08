package it.polito.grouptasksscreen.team

import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
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
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.google.relay.compose.ColumnScopeInstanceImpl.weight
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Status
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.profile.secondary_color
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.Purple80


class TeamAchievementViewModel(private val model: Model) : ViewModel() {
    fun getTeamById(teamId: String) = model.getTeamById(teamId)
    fun getTasksByTeam(teamId: String) = model.getTasksByTeam(teamId)
    fun getMembersByTeam(teamId: String) = model.getMembersByTeam(teamId)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamAchievementPane(
    vm: TeamAchievementViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    teamId: String
) {
    val actions = remember(navController) { Actions(navController = navController) }

    val team by vm.getTeamById(teamId).collectAsState(Team(id = "-1"))
    val tasks by vm.getTasksByTeam(teamId).collectAsState(initial = null)
    val members by vm.getMembersByTeam(teamId).collectAsState(initial = emptyList())
    val completedTasks = tasks?.count { it.status == Status.COMPLETED } ?: 0
    val total = tasks?.count {
        it.status == Status.PENDING ||
                it.status == Status.IN_PROGRESS ||
                it.status == Status.CHECKING ||
                it.status == Status.OVERDUE ||
                it.status == Status.COMPLETED
    } ?: 0

    val progress = if (total > 0) completedTasks.toFloat() / total.toFloat() else 0f
    val progressText = "${(progress * 100).toInt()}%"

    var tasksAssignedMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var tasksCompletedMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var tasksOverdueMap by remember { mutableStateOf(mapOf<String, Int>()) }

    LaunchedEffect(teamId, members) {
        vm.getTasksByTeam(teamId).collect { taskList ->
            tasksAssignedMap = members.associate { member ->
                member.id to taskList.count { task -> task.assignedUser.contains(member.id) }
            }
            tasksCompletedMap = members.associate { member ->
                member.id to taskList.count { task -> task.assignedUser.contains(member.id) && task.status == Status.COMPLETED }
            }
            tasksOverdueMap = members.associate { member ->
                member.id to taskList.count { task -> task.assignedUser.contains(member.id) && task.status == Status.OVERDUE }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color("#FFFAFA".toColorInt())),
                title = {
                    Text(
                        modifier = Modifier.weight(0.9f),
                        text = team.name,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily(Font(R.font.relay_inter_bold))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { actions.navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item {
                CircularProgressIndicatorWithText(progress = progress, progressText = progressText, teamImageUri = team.groupImageUri)
                Spacer(modifier = Modifier.height(20.dp))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(0.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = CardDefaults.elevatedCardElevation(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    )

                ) {
                    Column(modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                        TeamInfoRow("Completed Tasks:", completedTasks)
                        HorizontalDivider()
                        TeamInfoRow("Total Tasks:", total)
                        HorizontalDivider()
                        TeamInfoRow("Tasks Set on PENDING:", tasks?.count { it.status == Status.PENDING } ?: 0)
                        HorizontalDivider()
                        TeamInfoRow("Tasks Set on IN_PROGRESS:", tasks?.count { it.status == Status.IN_PROGRESS } ?: 0)
                        HorizontalDivider()
                        TeamInfoRow("Tasks Set on CHECKING:", tasks?.count { it.status == Status.CHECKING } ?: 0)
                        HorizontalDivider()
                        TeamInfoRow("Tasks Set on OVERDUE:", tasks?.count { it.status == Status.OVERDUE } ?: 0)
                    }
                }
            }
            if (members.isNotEmpty()) {
                items(members) { member ->
                    val tasksAssigned = tasksAssignedMap[member.id] ?: 0
                    val tasksCompleted = tasksCompletedMap[member.id] ?: 0
                    val tasksOverdue = tasksOverdueMap[member.id] ?: 0
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .shadow(0.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        elevation = CardDefaults.elevatedCardElevation(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color("#FFFAFA".toColorInt())
                        ),
                    ) {
                        Column(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            TeamMemberRow(member, tasksAssigned, tasksCompleted, tasksOverdue)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .shadow(0.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        elevation = CardDefaults.elevatedCardElevation(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color("#FFFAFA".toColorInt())
                        ),
                    ) {
                        Column(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "No team members available.",
                                fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamInfoRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
        )
        Text(
            text = "$value",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TeamMemberRow(user: User, tasksAssigned: Int, tasksCompleted: Int,tasksOverdue: Int) {
    Row(modifier = Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = rememberImagePainter(
                data = user.profilePhotoUri,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.appicon)
                    error(R.drawable.appicon)
                }
            ),
            contentDescription = "Member Profile Photo",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = user.fullName,
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 25.sp,
        )
    }
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Total Assigned Tasks:",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
        )
        Text(
            text = "$tasksAssigned",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Total Task Completed:",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
        )
        Text(
            text = "$tasksCompleted",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Total Task in Overdue:",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
        )
        Text(
            text = "$tasksOverdue", // Calcola il numero di task in "OVERDUE"
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Text(
            text = "Ratings:",
            fontFamily = FontFamily(Font(R.font.relay_inter_bold)),
            fontSize = 18.sp,
        )
        for (i in 1..5) {
            if (i <= user.mean /*/ 2*/) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "fill star $i",
                    modifier = Modifier.size(32.dp),
                    Color(secondary_color)
                )
            }else if (i > user.mean /*/ 2*/) {
                Icon(
                    painter = painterResource(id = R.drawable.empty_star),
                    contentDescription = "empty star $i",
                    modifier = Modifier.size(32.dp),
                    Color(secondary_color)

                )
            }
        }
    }

}


@OptIn(ExperimentalCoilApi::class)
@Composable
fun CircularProgressIndicatorWithText(
    progress: Float,
    progressText: String,
    teamImageUri: Uri
) {
    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = Purple40,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
            trackColor = Purple80
        )

        Image(
            painter = rememberImagePainter(
                data = teamImageUri,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.appicon)
                    error(R.drawable.appicon)
                }
            ),
            contentDescription = "Team Image",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape), // Applica il crop circolare
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Project Status:",
        textAlign = TextAlign.Center,
        fontSize = 25.sp,
        fontFamily = FontFamily.Default
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = progressText,
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
        fontFamily = FontFamily.Default
    )
}