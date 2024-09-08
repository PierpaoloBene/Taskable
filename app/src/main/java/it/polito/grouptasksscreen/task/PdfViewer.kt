package it.polito.grouptasksscreen.task
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.polito.grouptasksscreen.ui.theme.GroupTasksScreenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroupTasksScreenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfOpener()
                }
            }
        }
    }
}

@Composable
fun PdfOpener() {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf(Uri.EMPTY) }
    var showAddAttachmentDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            selectedUri = it
            openPdf(context, selectedUri)
        }
    }

    if (selectedUri != Uri.EMPTY) {
        launcher.launch(arrayOf("application/pdf"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                launcher.launch(arrayOf("application/pdf"))
            }
    ) {
        Text("Click to select PDF")
    }
}

fun openPdf(context: Context, uri: Uri) {
    val openPdfIntent = Intent(Intent.ACTION_VIEW)
    openPdfIntent.setDataAndType(uri, "application/pdf")
    openPdfIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

    try {
        context.startActivity(openPdfIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open PDF", Toast.LENGTH_SHORT).show()
    }
}