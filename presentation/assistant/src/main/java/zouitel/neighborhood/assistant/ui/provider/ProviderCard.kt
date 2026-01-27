package zouitel.neighborhood.assistant.ui.provider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.ai.provider.ProviderSetting
import coil.compose.AsyncImage

@Composable
fun ProviderCard(provider: ProviderSetting, onClick: (ProviderSetting) -> Unit) {
    Card(
        modifier = Modifier.padding(3.dp).heightIn(max = 280.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable { onClick(provider) },
            contentAlignment = Alignment.BottomStart
        ) {
            // Clear image at top
            AsyncImage(
                provider.logoUrl,
                "Eye monitoring visualization",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Blurred image layer with darkness
            AsyncImage(
                provider.logoUrl,
                null,
                Modifier
                    .fillMaxSize()
                    .blur(
                        radius = 20.dp,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle
                    )
                    .graphicsLayer {
                        alpha = 0.99f
                    }
                    .drawWithContent {
                        drawContent()
                        // Apply gradient mask with darkness
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black,
                                    Color.Black,
                                ),
                                startY = 0f,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        // Add dark overlay
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = size.height * 0.3f,
                                endY = size.height
                            )
                        )
                    },
                contentScale = ContentScale.Crop
            )

            // Title Section overlaying at bottom

                Text(
                    text = provider.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
    }
}