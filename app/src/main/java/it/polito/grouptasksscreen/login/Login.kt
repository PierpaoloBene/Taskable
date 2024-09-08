package it.polito.grouptasksscreen.login

import android.annotation.SuppressLint
import android.content.Context
import android.credentials.ClearCredentialStateException
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import it.polito.grouptasksscreen.R
import it.polito.grouptasksscreen.model.Model
import it.polito.grouptasksscreen.navigation.Actions
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.ui.theme.Purple40
import kotlinx.coroutines.launch

class LoginViewModel(private val model: Model) : ViewModel() {


    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user


    fun setUser(firebaseUser: FirebaseUser?) {
        _user.value = firebaseUser
    }


    var teamId: String = ""


    fun handleInvitation(invitedToTeamId: String){
        teamId = invitedToTeamId
    }

    fun resetInvitation(){
        teamId = ""
        invitationHandled = true
    }

    var invitationHandled by mutableStateOf(false)

    fun setInvitationHandled(){
        invitationHandled = true
    }
    fun getUserByMail(email: String) = model.getMemberByMail(email)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun clearCredentialState(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            val request = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(request)
            Log.d("LoginViewModel", "Credential state cleared successfully")
        } catch (e: ClearCredentialStateException) {
            Log.e("LoginViewModel", "Failed to clear credential state: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun signOut(context: Context, onSignOutComplete: () -> Unit) {
        setUser(null)
        viewModelScope.launch {
            clearCredentialState(context)
            FirebaseAuth.getInstance().signOut()
            onSignOutComplete()
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreenPane(
    navController: NavController,
    handleGoogleSignIn: (callback: (Boolean) -> Unit) -> Unit,
    vm: LoginViewModel = viewModel(factory = Factory(LocalContext.current.applicationContext))
) {
    val context = LocalContext.current
    val actions = remember(navController) { Actions(navController) }

    //PER SALTARE IL LOGIN NEGLI ACCESSI SUCCESSIVI HO MESSO QUESTO CODICE, TOGLILO SE DA PROBLEMI UOMO
  /*
    if(vm.user.value != null){
       val loggedUserId = vm.getUserByMail(vm.user.value!!.email.toString()).collectAsState(initial = "-1").value
        if(loggedUserId != "-1" ){
            if(vm.teamId.isEmpty()) {
                actions.navigateToTeamList(loggedUserId)
            }else{
                actions.navigateToTeamDetails(loggedUserId, vm.teamId, "true")
            }
            Toast.makeText(context, "Welcome Back!", Toast.LENGTH_SHORT).show()
        }
    }

   */
    //FINO A QUI NON TOCCARE IL RESTO MALEDETTO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.textlogo),
            contentDescription = "TextLogo",
            modifier = Modifier.size(240.dp)
        )

        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()

        HorizontalPager(
            count = 3, // Numero di immagini
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val imageRes = when (page) {
                0 -> R.drawable.image1
                1 -> R.drawable.image2
                else -> R.drawable.image3
            }
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Image $page",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Testo sotto il pager che cambia in base alla pagina corrente
        val descriptions = listOf("Manage your Team in a more efficient way",
            "Have a look at your daily tasks, all in one place!",
            "Chat with collegues and team members")
        Text(
            text = descriptions[pagerState.currentPage],
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )

        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            activeColor = Purple40,
            inactiveColor = Color.Gray,
            indicatorWidth = 8.dp,
            indicatorHeight = 8.dp
        )

        Button(
            onClick = {
                handleGoogleSignIn { success ->
                    if (success ) {
                        val firebaseUser = vm.user.value
                        firebaseUser?.let {
                            coroutineScope.launch {
                                vm.getUserByMail(it.email.toString()).collect{ loggedUserId ->
                                    if(loggedUserId != "-1" ){
                                        if(vm.teamId.isEmpty()) {
                                            actions.navigateToTeamList(loggedUserId)
                                        }else{
                                            actions.navigateToTeamDetails(loggedUserId, vm.teamId, "true")
                                            vm.teamId = ""
                                        }
                                    }
                                    else{
                                        Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Purple40)
        ) {
            Text(text = "Sign in with Google", color = Color.White)
        }
    }
}
