package zouitel.neighborhood.account.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.account.utils.PhotoProfile
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonIcons.USER_ADD
import zouitel.neighborhood.ui.CommonIcons.USER_GEAR
import zouitel.neighborhood.ui.CommonIcons.USER_LOGOUT

class AccountScreen: Screen {
    @Composable
    override fun Content() {
        Account(
            getScreenModel(),
            getScreenModel()
        )
    }

    @Composable
    private fun Account(
        accountModel: AccountScreenModel,
        syncModel: SyncScreenModel
    ) {
        val navigator = LocalNavigator.current
        val currentUser by remember(accountModel, accountModel::currentUser).collectAsState()
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val isUploading by remember(syncModel, syncModel::isUploading).collectAsState()
        val isDownloading by remember(syncModel, syncModel::isDownloading).collectAsState()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton({
                            navigator?.pop()
                        }) {
                            Icon(
                                painter = painterResource(CommonIcons.ARROW_LEFT),
                                contentDescription = "button back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = 30.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f))
                            .clickable {
                                accountModel.signOut()
                                navigator?.pop()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(USER_LOGOUT),
                            contentDescription = "logout",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(15.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 15.dp, horizontal = 5.dp)
                        )
                    }
                }
            }
        ) { contentPadding ->
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = contentPadding,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .padding(vertical = 20.dp, horizontal = 30.dp)
            ) {
                item {
                    currentUser?.let {
                        PhotoProfile(it.photoUrl)
                    }
                }

                item {
                    currentUser?.let {
                        Text(
                            text = ("Hi, ${it.displayName}"),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                item {
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .8f))
                    ) {
                        // Add another account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountModel.addAnotherAccount() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(USER_ADD),
                                contentDescription = "Add another account",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(15.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Add another account",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(vertical = 15.dp, horizontal = 5.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

                        // Manage accounts on this device
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountModel.manageAccountOnThisDevice() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(USER_GEAR),
                                contentDescription = "manage accounts",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(15.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Manage accounts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(vertical = 15.dp, horizontal = 5.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

                        // Upload user data.
                        if (isUploading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncModel.enqueueUploadsProcessing() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(CommonIcons.CROSS_18),
                                    contentDescription = "cancel",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                AnimatedVisibility(true) {
                                    LinearProgressIndicator(
                                        color = CommonColors.prime,
                                        trackColor = Color.Transparent
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncModel.enqueueUploadsProcessing() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(CommonIcons.UPLOAD_CLOUD),
                                    contentDescription = "uploads",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Upload Data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(vertical = 15.dp, horizontal = 5.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

                        // Download user data.
                        if (isDownloading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncModel.enqueueDownloadsProcessing() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(CommonIcons.CROSS_18),
                                    contentDescription = "download",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                AnimatedVisibility(true) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.graphicsLayer(scaleX = -1f),
                                        color = CommonColors.prime,
                                        trackColor = Color.Transparent
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { syncModel.enqueueDownloadsProcessing() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(CommonIcons.DOWNLOAD_CLOUD),
                                    contentDescription = "download",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Download Data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(vertical = 15.dp, horizontal = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}