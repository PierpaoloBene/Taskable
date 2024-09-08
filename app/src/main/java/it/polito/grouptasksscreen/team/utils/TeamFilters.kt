package it.polito.grouptasksscreen.team.utils


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
import it.polito.grouptasksscreen.model.Team
import it.polito.grouptasksscreen.model.User
import it.polito.grouptasksscreen.team.TeamListViewModel
import it.polito.grouptasksscreen.ui.theme.Purple40
import it.polito.grouptasksscreen.ui.theme.Purple80


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamFiltersBottomSheet(vm: TeamListViewModel, userList: List<User>, teamList: List<Team>, categoryList: List<String>) {

    val categoryfilterStates = remember { vm.categoryfilterStates }
    val userfilterStates = remember { vm.userfilterStates }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf( "User", "Category")



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
                ) {
                    Text(label)
                }
            }
        }
    }
    when (selectedIndex) {
        // USER FILTERS CHECKBOXES
        // USER FILTERS CHECKBOXES
        0 -> {

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
                                    teamList
                                } else {
                                    teamList.filter { team ->
                                        selectedUserIds.all { userId -> team.members.contains(userId) }
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
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple40

                        ),
                        onClick = {
                        vm.resetFilter()
                        userfilterStates.fill(false)
                        categoryfilterStates.fill(false)
                    }) {
                        Text(text = "Remove Applied Filters")
                    }
                }
            }
        }

        // CATEGORY FILTERS CHECKBOXES
        1 -> {

            Column(
                Modifier.padding(10.dp)
            ) {
                Text(text = "Tags",
                    fontSize = 4.em,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                    style = typography.titleSmall
                )

                categoryList.forEachIndexed { index, category ->

                    if (categoryfilterStates.size <= index) {
                        categoryfilterStates.add(false)
                    }
                    val categoryBoolean = categoryfilterStates[index]


                    Row (verticalAlignment = Alignment.CenterVertically){
                        Checkbox(
                            checked = categoryBoolean,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // filtra per tag
                                    teamList.filter {
                                        it.category == category
                                    }.forEach{
                                        vm.addFilter(it)
                                    }

                                } else {
                                    // rimuovi filtro
                                    teamList.filter { it.category == category }.forEach{
                                        vm.removeFilter(it)
                                    }

                                }
                                categoryfilterStates[index] = isChecked

                            },
                            modifier = Modifier.padding(3.dp)
                        )
                        Text(category)

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
                        categoryfilterStates.fill(false)
                    },
                        colors = ButtonDefaults.buttonColors().copy(containerColor = Purple40)) {
                        Text(text = "Remove Applied Filters")
                    }
                }
            }

        }
    }
}
