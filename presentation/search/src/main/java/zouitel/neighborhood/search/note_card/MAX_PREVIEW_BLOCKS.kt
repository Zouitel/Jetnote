package zouitel.neighborhood.search.note_card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.core.SpanStyle as CascadeSpanStyle

private const val MAX_PREVIEW_BLOCKS = 8

@Composable
fun BlocksPreview(
    blocks: List<Block>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val previewBlocks = remember(blocks) { blocks.take(MAX_PREVIEW_BLOCKS) }
    val overflow = blocks.size - previewBlocks.size

    Column(modifier = modifier) {
        previewBlocks.forEach { block ->
            BlockPreviewItem(block = block, textColor = textColor)
        }
        if (overflow > 0) {
            Text(
                text = "+$overflow more…",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun BlockPreviewItem(block: Block, textColor: Color) {
    val indentPadding = (block.attributes.indentationLevel * 16).dp

    when (val type = block.type) {

        is BlockType.Paragraph -> {
            val text = block.annotatedText(textColor) ?: return
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPadding + 4.dp, end = 4.dp, bottom = 2.dp),
            )
        }

        is BlockType.Heading -> {
            val text = block.annotatedText(textColor) ?: return
            val (weight, size) = when (type.level) {
                1 -> FontWeight.ExtraBold to 20.sp
                2 -> FontWeight.Bold      to 18.sp
                3 -> FontWeight.Bold      to 16.sp
                4 -> FontWeight.SemiBold  to 15.sp
                5 -> FontWeight.SemiBold  to 14.sp
                else -> FontWeight.Medium to 13.sp
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = weight,
                    fontSize = size,
                    color = textColor,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 2.dp, top = if (type.level <= 2) 4.dp else 2.dp),
            )
        }

        is BlockType.Todo -> {
            val text = block.annotatedText(textColor) ?: return
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPadding + 4.dp, end = 4.dp, bottom = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(
                            color = if (type.checked)
                                textColor.copy(alpha = 0.7f)
                            else
                                Color.Transparent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                        )
                        .then(
                            if (!type.checked) Modifier.background(
                                color = textColor.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                            ) else Modifier
                        ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (type.checked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (type.checked) textColor.copy(alpha = 0.45f) else textColor,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is BlockType.BulletList -> {
            val text = block.annotatedText(textColor) ?: return
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPadding + 4.dp, end = 4.dp, bottom = 2.dp),
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(top = 1.dp, end = 6.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is BlockType.NumberedList -> {
            val text = block.annotatedText(textColor) ?: return
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentPadding + 4.dp, end = 4.dp, bottom = 2.dp),
            ) {
                Text(
                    text = "${type.number}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(top = 1.dp, end = 6.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is BlockType.Quote -> {
            val text = block.annotatedText(textColor) ?: return
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(with(androidx.compose.ui.platform.LocalDensity.current) {
                            // intrinsic height of roughly 2 text lines
                            (MaterialTheme.typography.bodyMedium.fontSize.value * 2 * 1.5f).dp
                        })
                        .background(textColor.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = textColor.copy(alpha = 0.75f),
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is BlockType.Divider -> {
            HorizontalDivider(
                color = textColor.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        else -> Unit
    }
}

private fun Block.annotatedText(fallbackColor: Color): AnnotatedString? {
    val content = content as? BlockContent.Text ?: return null
    if (content.text.isBlank()) return null

    return buildAnnotatedString {
        append(content.text)
        content.spans.forEach { span ->
            val composeStyle = span.style.toComposeSpanStyle(fallbackColor) ?: return@forEach
            addStyle(composeStyle, span.start, span.end.coerceAtMost(content.text.length))
        }
    }
}

private fun CascadeSpanStyle.toComposeSpanStyle(textColor: Color): SpanStyle? = when (this) {
    is CascadeSpanStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    is CascadeSpanStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    is CascadeSpanStyle.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
    is CascadeSpanStyle.StrikeThrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    is CascadeSpanStyle.InlineCode -> SpanStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        background = textColor.copy(alpha = 0.12f),
        fontSize = 12.sp,
    )

    is CascadeSpanStyle.Highlight -> SpanStyle(background = Color(colorArgb))
    is CascadeSpanStyle.Custom -> null
}