package zouitel.neighborhood.account.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.account.utils.PhotoProfile
import zouitel.neighborhood.logic.R
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
        val storageInfo by remember(accountModel, accountModel::storageInfo).collectAsState()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                    contentDescription = null
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(15.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.log_out),
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
                            text = ("${stringResource(R.string.hi)}, ${it.displayName}"),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                item {
                    StorageCard(storageInfo)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(15.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.add_account),
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(15.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.manage_account),
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
                                    contentDescription = null,
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.upload_data),
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
                                    contentDescription = null,
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(15.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.download_data),
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

    @Composable
    private fun StorageCard(info: StorageInfo) {
        val totalFraction = if (info.quotaBytes > 0) {
            (info.usedBytes.toFloat() / info.quotaBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val notesFraction  = if (info.quotaBytes > 0) (info.notesBytes.toFloat() / info.quotaBytes).coerceIn(0f, 1f) else 0f
        val mediaFraction  = if (info.quotaBytes > 0) (info.mediaBytes.toFloat()  / info.quotaBytes).coerceIn(0f, 1f) else 0f
        val cacheFraction  = if (info.quotaBytes > 0) (info.cacheBytes.toFloat()  / info.quotaBytes).coerceIn(0f, 1f) else 0f

        val animatedTotal by animateFloatAsState(
            targetValue = totalFraction,
            animationSpec = tween(durationMillis = 900),
            label = "storage_total"
        )

        val safeTotal = totalFraction.coerceAtLeast(0.001f)
        val notesColor = CommonColors.prime
        val mediaColor = Color(0xFF4FC3F7)   // light blue
        val cacheColor = Color(0xFFFFB74D)   // amber

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(CommonIcons.UPLOAD_CLOUD),
                    contentDescription = null,
                    tint = CommonColors.prime,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Used / quota headline
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatBytes(info.usedBytes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ ${formatBytes(info.quotaBytes)} used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val nW = (notesFraction * animatedTotal / safeTotal).coerceAtLeast(0f)
                    val mW = (mediaFraction  * animatedTotal / safeTotal).coerceAtLeast(0f)
                    val cW = (cacheFraction  * animatedTotal / safeTotal).coerceAtLeast(0f)
                    val fW = (1f - nW - mW - cW).coerceAtLeast(0f)

                    if (nW > 0f) Box(modifier = Modifier.weight(nW).height(10.dp).background(notesColor))
                    if (mW > 0f) Box(modifier = Modifier.weight(mW).height(10.dp).background(mediaColor))
                    if (cW > 0f) Box(modifier = Modifier.weight(cW).height(10.dp).background(cacheColor))
                    if (fW > 0f) Box(modifier = Modifier.weight(fW).height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            StorageLegendRow(notesColor, "Notes & data", info.notesBytes)
            Spacer(modifier = Modifier.height(8.dp))
            StorageLegendRow(mediaColor, "Media files", info.mediaBytes)
            Spacer(modifier = Modifier.height(8.dp))
            StorageLegendRow(cacheColor, "Cache", info.cacheBytes)
            Spacer(modifier = Modifier.height(8.dp))
            StorageLegendRow(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .25f),
                label = "Free",
                bytes = info.freeBytes
            )
        }
    }

    @Composable
    private fun StorageLegendRow(color: Color, label: String, bytes: Long) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatBytes(bytes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024        -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024L               -> "%.0f KB".format(bytes / 1024.0)
        else                         -> "$bytes B"
    }
}