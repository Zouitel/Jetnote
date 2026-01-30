package zouitel.neighborhood.account

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.CommonBottomSheet
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
import com.google.android.gms.common.api.ApiException
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SyncScreen: Screen {
    @Composable
    override fun Content() {
        val bottomNav = LocalBottomSheetNavigator.current
        val syncModel = getScreenModel<SyncScreenModel>()
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val scope = rememberCoroutineScope()

        var isSigningIn by remember { mutableStateOf(false) }
        var errorState by remember { mutableStateOf<String?>(null) }

        // Use the ViewModel's user state instead of local state
        val currentUser by syncModel.userState.collectAsState()

        val signInLauncher = rememberLauncherForActivityResult(
            contract = StartActivityForResult()
        ) { result ->
            isSigningIn = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let { acc ->
                    val credential = GoogleAuthProvider.getCredential(acc.idToken, null)

                    auth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            Log.d("SyncScreen", "Sign in successful: ${auth.currentUser?.email}")
                            // ✅ FIXED: Don't call createDriveFolders here!
                            // The AuthStateListener in SyncScreenModel will handle everything
                            isSigningIn = false
                        }
                        .addOnFailureListener { e ->
                            Log.e("SyncScreen", "Sign in failed", e)
                            errorState = "Firebase authentication failed: ${e.message}"
                            isSigningIn = false
                        }
                }
            } catch (e: ApiException) {
                Log.e("SyncScreen", "Google sign in failed", e)
                errorState = "Google sign-in failed: ${e.localizedMessage}"
                isSigningIn = false
            }
        }

        val gso = rememberGso()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        CommonBottomSheet(null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Signing in, please wait...")
                } else if (currentUser != null) {
                    SignedIdDialog(
                        user = currentUser,
                        onSignOut = {
                            scope.launch {
                                auth.signOut()
                                googleSignInClient.signOut()
                                // State will update automatically via listener
                            }.invokeOnCompletion {
                                bottomNav.hide()
                            }
                        }
                    )
                } else {
                    SignInDialog(
                        error = errorState,
                        onSignIn = {
                            errorState = null
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun SignInDialog(error: String?, onSignIn: () -> Unit) {
        Text("Sign In to Sync", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sign in with your Google Account to back up and sync your notes across all your devices.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        DriveSignIn(onSignIn)
    }

    @Composable
    fun SignedIdDialog(user: FirebaseUser?, onSignOut: () -> Unit) {
        Text("Current Account", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                ImageRequest.Builder(LocalContext.current).data(data = user?.photoUrl)
                    .apply(block = fun ImageRequest.Builder.() {
                        crossfade(true)
                    }).build()
                ,
                "User Profile Picture",
                Modifier
                    .size(65.dp)
                    .padding(5.dp)
                    .clip(CircleShape)
            )
            Column {
                Text(
                    text = user?.displayName ?: "No Name",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user?.email ?: "No Email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onSignOut) {
            Text("Sign Out")
        }
    }

    @Composable
    private fun rememberGso(): GoogleSignInOptions {
        val webClientId = "866712879346-lsqit4thufnkc8ldn6tur5fqlnphlqc6.apps.googleusercontent.com"
        return GoogleSignInOptions.Builder(DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
    }
}