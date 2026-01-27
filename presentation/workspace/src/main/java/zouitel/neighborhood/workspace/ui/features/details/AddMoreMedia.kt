package zouitel.neighborhood.workspace.ui.features.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ui.CommonIcons

@Composable
fun BoxScope.AddMoreMedia() {
    VerticalFloatingToolbar(
        expanded = true,
        modifier = Modifier
            .animateContentSize()
            .align(Alignment.CenterEnd)
            .padding(horizontal = 10.dp),
    ) {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(painterResource(CommonIcons.GALLERY), contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(painterResource(CommonIcons.CAMERA), contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(painterResource(CommonIcons.VIDEO_CAMERA), contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(painterResource(CommonIcons.WAVEFORM), contentDescription = "Localized description")
        }
    }
}