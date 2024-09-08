package it.polito.grouptasksscreen.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.model.Notification
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.team.utils.BottomAppBar
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.PurpleGrey80
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
@RequiresApi(Build.VERSION_CODES.O)
class NotificationViewModel(private val model: Model) : ViewModel() {
    fun getUserNotification(loggedUserId: String) = model.getUserNotification(loggedUserId)
    fun updateNotificationReadFlag(notificationId: String, index: Int) = model.updateNotificationReadFlag(notificationId, index)
    fun clearReadNotifications(userId: String) = model.clearReadNotifications(userId)
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationList(
    vm: NotificationViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    navController: NavController,
    loggedUserId: String
){
    val notifications by vm.getUserNotification(loggedUserId).collectAsState(initial = emptyList())
    val actions = remember(navController) { Actions(navController) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            modifier = Modifier.padding(bottom = 4.dp),
                            color = Color.Black
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
                            tint = Color.Black
                        )
                    }
                }
            )
        },
        bottomBar = { BottomAppBar(navController, loggedUserId) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (notifications.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(10.dp),
                    onClick = { vm.clearReadNotifications(loggedUserId) },
                    shape = CardDefaults.shape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color("#FFFAFA".toColorInt())
                    ),
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
                            contentDescription = "Clear Read Notifications",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Read Notifications", color = Color.Black)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    itemsIndexed(notifications) { index, notification ->
                        if (notification.readFlag)
                            NotificationItem(notification, vm)
                        else
                            UnreadNotificationItem(notification, vm, index)
                    }
                }
            } else {
                Text(
                    "No notifications available",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.relay_inter_medium)),
                    color = Color.Black
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UnreadNotificationItem(
    notification: Notification,
    vm: NotificationViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext)),
    index: Int
) {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.ITALIAN)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(2.dp, Purple40),
        colors = CardDefaults.cardColors(
            containerColor = PurpleGrey80
        ),
        onClick = { vm.updateNotificationReadFlag(notification.id, index) }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (!notification.readFlag) {
                Canvas(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    drawCircle(color = Purple40)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color.Black
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = notification.creationDate.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationItem(
    notification: Notification,
    vm: NotificationViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext))
) {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.ITALIAN)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color.Black
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = notification.creationDate.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}