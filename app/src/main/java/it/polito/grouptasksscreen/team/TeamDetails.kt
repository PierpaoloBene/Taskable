package it.polito.grouptasksscreen.team

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory

class TeamDetailsViewModel(private val model: Model) : ViewModel() {
    fun getTeamById(teamId: String) = model.getTeamById(teamId)

    fun getMembers(teamId: String) = model.getMembersByTeam(teamId)
    fun addMemberToTeam(teamId: String, userId: String) = model.addMemberToTeam(teamId, userId)

    fun deleteUserFromTeam(teamId: String, userId: String) = model.deleteUserFromTeam(teamId, userId)

    var invitationPane by mutableStateOf(false)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeamDetailsPane(
    vm: TeamDetailsViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    teamId: String,
    invited: String,
) {

    val actions = remember(navController) {
        Actions(navController = navController)
    }


    val team by vm.getTeamById(teamId).collectAsState(initial = Team(id = "-1"))

    val members by vm.getMembers(teamId).collectAsState(initial = emptyList())



    var showConfirmation by remember { mutableStateOf(false) }


    LaunchedEffect(members) {
        if(vm.invitationPane == false) {
            vm.invitationPane = !members.map { it.id }.contains(loggedUserId)
        }
    }

    Log.d("YYYYINV", "invitationPane:${vm.invitationPane} and invited: $invited")

    if (team.id != "-1") {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color("#FFFAFA".toColorInt()),
                titleContentColor = Color("#061E51".toColorInt()),
            ), title = {
                Column {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily(
                            Font(
                                R.font.relay_inter_bold
                            )
                        )

                    )
                }
            }, navigationIcon = {
                IconButton(onClick = {
                    if(invited=="true") {
                        vm.invitationPane = false
                        actions.navigateToTeamList(loggedUserId)
                    }else{
                        actions.navigateBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            })
        }) {

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.padding(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
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
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(team.groupImageUri).build(),
                            contentDescription = "",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }

                }
                Spacer(modifier = Modifier.height(16.dp))

                ElevatedCard(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    )
                ) {
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        )
                        Text(
                            text = team.category,
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                    HorizontalDivider(
                        color = Color.Gray, thickness = 1.dp, modifier = Modifier.fillMaxWidth()
                    )
                    Column {
                        Text(
                            text = "Creation Date",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        )
                        Text(
                            text = "${team.creationDate.dayOfMonth}/${team.creationDate.monthValue}/${team.creationDate.year}",
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                    HorizontalDivider(
                        color = Color.Gray, thickness = 1.dp, modifier = Modifier.fillMaxWidth()
                    )
                    Column {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        )
                        Text(
                            text = team.description,
                            fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                if (vm.invitationPane && invited=="true") {
                    Spacer(modifier = Modifier.height(25.dp))
                    ElevatedCard(
                        enabled = false,
                        onClick = {},
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp,
                            disabledElevation = 6.dp
                        ), modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            disabledContentColor =  Color.Black,
                            disabledContainerColor =  Color("#FFFAFA".toColorInt()),
                        )) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center) {


                            Text(
                                text = "Do you want to join the team?",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    vm.invitationPane = false
                                vm.addMemberToTeam(teamId,loggedUserId)
                            }) {
                                Text("Accept")
                            }
                            Button(onClick = {
                                actions.navigateToTeamList(loggedUserId)
                            }) {
                                Text("Decline")
                            }
                        }
                    }
                }


                Text(
                    text = "Team Members",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    )
                ) {
                    members.sortedByDescending { it.id == team.ownerId }.forEach { user ->
                        Row(
                            modifier = Modifier
                                .clickable { actions.navigateToProfile(loggedUserId, user.id) }
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.width(10.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user.profilePhotoUri).build(),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent),
                                    contentScale = ContentScale.Crop,
                                )

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.nickname,
                                fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .weight(0.6f) // This will make the text take up available space
                            )
                            if (user.id == team.ownerId) {
                                Image(
                                    painter = painterResource(id = R.drawable.id_crown_icon),
                                    contentDescription = "Owner",
                                    modifier = Modifier
                                        .size(25.dp)
                                        .align(Alignment.CenterVertically)
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                            }
                        }
                        if(user.id != members.sortedByDescending { it.id == team.ownerId }.last().id) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                if(team.ownerId != loggedUserId  && team.members.contains(loggedUserId)){
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(10.dp),
                        onClick = { showConfirmation = true },
                        shape = CardDefaults.shape,
                        colors = CardDefaults.cardColors()
                            .copy(containerColor = Color.Red),
                        border = BorderStroke(1.dp, Color.LightGray),
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Leave Team",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Leave Team",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    // Banner di conferma
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(text = "Confirm") },
            text = { Text(text = "Are you sure you want to leave the team?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmation = false
                    vm.deleteUserFromTeam(teamId, loggedUserId)
                    actions.navigateBack()
                }) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmation = false }) {
                    Text(text = "Dismiss")
                }
            }
        )
    }
}