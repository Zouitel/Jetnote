package zouitel.neighborhood.assistant.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.assistant.ui.conversation.rememberAvatarShape

@Composable
fun TextAvatar(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    color: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Box(
        modifier = modifier
            .clip(shape = rememberAvatarShape(loading))
            .background(color)
            .size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 32.sp,
                stepSize = 1.sp
            ),
            lineHeight = 0.8.em
        )
    }
}