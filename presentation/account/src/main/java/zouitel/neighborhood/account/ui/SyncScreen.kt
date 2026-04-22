package zouitel.neighborhood.account.ui

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
import androidx.compose.ui.res.stringResource
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
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import zouitel.neighborhood.account.utils.DriveSignIn
import zouitel.neighborhood.logic.R

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
        val webClientId by remember(syncModel, syncModel::googleClientId).collectAsState()

        val authenticationFailedResource = stringResource(R.string.firebase_authentication_failed)
        val  signInFailedResource = stringResource(R.string.sign_in_failed)

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
                            isSigningIn = false
                        }
                        .addOnFailureListener { e ->
                            errorState = "${authenticationFailedResource}: ${e.message}"
                            isSigningIn = false
                        }
                }
            } catch (e: ApiException) {
                errorState = "${signInFailedResource}: ${e.localizedMessage}"
                isSigningIn = false
            }
        }

        val gso = rememberGso(webClientId)
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
                    Text(stringResource(R.string.please_wait))
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
        Text(stringResource(R.string.sync_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.sync_description),
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
        Text(stringResource(R.string.current_account), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                ImageRequest.Builder(LocalContext.current).data(data = user?.photoUrl)
                    .apply(block = fun ImageRequest.Builder.() {
                        crossfade(true)
                    }).build(),
                null,
                Modifier
                    .size(65.dp)
                    .padding(5.dp)
                    .clip(CircleShape)
            )
            Column {
                Text(
                    text = user?.displayName ?: stringResource(R.string.no_name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user?.email ?: stringResource(R.string.no_email),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out))
        }
    }

    @Composable
    private fun rememberGso(webClientId: String): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }
}