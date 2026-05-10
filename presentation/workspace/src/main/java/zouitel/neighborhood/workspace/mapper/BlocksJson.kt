package zouitel.neighborhood.workspace.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.core.SpanStyle

val BlocksJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "kind"          // avoids collision with BlockType.typeId
    serializersModule = SerializersModule {
        polymorphic(SpanStyle::class) {
            subclass(SpanStyle.Bold::class)
            subclass(SpanStyle.Italic::class)
            subclass(SpanStyle.Underline::class)
            subclass(SpanStyle.StrikeThrough::class)
            subclass(SpanStyle.InlineCode::class)
            subclass(SpanStyle.Highlight::class)
            subclass(SpanStyle.Custom::class)
        }
        polymorphic(BlockContent::class) {
            subclass(BlockContent.Text::class)
            subclass(BlockContent.Empty::class)
            subclass(BlockContent.Custom::class)
        }
        polymorphic(BlockType::class) {
            subclass(BlockType.Paragraph::class)
            subclass(BlockType.Heading::class)
            subclass(BlockType.Todo::class)
            subclass(BlockType.BulletList::class)
            subclass(BlockType.NumberedList::class)
            subclass(BlockType.Quote::class)
            subclass(BlockType.Divider::class)
        }
    }
}