package it.polito.grouptasksscreen.task


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.model.Comment
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.Purple80
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class CommentViewModel(private val model: Model) : ViewModel() {
    fun getTaskById(taskId: String) = model.getTaskById(taskId)
    fun getMemberById(userId: String) = model.getMemberById(userId)
    fun addCommentToTask(taskId: String, newComment: Comment) = model.addCommentToTask(taskId, newComment)
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSection(
    vm: CommentViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    taskId: String
) {
    var newComment by remember { mutableStateOf("") }
    val task by vm.getTaskById(taskId).collectAsState(initial = Task(id = "-1"))
    val actions = remember(navController) { Actions(navController) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color("#FFFAFA".toColorInt())
                ),
                title = {
                    Column {
                        Text(
                            text = task.title,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        actions.navigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "",
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color("#FFFFFF".toColorInt()),
                                focusedContainerColor = Color("#E7EAEE".toColorInt()),
                                unfocusedLabelColor = Purple40,
                                focusedLabelColor = Purple40,
                                focusedTrailingIconColor = Color.Transparent
                            ),
                            value = newComment,
                            onValueChange = { newComment = it },
                            label = { Text("Insert a new comment") },
                            modifier = Modifier
                                .weight(6f)
                                .clip(CircleShape)
                                .padding(horizontal = 0.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                vm.addCommentToTask(
                                    taskId,
                                    Comment(
                                        newComment,
                                        loggedUserId,
                                        LocalDateTime.now()
                                    )
                                )
                                newComment = ""
                            },

                            containerColor = Purple80,
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(horizontal = 8.dp),
                            shape = FloatingActionButtonDefaults.extendedFabShape
                        ) {
                            Text(text = "+", color = Color.Black, fontSize = 30.sp)
                        }
                    }
                },
                containerColor = Purple40
            )
        }
    ) {

        LazyColumn(
            modifier = Modifier.padding(it).background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.ITALIAN)
            items(task.comments) { comment ->
                val author by vm.getMemberById(comment.author).collectAsState(initial = User(nickname = "-1"))
                val formattedDate = comment.date.format(formatter)

                if(author.nickname!="-1") {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.White),
                        headlineContent = { Text(comment.text) },
                        overlineContent = { Text(author.nickname) },
                        supportingContent = { Text(formattedDate) },
                        leadingContent = {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(author.profilePhotoUri)
                                    .build(),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}