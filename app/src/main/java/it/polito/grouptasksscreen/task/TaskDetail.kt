package it.polito.grouptasksscreen.task

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.History
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Status
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.profile.secondary_color
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
class TaskDetailViewModel(private val model: Model) : ViewModel() {

    fun getTaskById(taskId: String) = model.getTaskById(taskId)
    fun addHistoryToTask(taskId: String, history: History) = model.addHistoryToTask(taskId,history)

    fun getMembers(userIds: List<String>) = model.getMembers(userIds)
    fun getMemberById(userId: String) = model.getMemberById(userId)
    fun updateTaskStatus(taskId: String, newStatus: String) = model.updateTaskStatus(taskId, newStatus)
    fun joinTask(taskId: String, userId: String) = model.joinTask(taskId, userId)

    //ratings
    private val _ratings = mutableStateOf<Map<String, Int>>(emptyMap())
    val ratings: State<Map<String, Int>> = _ratings

    fun UnReatedMembersToast() = model.UnReatedMembersToast()

    fun getUserReviews(userId: String) = model.getUserReviews(userId)
    private fun updateUserRatings(usersMap: Map<String, Int>, taskId: String, loggedUserId: String) = model.updateUserRatings(usersMap, taskId, loggedUserId)
    fun updateRating(userId: String, rating: Int) {
        _ratings.value += (userId to rating)
    }
    private fun allUsersRated(users: List<String>): Boolean {
        val ratedUserIds = _ratings.value.keys
        return users.all { it in ratedUserIds }
    }
    fun submitRatings(users: List<String>, taskId: String, loggedUserId: String) {
        if (!allUsersRated(users)) {
            UnReatedMembersToast()
            return
        }
        updateUserRatings(ratings.value, taskId, loggedUserId)
    }
    fun getAttachment(taskId: String,fileName: String, onSuccess:(Uri) -> Unit) = model.getAttachment(taskId,fileName, onSuccess )
    fun deleteAttachment(taskId: String,fileName: String) = model.removeAttachment(taskId,fileName,{},{})

    fun addAttachmentToTask(taskId: String, fileName: String, uri: Uri ) = model.uploadAttachment(taskId,fileName,uri, {attachmentsAdded()}, {attachmentsFailed(it)} )

    var attachmentsResponse by mutableStateOf(false)
    var attachmentsError by mutableStateOf("")

    fun attachmentsAdded(){
        attachmentsResponse = true
    }

    fun attachmentsFailed(err: String){
        attachmentsResponse = false
        attachmentsError = err
    }


}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CountdownTimer(
    comparisonDateTime: LocalDateTime,
    remaining_time: String,
    task: Task
) {
    var progress by remember { mutableStateOf(0f) }


    LaunchedEffect(comparisonDateTime) {

        // Calcola la differenza tra la data di creazione e la data di scadenza
        val totalDuration: Long = Duration.between(task.creationDate, task.dueDate).toMillis()

        while (true) {
            val currentDateTime: LocalDateTime = LocalDateTime.now()

            // Calcola la differenza tra la data corrente e la data di creazione
            val elapsedDuration: Long =
                Duration.between(task.creationDate, currentDateTime).toMillis()

            // Calcola la percentuale di avanzamento
            progress = (elapsedDuration.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

            // Aggiorna ogni secondo
            delay(1000)
        }
    }

    val progressColor = when (task.status) {
        Status.COMPLETED -> Color(0xFF4CAF50) // Verde come colore di successo
        Status.OVERDUE -> MaterialTheme.colorScheme.error
        else -> Purple40
    }

    LinearProgressIndicator(
        progress = {progress},
        color = progressColor
    )

    Spacer(modifier = Modifier.size(8.dp))

    Text(
        text = if(task.status == Status.OVERDUE) "OVERDUE" else remaining_time,
        color = progressColor
    )

}

@Composable
fun BoxIcon(number_state: Int, task_state: Int) {
    val backgroundColor =
        if (number_state == task_state) Purple40 else Color.Transparent
    val textColor =
        if (number_state == task_state) Color.White else Color.Black
    Box(
        modifier = Modifier
            .size(70.dp) // Dimensione del cerchio
            .background(
                color = backgroundColor, // Colore di sfondo trasparente
                shape = CircleShape // Forma del cerchio
            )
            .border(
                width = 2.dp, // Spessore del bordo
                color = Color.Black, // Colore del bordo
                shape = CircleShape // Forma del cerchio
            )
    ) {
        if (number_state < task_state) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Check Icon",
                tint = Color.Black, // Colore dell'icona
                modifier = Modifier
                    .size(24.dp) // Dimensione dell'icona
                    .align(Alignment.Center) // Icona centrata all'interno del cerchio
            )
        }
        if (number_state >= task_state) {
            Box(
                modifier = Modifier.align(Alignment.Center) // Centra il contenuto all'interno del cerchio
            ) {
                Text(
                    text = "$number_state",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
            }
        }
    }
    if (number_state != 4) {
        HorizontalDivider(
            modifier = Modifier.width(16.dp), // Larghezza della linea
            color = Color.Black, // Colore della linea
            thickness = 2.dp // Spessore della linea
        )
    }
}

