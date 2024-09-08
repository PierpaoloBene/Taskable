package it.polito.grouptasksscreen.task.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import it.polito.grouptasksscreen.task.TaskListViewModel
import it.polito.grouptasksscreen.ui.theme.Purple40


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortingBottomSheet(vm: TaskListViewModel){


    // Sheet content

    // SORT METHODS
    Column (
        Modifier.padding(10.dp)
    ){

        Text(text = "Sort By", fontSize = 4.em , modifier = Modifier.padding(5.dp,0.dp,0.dp,0.dp))

        FlowRow {

            //ALPHABETIC SORT
            Button(onClick = {
                vm.setSorting("title")

            },colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40),
                modifier = Modifier.padding(3.dp)) {
                Text("Aplhabetic")
            }

            //EXPIRING DATE SORT
            Button(onClick = {
                vm.setSorting("dueTimestamp")
            }, Modifier.padding(3.dp),
                colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
                Text("Due Date")
            }

            //Creation Date
            Button(onClick = {
                vm.setSorting("creationTimestamp")
            }, Modifier.padding(3.dp),
                colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
                Text("Creation Date")
            }

        }

    }





    //DIVIDER
    Column (
        Modifier.padding(10.dp)
    ){
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
    }

    //REMOVE FILTERS
    Column(
        Modifier.padding(10.dp)
    ) {
        Button(onClick = {
            vm.resetSorting()
        }
        , colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
            Text(text = "Remove Sorting")
        }
    }


}