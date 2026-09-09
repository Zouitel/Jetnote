package zouitel.neighborhood.workspace.ui.bottom_bar

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonConstants.FOCUS_NAVIGATION
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonIcons.TINT_SLASH
import zouitel.neighborhood.ui.CommonRow
import zouitel.neighborhood.ui.SoundEffect
import zouitel.neighborhood.ui.listOfBackgroundColors
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import zouitel.neighborhood.workspace.ui.features.OptionsScreen
import zouitel.neighborhood.workspace.ui.utils.listOfTextColors
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import zouitel.neighborhood.markdown.state.EditorStateHolder
import zouitel.neighborhood.workspace.R
import kotlin.math.abs

@SuppressLint("SimpleDateFormat")
@Composable
internal fun BottomBar(
    data: MutableState<Data>,
    titleState: TextFieldState?,
    editorState: EditorStateHolder,
    isSound: Boolean?,
    dataModel: DataScreenModel,
    linkModel: LinkScreenModel,
    backgroundState: MutableState<Int>,
    textColorState: MutableState<Int>
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val bottomNav = LocalBottomSheetNavigator.current
    val sound by lazy { SoundEffect() }
    val auth = FirebaseAuth.getInstance()
    val untitledResource = stringResource(zouitel.neighborhood.logic.R.string.untitled)

    val isImeVisible = WindowInsets.isImeVisible

    AnimatedVisibility(
        visible = !isImeVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color(backgroundState.value),
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = .6f))
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row {
                        CommonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .align(Alignment.Bottom)
                        ) {
                            IconButton(
                                onClick = {
                                    sound.performSoundEffect(context, FOCUS_NAVIGATION, isSound)
                                    bottomNav.show(
                                        OptionsScreen(
                                            data = data,
                                            titleState = titleState,
                                            titleState
                                        )
                                    )
                                }) {
                                Icon(
                                    painter = painterResource(CommonIcons.PLUS),
                                    contentDescription = null,
                                    tint = Color.LightGray
                                )
                            }

                            // undo
                            UndoRedo(
                                isSound = isSound,
                                titleState = titleState,
                                titleState,
                            )

                            IconButton(
                                onClick = {
                                    sound.performSoundEffect(
                                        context,
                                        CommonConstants.KEY_STANDARD,
                                        isSound
                                    )
                                    dataModel.sendAction(
                                        Action.Insert(
                                            data.value.copy(
                                                userUid = auth.uid.toString(),
                                                title = titleState?.text.toString().ifEmpty { untitledResource },
                                                position = 2,
                                                blocks = editorState.state.blocks,
                                                background = backgroundState.value,
                                                textColor = textColorState.value
                                            )
                                        )
                                    )
                                    linkModel.urlsProcessing(
                                        data.value.uid,
                                        "..."
                                    )
                                    navigator?.items?.size?.let {
                                        if (it > 1) {
                                            navigator.pop()
                                        } else {
                                            (context as Activity).finishAfterTransition()
                                        }
                                    }
                                }) {
                                Icon(
                                    painter = painterResource(CommonIcons.CHECK),
                                    contentDescription = null,
                                    tint = Color.LightGray
                                )
                            }
                        }
                    }
                }

                // row of background colors.
                ColorsRow(
                    isSound = isSound,
                    colors = listOfBackgroundColors
                ) { color ->
                    backgroundState.value = color
                }

                // row of text colors.
                ColorsRow(
                    isSound = isSound,
                    colors = listOfTextColors
                ) { color ->
                    textColorState.value = color
                }
            }
        }
    }
}
@Composable
private fun ColorsRow(
    isSound: Boolean?,
    colors: Array<Color>,
    onAction: (Int) -> Unit
) {
    val currentColor = remember { mutableStateOf(Color.White) }
    val selectedIndex = remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    val bottomNav = LocalBottomSheetNavigator.current
    val defaultBackgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val sound = SoundEffect()

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
                        sound.performSoundEffect(context, KEY_CLICK, isSound)
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
            IconButton({
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