package zouitel.neighborhood.feedback.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonColors
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import zouitel.neighborhood.logic.R

data class FeedbackSheetScreen(val background: Int? = null): Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current
        val effect = LocalHapticFeedback.current

        val text = buildAnnotatedString {
            withStyle(SpanStyle(
                color = Color.White,
                fontSynthesis = FontSynthesis.Style,
                fontSize = 25.sp
            )) {
                appendLine(stringResource(R.string.find_an_issue))
            }
            appendLine()
            withStyle(SpanStyle(
                color = Color.LightGray,
                fontSize = 17.sp
            )) {
                appendLine(stringResource(R.string.take_a_screenshot))
            }
        }

        run {
            effect.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        CommonBottomSheet(background) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(200.dp)
            ) {
                AsyncImage(
                    zouitel.neighborhood.feedback.R.drawable.shake,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 50.dp, bottom = 50.dp)
                        .sizeIn(maxWidth = 100.dp, maxHeight = 100.dp)
                )

                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )

                Button(
                    onClick = {
                        navigator?.push(FeedbackScreen())
                    },
                    enabled = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommonColors.prime,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.report_an_issue),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                LaunchedEffect(Unit) {
                    delay(10000)
                    bottomNav.hide()
                }
            }
        }
    }
}