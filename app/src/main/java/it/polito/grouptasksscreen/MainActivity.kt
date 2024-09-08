@file:Suppress("DEPRECATION")

package it.polito.grouptasksscreen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import it.polito.grouptasksscreen.login.LoginViewModel
import it.polito.grouptasksscreen.navigation.Factory
import it.polito.grouptasksscreen.navigation.NavigationController
import it.polito.grouptasksscreen.ui.theme.GroupTasksScreenTheme

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {

    private var isDeepLink: Boolean = false
    private var deepLinkData: Uri? = null



    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val loginViewModel: LoginViewModel by viewModels {
        Factory(applicationContext)
    }


    private val rcSignIn = 9001

    private val webClientId = "115781484055-n0ph164k01h3qqrdr11rkg73nnggdmd7.apps.googleusercontent.com"
    var user: FirebaseUser? = null
    
    private var onGoogleSignInResult: ((Boolean) -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //region DEEP LINK UTILITIES
        handleDynamicLink(intent)
        //endregion



        auth = FirebaseAuth.getInstance()
        user = auth.currentUser

        if(user !=null){
            loginViewModel.setUser(user)
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            GroupTasksScreenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    NavigationController(

                        handleGoogleSignIn = { callback ->
                            onGoogleSignInResult = callback
                            handleGoogleSignIn()
                        },
                        loginViewModel,
                    )

                }
            }
        }
    }


    private fun handleDynamicLink(intent: Intent) {
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                pendingDynamicLinkData?.let {
                    deepLinkData = it.link
                    deepLinkData?.let { uri ->
                        Log.d("YYYURI", uri.toString())
                        isDeepLink = true
                        val teamId = uri.getQueryParameter("teamId")
                        Log.d("YYYTeamID", teamId.toString())
                        if (teamId != null && !loginViewModel.invitationHandled) {

                            loginViewModel.handleInvitation(teamId)
                            loginViewModel.setInvitationHandled()
                            Toast.makeText(this, "Invitation link detected", Toast.LENGTH_LONG).show()
                        } else {
                            Log.d("Dynamic Link", "Missing parameters in deep link")
                        }
                    }
                } ?: Log.d("Dynamic Link", "No pending dynamic link data")
            }
            .addOnFailureListener(this) { e -> Log.w("Dynamic Link", "getDynamicLink:onFailure", e) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDynamicLink(intent)
    }

    private fun handleGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, rcSignIn)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == rcSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                onGoogleSignInResult?.invoke(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    user = auth.currentUser
                    loginViewModel.setUser(user)
                    onGoogleSignInResult?.invoke(true)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    onGoogleSignInResult?.invoke(false)
                }
            }
    }




    companion object {
        private const val TAG = "MainActivity"
    }
}