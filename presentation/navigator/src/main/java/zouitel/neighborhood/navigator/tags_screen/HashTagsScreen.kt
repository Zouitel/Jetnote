package zouitel.neighborhood.navigator.tags_screen

//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonIcons.TINT_SLASH
import zouitel.neighborhood.ui.CommonTopAppBar
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.listOfBackgroundColors
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.workspace.R
import zouitel.neighborhood.workspace.ui.bottom_bar.ColorPicker
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

class HashTagsScreen: Screen {
    @Composable
    override fun Content() {
        Tags(
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel()
        )
    }

    @SuppressLint(
        "UnusedMaterial3ScaffoldPaddingParameter",
        "UnusedMaterialScaffoldPaddingParameter"
    )
    @Composable
    private fun Tags(
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        searchModel: SearchScreenModel,
        chatModel: ChatScreenModel
    ) {
        val keyboardManager = LocalFocusManager.current
        val bottomNav = LocalBottomSheetNavigator.current
        val haptic = LocalHapticFeedback.current

        val uiState by remember(tagModel, tagModel::uiState).collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val snackbarState = remember { SnackbarHostState() }
        val textFieldState = rememberTextFieldState(uiState.currentLabel)
        val focusRequester by lazy { FocusRequester() }
        val colorState = remember { mutableStateOf<Int?>(null) }
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()
        val scope = rememberCoroutineScope()
        val tagResource = stringResource(zouitel.neighborhood.logic.R.string.tag_placeholder)
        val tagCannotBeEmptyResource = stringResource(zouitel.neighborhood.logic.R.string.tag_cannot_be_empty)
        var placeholderText by remember { mutableStateOf(tagResource) }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarState) { data ->
                    Snackbar(
                        snackbarData = data,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = CommonColors.prime
                    )
                }
            }
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    NavigationDrawer(
                        settingsModel = settingsModel,
                        tagModel = tagModel,
                        chatModel = chatModel,
                        searchModel = searchModel,
                        drawerState = drawerState,
                        snackbarState = snackbarState,
                        modelsModel = koinScreenModel(),
                        assistantModel = koinScreenModel()
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .reportable(settings.isShakable)
                        .cloudy(enabled = bottomNav.isVisible || drawerState.isOpen),
                    topBar = {
                        CommonTopAppBar(
                            settingsModel = settingsModel,
                            drawerState = drawerState,
                            scrollBehavior = scrollBehavior,
                            title = stringResource(zouitel.neighborhood.logic.R.string.screen_tags)
                        )
                    },
                    bottomBar = {
                        ColorsRow(listOfBackgroundColors) {
                            colorState.value = it
                        }
                    }
                ) { paddingValues ->
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(height)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        item {
                            HashTagLayout(tagModel, snackbarState) { tag ->
                                textFieldState.apply {
                                    clearText()
                                    edit { append(tag.label) }
                                }
                                tagModel.updateId(tag.id)
                                colorState.value = tag.color
                            }
                        }

                        item {
                            OutlinedTextField(
                                state = textFieldState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                    .focusRequester(focusRequester)
                                    .onFocusEvent { keyboardManager.moveFocus(FocusDirection.Enter) },
                                shape = MaterialTheme.shapes.extraLarge,
                                textStyle = MaterialTheme.typography.titleLarge,
                                placeholder = { Text(placeholderText, style = MaterialTheme.typography.titleLarge) },
                                trailingIcon = {
                                    IconButton({
                                        tagModel.updateId().updateLabel()
                                        textFieldState.clearText()
                                        colorState.value = null
                                    }) {
                                        AnimatedVisibility(textFieldState.text.isNotEmpty()) {
                                            Icon(
                                                painterResource(CommonIcons.CROSS_18),
                                                null,
                                                tint = colorState.value?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),

                                onKeyboardAction = KeyboardActionHandler {
                                    val inputTag = textFieldState.text.toString().trim()

                                    if (inputTag.isEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        placeholderText = tagCannotBeEmptyResource
                                        scope.launch {
                                            delay(3000.milliseconds)
                                            placeholderText = tagResource
                                        }
                                    } else {
                                        tagModel.sendAction(
                                            Action.Insert(
                                                data = Tag(
                                                    id = uiState.currentId,
                                                    label = inputTag,
                                                    color = colorState.value
                                                )
                                            )
                                        )
                                        tagModel.updateId().updateLabel()
                                        textFieldState.clearText()
                                        colorState.value = null
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedContainerColor = colorState.value?.let { Color(it).copy(alpha = .5f) } ?: MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .5f),
                                    focusedTextColor = colorState.value?.let { Color(it) } ?: Color.Unspecified
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ColorsRow(colors: Array<Color>, onAction: (Int) -> Unit) {
        val currentColor = remember { mutableStateOf(Color.White) }
        val bottomNav = LocalBottomSheetNavigator.current
        val selectedIndex = remember { mutableIntStateOf(-1) }
        val defaultBackgroundColor = MaterialTheme.colorScheme.surface.toArgb()

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                itemsIndexed(items = colors) { index, color ->
                    val distance = abs(index - selectedIndex.intValue)
                    val scale = remember { Animatable(1f) }

                    LaunchedEffect(selectedIndex.intValue) {
                        if (selectedIndex.intValue != -1) {
                            when (distance) {
                                0 -> {
                                    scale.animateTo(1.2f, tween(100))
                                    scale.animateTo(1f, tween(100))
                                }
                                1 -> {
                                    scale.animateTo(1.1f, tween(100))
                                    scale.animateTo(1f, tween(100))
                                }
                                2 -> {
                                    scale.animateTo(1.05f, tween(100))
                                    scale.animateTo(1f, tween(100))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))
                    Canvas(
                        modifier = Modifier
                            .size(27.dp)
                            .scale(scale.value)
                            .clickable {
                                currentColor.value = color
                                selectedIndex.intValue = index
                                onAction(color.toArgb())
                            }
                    ) {
                        drawArc(
                            color = color,
                            startAngle = 1f,
                            sweepAngle = 360f,
                            useCenter = true,
                            style =
                                if (currentColor.value == color) {
                                    Stroke(width = 10f, cap = StrokeCap.Round)
                                } else {
                                    Fill
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
                item {
                    androidx.compose.material3.IconButton({
                        onAction(defaultBackgroundColor)
                    }) {
                        Icon(
                            painter = painterResource(id = TINT_SLASH),
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
                item {
                    IconButton(
                        modifier = Modifier.size(38.dp),
                        onClick = {
                            bottomNav.show(ColorPicker(onAction))
                        }) {
                        AsyncImage(
                            model = R.drawable.color_wheel,
                            contentDescription = null,
                            modifier = Modifier.clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
        }
    }
}