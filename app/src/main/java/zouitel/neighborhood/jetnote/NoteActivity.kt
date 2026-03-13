package zouitel.neighborhood.jetnote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import zouitel.neighborhood.security.applock.LockModeHandler
import zouitel.neighborhood.audio.player.PlaybackManager
import zouitel.neighborhood.bin.BinScreenModel
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.navigator.main_screen.MainScreen
import zouitel.neighborhood.security.screenshot.ScreenshotHandler
import zouitel.neighborhood.shortcut.checkNoteActivityShortcut
import zouitel.neighborhood.ui.MainTheme
import zouitel.neighborhood.ui.SettingsScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import zouitel.neighborhood.account.utils.DriveCleanupScreenModel
import zouitel.neighborhood.security.detection.SecurityCheckingScreen
import zouitel.neighborhood.security.detection.SecurityHandler
import zouitel.neighborhood.security.detection.SecurityState
import zouitel.neighborhood.security.detection.SecurityThreatScreen
import zouitel.neighborhood.ui.hightlight.Highlighter
import zouitel.neighborhood.ui.hightlight.LocalHighlighter

class NoteActivity : AppCompatActivity(), KoinComponent {

    private val settingsModel: SettingsScreenModel by inject()
    private val playerManager: PlaybackManager by inject()
    private val permissionModel: PermissionScreenModel by inject()
    private val binModel: BinScreenModel by inject()
    private val driveCleanupModel: DriveCleanupScreenModel by inject()
    private val highlighter: Highlighter by inject()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var securityHandler: SecurityHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        permissionModel.activityBind(this)

        // Initialize security handler
        securityHandler = SecurityHandler(this)

        // Perform security checks
        lifecycleScope.launch {
            securityHandler.checkSecurity()
        }

        setContent {
            val securityState by securityHandler.securityState.collectAsState()

            // Show security screen if threat detected
            when (securityState) {
                is SecurityState.Checking -> {
                    SecurityCheckingScreen()
                }
                is SecurityState.Threat -> {
                    SecurityThreatScreen(
                        result = (securityState as SecurityState.Threat).result,
                        onExit = {
                            finish()
                        }
                    )
                }
                is SecurityState.Secure -> {
                    MainApp()
                }
                is SecurityState.Error -> {
                    // Show error but allow to continue
                    MainApp()
                }
            }
        }
    }

    @Composable
    private fun MainApp() {
        LockModeHandler(settingsModel)
        ScreenshotHandler(window, settingsModel)

        CompositionLocalProvider(LocalHighlighter provides highlighter) {
            MainTheme(settingsModel) {
                BottomSheetNavigator(
                    sheetShape = ShapeDefaults.Large,
                    sheetElevation = 150.dp
                ) {
                    Navigator(MainScreen())
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
//        ListReceiver.updateBroadcast(this)
        // Cancel the performUiEvent when the activity is destroyed
        activityScope.cancel()
        playerManager.releaseController()
    }

    override fun onStop() {
        super.onStop()
        driveCleanupModel.executeManualCleanup()
        binModel.initBinWorker()
//        syncModel.enqueueUploadsProcessing()
    }

    override fun onResume() {
        super.onResume()
        checkNoteActivityShortcut(this)
    }
}