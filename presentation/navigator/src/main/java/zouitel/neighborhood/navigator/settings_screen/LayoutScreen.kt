package zouitel.neighborhood.navigator.settings_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonColors.azure
import zouitel.neighborhood.ui.CommonConstants.GRID
import zouitel.neighborhood.ui.CommonConstants.LIST
import zouitel.neighborhood.ui.SettingsScreenModel
import coil.compose.AsyncImage
import zouitel.neighborhood.navigator.R

class LayoutScreen: Screen {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val settingsModel = getScreenModel<SettingsScreenModel>()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        CommonBottomSheet(null) {
            Text(
                text = "Select layout",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 150.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.layout == GRID) azure
                        else Color.Transparent
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        AsyncImage(
                            model = R.drawable.dashboard,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(5.dp)
                                .clickable {
                                    settingsModel.update(settings.copy(layout = GRID))
                                }
                        )

                        RadioButton(
                            selected = settings.layout == GRID,
                            onClick = {
                                settingsModel.update(settings.copy(layout = GRID))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.LightGray,
                                unselectedColor = Color.LightGray
                            )
                        )
                    }
                }
                Card(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.layout == LIST) azure
                        else Color.Transparent
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        AsyncImage(
                            model = R.drawable.threelines,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(5.dp)
                                .clickable {
                                    settingsModel.update(settings.copy(layout = LIST))
                                }
                        )
                        RadioButton(
                            selected = settings.layout == LIST,
                            onClick = {
                                settingsModel.update(settings.copy(layout = LIST))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.LightGray,
                                unselectedColor = Color.LightGray
                            )
                        )
                    }
                }
            }
        }
    }
}