@Composable
fun CircularBorderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp,
    buttonSize: Dp = 64.dp,
    contentColor: Color = Color.Black,
    backgroundColor: Color = Color.Transparent,
    shape: RoundedCornerShape = CircleShape
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .border(borderWidth, Color.Black, shape)
            .background(backgroundColor, shape)
            .clickable { onClick() },  // Aggiungi il gestore di click qui
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Icon",
            tint = contentColor
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetail(
    vm: TaskDetailViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String,
    taskId: String
) {
    val task by vm.getTaskById(taskId).collectAsState(initial = Task(id = "-1"))
    val members by vm.getMembers(task.assignedUser).collectAsState(initial = emptyList())
    val reviews by vm.getUserReviews(loggedUserId).collectAsState(initial = emptyList())
    val loggedUser by vm.getMemberById(loggedUserId).collectAsState(initial = null)
    var selectedUri by remember {
        mutableStateOf( Uri.EMPTY)
    }

    var showAddAttachmentDialog by remember {
        mutableStateOf(false)
    }

    if(selectedUri!= Uri.EMPTY){
        openPdf(LocalContext.current, selectedUri)
        selectedUri = Uri.EMPTY
    }

    val actions = remember(navController) { Actions(navController) }
    var task_state = 0
    if(task.status == Status.PENDING) task_state = 1
    if(task.status == Status.IN_PROGRESS) task_state = 2
    if(task.status == Status.CHECKING) task_state = 3
    if(task.status == Status.COMPLETED) task_state = 4

    // Ottieni la data e l'orario correnti
    val currentDate: LocalDateTime = LocalDateTime.now()

    // Calcola la differenza tra le due date
    val duration: Duration = Duration.between(currentDate,task.dueDate)

    // Ottieni i giorni, le ore e i minuti dalla durata
    val days: Long = duration.toDays()
    val hours: Long = duration.toHours() % 24
    val minutes: Long = duration.toMinutes() % 60
    val remaining_time = "Remaining time: $days d $hours h $minutes m"
    var number_state = 1
    // Creo un formatter per rendere la data come richiede il testo
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val formattedTaskDueDate = task.dueDate.format(formatter)

    // Variabile per la conferma di change status del task
    var showConfirmation by remember { mutableStateOf(false) }

    // Funzione per mostrare il banner di conferma
    fun showConfirmationBanner() {
        showConfirmation = true
    }

    // Funzione per nascondere il banner di conferma
    fun hideConfirmationBanner() {
        showConfirmation = false
    }

    val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.ITALIAN)

    var textBanner = "Update Task Status"
    if (task.status == Status.COMPLETED) textBanner = "TASK COMPLETED"
    if(task.id=="-1") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Nothing was found for this task")
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    ),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = task.title,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Purple40,
                                    modifier = Modifier.align(Alignment.Center)

                                )
                            }
                            IconButton(
                                onClick = {
                                    actions.navigateToCommentSection(loggedUserId,taskId)
                                },

                                ) {
                                Icon(
                                    Icons.AutoMirrored.Default.Send,
                                    contentDescription = "Chat Icon"
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            actions.navigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = ""
                            )
                        }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                Spacer(modifier = Modifier.size(24.dp))
                if(task.status != Status.COMPLETED && task.status != Status.OVERDUE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxIcon(number_state, task_state)
                        number_state += 1
                        BoxIcon(number_state, task_state)
                        number_state += 1
                        BoxIcon(number_state, task_state)
                        number_state += 1
                        BoxIcon(number_state, task_state)
                        number_state = 1
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending   ",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple40
                        )
                        Text(
                            text = "In Progress ",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple40
                        )
                        Text(
                            text = "Checking",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple40
                        )
                        Text(
                            text = "Completed",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Purple40
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                }
                if (task.status != Status.COMPLETED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Due date: $formattedTaskDueDate ",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if(task.status == Status.OVERDUE) MaterialTheme.colorScheme.error else Purple40
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    CountdownTimer(
                        LocalDateTime.now().plusMinutes(30),
                        remaining_time,
                        task
                    )

                }
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp) // Margine esterno della card
                        .background(Color.White) // Colore di sfondo della card
                        .shadow(
                            4.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) // Effetto di depressione
                        .heightIn(
                            min = 120.dp,
                            max = 200.dp
                        ) ,
                    colors = CardDefaults.cardColors( containerColor = Color("#FFFAFA".toColorInt()))
                ) {
                    Box(
                        modifier = Modifier.verticalScroll(
                            rememberScrollState()
                        )
                    ) {
                        Column {
                            Text(
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                text = "Description:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Purple40
                            )
                            Text(
                                text = task.description,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 0.dp) // Aggiungi eventuali altri modificatori
                            )
                        }
                    }
                }


                if(task.tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Tag List:",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Purple40
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp) // Margine esterno della card
                            .background(Color.White) // Colore di sfondo della card
                            .shadow(
                                4.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) // Effetto di depressione
                    ){
                        task.tagList.forEach { tag ->
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
                                    Spacer(modifier = Modifier.size(8.dp))
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
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "History:",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Purple40
                )
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .shadow(
                            4.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .heightIn(min = 60.dp, max = 180.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(task.history) { historyItem ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = historyItem.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = historyItem.creationDate.format(dateFormatter),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                                Divider(
                                    color = Color.LightGray,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Assigned Users:",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Purple40
                )
                if(members.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        members.forEach { user ->
                            Card(
                                modifier = Modifier
                                    .size(120.dp, 120.dp)
                                    .padding(10.dp),
                                onClick = {
                                    actions.navigateToProfile(loggedUserId, user.id)
                                },
                                shape = CardDefaults.shape,
                                colors =
                                CardDefaults.cardColors()
                                    .copy(
                                        containerColor = Color("#FFFAFA".toColorInt()),
                                        disabledContainerColor = Color("#FFFAFA".toColorInt()),
                                        disabledContentColor = Color.Black
                                    ),
                                border = BorderStroke(1.dp, Color.LightGray),
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
                                    Text(
                                        user.nickname,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily(Font(R.font.relay_inter_medium))
                                    )

                                }

                                //endregion
                            }
                        }
                    }
                }
                // region TASK ATTACHMENTS CARDS
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Attachments:",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Purple40
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Start
                ) {

                    // region ADD NEW ATTACHMENT CARD
                    if(task.status != Status.COMPLETED && task.assignedUser.contains(loggedUserId)) {
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = { showAddAttachmentDialog = true },
                            shape = CardDefaults.shape,
                            colors = CardDefaults.cardColors()
                                .copy(containerColor = Color("#FFFAFA".toColorInt())),
                            border = BorderStroke(1.dp, Color.LightGray),
                            elevation = CardDefaults.elevatedCardElevation()
                        ) {

                            // region ADD NEW BUTTON
                            Box(
                                modifier = Modifier.size(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Image(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Add Attachment",
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp)) // Aggiungi uno spazio tra l'icona e il testo
                                    Text(
                                        "Add New",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily(Font(R.font.relay_inter_bold))
                                    )
                                }
                            }

                            // endregion

                            // region Add New Attachment Text

                            // endregion
                        }
                    }
                    // endregion

                    // region ATTACHMENTS LIST
                    task.attachments.forEach { attachmentName ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 120.dp)
                                .padding(10.dp),
                            onClick = {
                                // Handle attachment click (e.g., open the document)
                                      vm.getAttachment(taskId, attachmentName) { uri ->
                                          selectedUri = uri

                                      }
                            },
                            shape = CardDefaults.shape,
                            colors =
                            CardDefaults.cardColors()
                                .copy(containerColor = Color("#FFFAFA".toColorInt())),
                            border = BorderStroke(1.dp, Color.LightGray),
                            elevation = CardDefaults.elevatedCardElevation()
                        ) {
                            // region ATTACHMENT Text
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    attachmentName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily(Font(R.font.relay_inter_bold))
                                )
                            }
                            //endregion

                        }
                    }
                    // endregion
                }
