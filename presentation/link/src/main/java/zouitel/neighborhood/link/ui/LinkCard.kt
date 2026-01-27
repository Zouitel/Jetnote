package zouitel.neighborhood.link.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.link.model.Link
import coil.compose.AsyncImage

@Composable
fun LinkCard(link: Link, count: Int, background: Int, textColor: Int) {
    Card(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(.6f, .6f, .6f, .5f),
            contentColor = Color.White
        )
    ) {
        Row {
            AsyncImage(
                modifier = Modifier.size(74.dp),
                model = link.image,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.title,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                )

                Text(
                    text = link.description,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                )
            }

        }
        if (count > 1) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(background)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "+${count - 1} more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(textColor).copy(alpha = .5f)
                )
            }
        }
    }
}