package zouitel.neighborhood.jetnote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import zouitel.neighborhood.account.SyncScreenModel
import zouitel.neighborhood.security.applock.LockModeHandler
import zouitel.neighborhood.audio.player.PlaybackManager
import zouitel.neighborhood.bin.BinScreenModel
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.security.RootCheckerHandler
import zouitel.neighborhood.navigator.main_screen.MainScreen
import zouitel.neighborhood.security.screenshot.ScreenshotHandler
import zouitel.neighborhood.shortcut.checkNoteActivityShortcut
import zouitel.neighborhood.ui.MainTheme
import zouitel.neighborhood.ui.SettingsScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class NoteActivity : AppCompatActivity(), KoinComponent {

    private val settingsModel: SettingsScreenModel by inject()
    private val playerManager: PlaybackManager by inject()
    private val permissionModel: PermissionScreenModel by inject()
    private val binModel: BinScreenModel by inject()
    private val syncModel: SyncScreenModel by inject()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        permissionModel.activityBind(this)

        setContent {
            Navigator(RootCheckerHandler())
            LockModeHandler(settingsModel)
            ScreenshotHandler(window, settingsModel)

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
        binModel.initBinWorker()
//        syncModel.enqueueUploadsProcessing()
    }

    override fun onResume() {
        super.onResume()
        checkNoteActivityShortcut(this)
    }
}