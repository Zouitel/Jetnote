package zouitel.neighborhood.assistant.ai.transformers

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.assistant.model.Assistant
import org.koin.core.component.KoinComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.util.Locale
import java.util.TimeZone

data class PlaceholderCtx(
    val context: Context,
    val model: Model,
    val assistant: Assistant?,
)

interface PlaceholderProvider {
    val placeholders: Map<String, PlaceholderInfo>
}

data class PlaceholderInfo(
    val displayName: @Composable () -> Unit,
    val resolver: (PlaceholderCtx) -> String
)

class PlaceholderBuilder {
    private val placeholders = mutableMapOf<String, PlaceholderInfo>()

    fun placeholder(
        key: String,
        displayName: @Composable () -> Unit,
        resolver: (PlaceholderCtx) -> String
    ) {
        placeholders[key] = PlaceholderInfo(displayName, resolver)
    }

    fun build(): Map<String, PlaceholderInfo> = placeholders.toMap()
}

fun buildPlaceholders(block: PlaceholderBuilder.() -> Unit): Map<String, PlaceholderInfo> {
    return PlaceholderBuilder().apply(block).build()
}

object DefaultPlaceholderProvider : PlaceholderProvider {
    override val placeholders: Map<String, PlaceholderInfo> = buildPlaceholders {
        placeholder("cur_date", { Text("Date") }) {
            LocalDate.now().toDateString()
        }

        placeholder("cur_time", { Text("Time") }) {
            LocalTime.now().toTimeString()
        }

        placeholder("cur_datetime", { Text("Date And Time") }) {
            LocalDateTime.now().toDateTimeString()
        }

        placeholder("model_id", { Text("Model ID") }) {
            it.model.modelId
        }

        placeholder("model_name", { Text("Model Name") }) {
            it.model.displayName
        }

        placeholder("locale", { Text("Local") }) {
            Locale.getDefault().displayName
        }

        placeholder("timezone", { Text("Timezone") }) {
            TimeZone.getDefault().displayName
        }

        placeholder("system_version", { Text("System Version") }) {
            "Android SDK v${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
        }

        placeholder("device_info", { Text("Dvice Info") }) {
            "${Build.BRAND} ${Build.MODEL}"
        }

        placeholder("battery_level", { Text("Battery Level") }) {
            it.context.batteryLevel().toString()
        }

        placeholder("nickname", { Text("User Nickname") }) {
            "user" // it.settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" }
        }

        placeholder("char", { Text("Assistant Name") }) {
            it.assistant?.name?.ifBlank { "assistant" } ?: ""
        }

        placeholder("user", { Text("User") }) {
           "user" //it.settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" }
        }
    }

    private fun Temporal.toDateString() = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Temporal.toTimeString() = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Temporal.toDateTimeString() = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Context.batteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}

object PlaceholderTransformer : InputMessageTransformer, KoinComponent {
    private val defaultProvider = DefaultPlaceholderProvider

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return messages.map {
            it.copy(
                parts = it.parts.map { part ->
                    if (part is UIMessagePart.Text) {
                        part.copy(
                            text = replacePlaceholders(text = part.text, ctx = ctx)
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }

    private fun replacePlaceholders(
        text: String,
        ctx: TransformerContext
    ): String {
        var result = text

        val ctx = PlaceholderCtx(
            context = ctx.context,
            model = ctx.model,
            assistant = ctx.assistant
        )
        defaultProvider.placeholders.forEach { (key, placeholderInfo) ->
            val value = placeholderInfo.resolver(ctx)
            result = result
                .replace(oldValue = "{{$key}}", newValue = value, ignoreCase = true)
                .replace(oldValue = "{$key}", newValue = value, ignoreCase = true)
        }

        return result
    }
}
