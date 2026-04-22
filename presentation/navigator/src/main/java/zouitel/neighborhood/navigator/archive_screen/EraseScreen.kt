package zouitel.neighborhood.navigator.archive_screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonConfirm
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import kotlinx.coroutines.launch
import zouitel.neighborhood.logic.R

data class EraseScreen(val onConfirm: () -> Unit): Screen {
    @Composable
    override fun Content() {
        Erase(
            settingsModel = getScreenModel(),
            dataModel = getScreenModel()
        )
    }

    @Composable
    private fun Erase(
        settingsModel: SettingsScreenModel,
        dataModel: DataScreenModel
    ) {
        val context = LocalContext.current
        val navBottom = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val scope = rememberCoroutineScope()

        CommonBottomSheet(null) {

            LazyColumn {
                item {
                    Text(
                        modifier = Modifier.padding(7.dp),
                        text = stringResource(R.string.erase_notes),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        modifier = Modifier.padding(7.dp),
                        text = stringResource(R.string.confirm_erase_notes),
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                item {
                    CommonConfirm(
                        onConfirm = {
                            scope.launch {
                                sound.performSoundEffect(
                                    context,
                                    CommonConstants.KEY_CLICK,
                                    settings.isSound
                                )
                                dataModel.sendAction(Action.Erase)
                            }.invokeOnCompletion {
                                navBottom.hide()
                            }
                        }
                    )
                }
            }
        }
    }
}