// endregion

                //region TASK ATTACHMENTS DIALOG
                if (showAddAttachmentDialog) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        AddAttachmentDialog(
                            vm,
                            taskId,
                            {showAddAttachmentDialog = false}
                        )
                    }
                }
                //endregion


                val progressColor = when (task.status) {
                    Status.COMPLETED -> Color(0xFF4CAF50) // Verde come colore di successo
                    Status.OVERDUE -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                Button(
                    onClick = {
                        //task_state += 1
                        if (task.status != Status.COMPLETED && task.status != Status.OVERDUE) {
                            showConfirmationBanner()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(Purple40),
                    modifier = Modifier
                        .padding(16.dp) // Modificatore per il margine del pulsante
                        .fillMaxWidth()
                        .size(70.dp)
                ) {
                    val buttonText = if (task.status == Status.COMPLETED) {
                        "Task Completed!"
                    } else if (task.status == Status.OVERDUE){
                        "Task Overdue!"
                    } else if (members.find { it.id == loggedUserId } == null){
                        "Join Task"
                    }else {
                        "Upgrade Task Status"
                    }

                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                //ratings

                if((task.status == Status.COMPLETED || task_state == 4) && task.assignedUser.contains(loggedUserId)){
                    if(!reviews.contains(taskId)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                        Text(
                            text = "Congratulations!",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (members.size > 1) {
                            Text(
                                text = "Rank the effort of the other members",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            members.filter { it.id != loggedUserId }.forEach { member ->
                                UserItem(member, vm)
                            }

                            ButtonSubmitRatings(
                                vm,
                                members.filter { it.id != loggedUserId }.map { it.id },
                                taskId,
                                loggedUserId
                            )
                        } else {
                            Text(
                                text = "Thanks for your feedback!",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }

                // Banner di conferma
                if (showConfirmation) {
                    ConfirmationDialog(
                        onConfirm = {
                            hideConfirmationBanner()
                            if (members.find { it.id == loggedUserId } != null) {
                                task_state += 1

                                when (task_state) {
                                    2 -> {
                                        vm.updateTaskStatus(taskId, "IN_PROGRESS")
                                        vm.addHistoryToTask(
                                            task.id,
                                            History(
                                                "Status upgraded to IN_PROGRESS",
                                                LocalDateTime.now()
                                            )
                                        )
                                    }

                                    3 -> {
                                        vm.updateTaskStatus(taskId, "CHECKING")
                                        vm.addHistoryToTask(
                                            task.id,
                                            History(
                                                "Status upgraded to CHECKING",
                                                LocalDateTime.now()
                                            )
                                        )
                                    }

                                    4 -> {
                                        vm.updateTaskStatus(taskId, "COMPLETED")
                                        vm.addHistoryToTask(
                                            task.id,
                                            History(
                                                "Status upgraded to COMPLETED",
                                                LocalDateTime.now()
                                            )
                                        )
                                    }
                            }
                                }else{
                                vm.joinTask(taskId, loggedUserId)
                                vm.addHistoryToTask(
                                    task.id,
                                    History(
                                        "${loggedUser?.nickname} joined the task",
                                        LocalDateTime.now()
                                    )
                                )
                            }
                        },
                        onDismiss = {
                            hideConfirmationBanner()
                        },
                        text =
                            if (members.find { it.id == loggedUserId } != null) {
                                "Are you sure you want to change the status of this task?"
                            }else{
                                "Are you sure you want to join this task?"
                            }

                    )
                }

            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ButtonSubmitRatings(
    vm: TaskDetailViewModel,
    users: List<String>,
    taskId: String,
    loggedUserId: String
){
    Card(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(10.dp),
        onClick = {
            vm.submitRatings(users, taskId, loggedUserId)
        },
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors()
            .copy(containerColor = Color("#FFFAFA".toColorInt())),
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
                imageVector = Icons.Default.Check,
                contentDescription = "Send Ratings",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Send Ratings")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UserItem(user: User, vm: TaskDetailViewModel) {

    var selectedRating by remember { mutableStateOf(0) }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ), modifier = Modifier
            .fillMaxWidth(0.97f)
            .padding(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color("#FFFAFA".toColorInt())
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

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
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(0.6f)
            )

            for(i in 1..5){
                Icon(
                    imageVector = if(i<=selectedRating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "fill star",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            selectedRating = i
                            vm.updateRating(user.id, i)
                        },
                    Color(secondary_color)
                )
            }


            /*
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = "fill star",
            modifier = Modifier.size(32.dp),
            Color(secondary_color)
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
        }*/
        }
    }
}

@Composable
fun ConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    text: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Confirm") },
        text = { Text(text = text) },
        confirmButton = {
            Button(onClick = onConfirm,colors = ButtonDefaults.buttonColors(Purple40)) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss,colors = ButtonDefaults.buttonColors(Purple40)) {
                Text(text = "Dismiss")
            }
        }
    )
}



@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AddAttachmentDialog(
    vm: TaskDetailViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    taskId: String,
    onDismissRequest: () -> Unit,
) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedFileUri = it }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false),
        title = { Text(text = "Add Attachment") },
        text = {
            Column {
                if (selectedFileUri == null) {
                    Button(
                        onClick = {
                            launcher.launch("application/*")
                        }
                    ) {
                        Text(text = "Choose File")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text field for attachment name
                OutlinedTextField(
                    value = attachmentName,
                    onValueChange = {
                        attachmentName = it
                    },
                    label = { Text("Attachment Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (attachmentName.isNotBlank() && selectedFileUri != null) {
                        vm.addAttachmentToTask(taskId, attachmentName, selectedFileUri!!)
                        selectedFileUri = null
                        attachmentName = ""
                        onDismissRequest()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    selectedFileUri = null
                    attachmentName = ""
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
