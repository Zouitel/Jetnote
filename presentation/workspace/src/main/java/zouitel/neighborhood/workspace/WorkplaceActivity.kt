package zouitel.neighborhood.workspace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import zouitel.neighborhood.ui.MainTheme
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.provider.SharedScreen
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import kotlin.getValue

@SuppressLint("RestrictedApi")
class WorkplaceActivity: AppCompatActivity(), KoinComponent {
    private val settingsModel: SettingsScreenModel by inject()
    private var currentNote: Data? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the intent on first launch
        handleIncomingIntent(intent)

        setContent {
            val workplaceScreen = rememberScreen(
                    SharedScreen.Workplace(
                        currentNote ?: Data(),
                        false
                    )
                )

            MainTheme(settingsModel) {
                BottomSheetNavigator(
                    sheetShape = ShapeDefaults.Large,
                    sheetElevation = 150.dp
                ) {
                    Navigator(workplaceScreen)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        recreate()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        // Handle both ACTION_VIEW and your custom action
        val isWorkplaceIntent = intent?.action == Intent.ACTION_VIEW

        if (isWorkplaceIntent && intent.extras?.containsKey("start_workplace") == true) {
            runCatching {
                val noteJson = intent.getStringExtra("data")
                if (noteJson != null) {
                    currentNote = JsonInstant.decodeFromString<Data>(noteJson)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy().runCatching {
            currentNote = null
        }.onFailure {
            it.printStackTrace()
        }
    }
}