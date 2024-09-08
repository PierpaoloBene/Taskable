package it.polito.grouptasksscreen.task.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import it.polito.grouptasksscreen.model.Status
import it.polito.grouptasksscreen.model.Task
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.task.TaskListViewModel
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.Purple80


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(vm: TaskListViewModel, userList: List<User>, taskList: List<Task>, tagList: List<String>) {

    val statusfilterStates = remember { vm.statusfilterStates }
    val userfilterStates = remember { vm.userfilterStates }
    val tagfilterStates = remember { vm.tagfilterStates }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Status", "User", "Tag")



    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow{
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex,
                    colors = SegmentedButtonDefaults.colors().copy(
                        activeContainerColor = Purple80,
                        inactiveContainerColor = Color.White)
                ){
                    Text(label)
                }
            }
        }
    }
    when (selectedIndex) {
        //STATUS FILTERS CHECKBOXES
        0 -> {

            Column(
                Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Actual Status",
                    fontSize = 4.em,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                    style = typography.titleSmall
                )

                Status.entries.forEachIndexed { index, status ->
                    val statusLabel =
                        status.name.first().uppercase() + status.name.removeRange(0..0)
                            .replace('_', ' ').lowercase()

                    if (statusfilterStates.size <= index) {
                        statusfilterStates.add(false)
                    }
                    val statusBoolean = statusfilterStates[index]

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = statusBoolean,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // filtra per stato
                                    taskList.filter { it.status.name == status.name
                                    }.forEach{
                                        vm.addFilter(it)
                                    }

                                } else {
                                    // rimuovi filtro
                                    taskList.filter { it.status.name == status.name }.forEach{
                                        vm.removeFilter(it)
                                    }

                                }
                                statusfilterStates[index] = isChecked


                            },
                            modifier = Modifier.padding(3.dp)
                        )
                        Text(statusLabel)

                    }
                }

                //REMOVE APPLIED FILTERS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp, 1.dp, 1.dp, 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        vm.resetFilter()
                        userfilterStates.fill(false)
                        statusfilterStates.fill(false)
                        tagfilterStates.fill(false)
                    },
                        colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)
                    ) {
                        Text(text = "Remove Applied Filters")
                    }
                }
            }
        }
        // USER FILTERS CHECKBOXES
        1 -> {

            Column(
                Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Assigned User",
                    fontSize = 4.em,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                    style = typography.titleSmall
                )

                userList.forEachIndexed { index, user ->
                    val userNickname = user.nickname
                    // Se la lista degli stati dei filtri è vuota, aggiungi uno stato falso
                    if (userfilterStates.size <= index) {
                        userfilterStates.add(false)
                    }
                    val userBoolean = userfilterStates[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Checkbox(
                            checked = userBoolean,
                            onCheckedChange = { isChecked ->
                                userfilterStates[index] = isChecked

                                // Filtra i team in base agli utenti selezionati
                                val selectedUserIds = userList.filterIndexed { idx, _ -> userfilterStates[idx] }.map { it.id }
                                val filteredTeams = if (selectedUserIds.isEmpty()) {
                                    taskList
                                } else {
                                    taskList.filter { task ->
                                        selectedUserIds.all { userId -> task.assignedUser.contains(userId) }
                                    }
                                }

                                vm.resetFilter()
                                filteredTeams.forEach { vm.addFilter(it) }
                            },
                            modifier = Modifier.padding(3.dp),
                        )
                        Text(text = userNickname)
                    }
                }

                //REMOVE APPLIED FILTERS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp, 1.dp, 1.dp, 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        vm.resetFilter()
                        userfilterStates.fill(false)
                        statusfilterStates.fill(false)
                        tagfilterStates.fill(false)
                    },
                        colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
                        Text(text = "Remove Applied Filters")
                    }
                }
            }
        }
        // TAG FILTERS CHECKBOXES
        2 -> {

            Column(
                Modifier.padding(10.dp)
            ) {
                Text(text = "Tags",
                    fontSize = 4.em,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                    style = typography.titleSmall
                )

                tagList.forEachIndexed { index, tag ->

                    if (tagfilterStates.size <= index) {
                        tagfilterStates.add(false)
                    }
                    val tagBoolean = tagfilterStates[index]


                    Row (verticalAlignment = Alignment.CenterVertically){
                        Checkbox(
                            checked = tagBoolean,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // filtra per tag

                                    taskList.filter {it.tagList.contains(tag)
                                    }.forEach{
                                        vm.addFilter(it)
                                    }

                                } else {
                                    // rimuovi filtro
                                    taskList.filter { it.tagList.contains(tag) }.forEach{
                                        vm.removeFilter(it)
                                    }

                                }
                                tagfilterStates[index] = isChecked

                            },
                            modifier = Modifier.padding(3.dp)
                        )
                        Text(tag)

                    }
                }

                //REMOVE APPLIED FILTERS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp, 1.dp, 1.dp, 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        vm.resetFilter()
                        userfilterStates.fill(false)
                        statusfilterStates.fill(false)
                        tagfilterStates.fill(false)
                    },
                        colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
                        Text(text = "Remove Applied Filters")
                    }
                }
            }

        }
    }
}
