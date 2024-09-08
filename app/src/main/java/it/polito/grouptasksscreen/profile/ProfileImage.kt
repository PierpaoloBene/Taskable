package it.polito.grouptasksscreen.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.grouptasksscreen.R

@Composable
fun ProfileIcon(
    firstName: String,
    lastName: String,
    size: DpSize,
    selectedUri: Uri?,
    isEditing: Boolean
) {

    val initials =
        "${firstName.firstOrNull()?.uppercaseChar()}${lastName.firstOrNull()?.uppercaseChar()}"


    Box(
        modifier = Modifier
            .size(size)
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

        if (selectedUri == Uri.EMPTY) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(selectedUri)
                    .build(),
                contentDescription = "",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

        }
        if (isEditing) {

            Icon(
                painter = painterResource(id = R.drawable.camera_icon),
                contentDescription = "Camera Icon"
            )
        }

        /*
        if(!capturedImage.path.isNullOrEmpty()) {
            Image(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clip(CircleShape),
                painter = rememberAsyncImagePainter(capturedImage),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )

         }

         */

    }

}





