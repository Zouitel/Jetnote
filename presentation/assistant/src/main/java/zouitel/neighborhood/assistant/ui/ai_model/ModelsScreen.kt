package zouitel.neighborhood.assistant.ui.ai_model

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.ui.configurations.ModelConfigurationScreen
import zouitel.neighborhood.assistant.ui.provider.ProvidersScreen
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FullModels(
    assistantModel: AssistantScreenModel,
    modelsModel: ModelsScreenModel,
    settingsModel: SettingsScreenModel
) {
    val navigator = LocalNavigator.current
    val bottomNav = LocalBottomSheetNavigator.current

    val settings by remember(settingsModel, settingsModel::observe).collectAsState()

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val jobCounter = remember { mutableIntStateOf(0) }
    val deletedModel = remember { mutableStateListOf<Pair<Model, Int>>() }
    val (lazyListState, height) = rememberAutoHeightCacheLazyList()

    val deletedResource = stringResource(R.string.deleted)
    val undoResource = stringResource(R.string.action_undo)

    Scaffold(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .reportable(settings.isShakable)
            .cloudy(enabled = bottomNav.isVisible),
        {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TopAppBar(
                    navigationIcon = {
                        IconButton({
                            navigator?.pop()
                        }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), null)
                        }
                    },
                    title = { Text(stringResource(R.string.ai_models)) },
                    actions = {
                        IconButton({
                            navigator?.push(ProvidersScreen())
                        }) {
                            Icon(painterResource(CommonIcons.PLUS), null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(
                    snackbarData = data,
                    actionOnNewLine = false,
                    shape = MaterialTheme.shapes.extraLarge,
                    actionColor = CommonColors.prime
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            Modifier.fillMaxSize()
                .then(height),
            lazyListState,
            contentPadding = paddingValues
        ) {
            item {
                Models(
                    assistantModel, modelsModel, false,
                    {
                        navigator?.push(ModelConfigurationScreen(it))
                    },
                    {
                        val currentJobId = jobCounter.intValue++
                        deletedModel.add(Pair(it, currentJobId))
                        val modelPair =
                            deletedModel.firstOrNull { pair -> pair.second == currentJobId }

                        modelsModel.delete(it.modelId)

                        scope.launch {
                            snackbarState.currentSnackbarData?.dismiss()
                            val result = snackbarState.showSnackbar(
                                message = "${modelPair?.first?.displayName} $deletedResource",
                                actionLabel = undoResource,
                                duration = SnackbarDuration.Long
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                modelPair?.first?.let { model ->
                                    modelsModel.upsert(model)
                                }
                            }
                            modelPair?.let { model -> deletedModel.remove(model) }
                        }
                    })
            }
        }
    }
}
