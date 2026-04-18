package zouitel.neighborhood.assistant.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.assistant.ui.conversation.rememberAvatarShape
import zouitel.neighborhood.ui.getAverageColorFromImage

@Composable
fun Avatar(isLoading: Boolean, logo: String?, modelName: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val avgColor = remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(Unit) {
        avgColor.value = getAverageColorFromImage(context, logo ?: return@LaunchedEffect)
    }

    IconButton(
        onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isLoading || logo.isNullOrEmpty()) avgColor.value else Color.Transparent
        ),
        shape = rememberAvatarShape(isLoading)
    ) {
        if (logo.isNullOrEmpty()) {
            TextAvatar(modelName)
        } else {
            AsyncImage(
                logo,
                "avatar",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun TextAvatar(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(shape = rememberAvatarShape(loading))
            .size(30.dp),
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