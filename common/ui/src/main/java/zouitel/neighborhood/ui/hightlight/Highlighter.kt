package zouitel.neighborhood.ui.hightlight

import android.content.Context
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSArray
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import zouitel.neighborhood.ui.R
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Highlighter(ctx: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val destroyed = AtomicBoolean(false)

    init {
        executor.submit {
            QuickJSLoader.init()

            context // init context
        }
    }

    private val script: String by lazy {
        ctx.resources.openRawResource(R.raw.prism).use {
            it.bufferedReader().readText()
        }
    }

    private val contextDelegate = lazy {
        QuickJSContext.create().also {
            it.evaluate(script)
        }
    }
    private val context: QuickJSContext by contextDelegate

    private val highlightFn by lazy {
        context.globalObject.getJSFunction("highlight")
    }

    suspend fun highlight(code: String, language: String) =
        suspendCancellableCoroutine { continuation ->
            if (destroyed.get()) {
                continuation.resumeWithException(IllegalStateException("Highlighter has been destroyed"))
                return@suspendCancellableCoroutine
            }
            try {
                executor.submit {
                    if (destroyed.get()) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException("Highlighter has been destroyed"))
                        }
                        return@submit
                    }

                    try {
                        val result = highlightFn.call(code, language)
                        require(result is QuickJSArray) {
                            "highlight result must be an array"
                        }
                        val tokens = try {
                            buildList {
                                for (i in 0 until result.length()) {
                                    when (val element = result[i]) {
                                        is String -> add(HighlightToken.Plain(content = element))
                                        is QuickJSObject -> {
                                            val json = element.stringify()
                                            add(
                                                format.decodeFromString<HighlightToken.Token>(
                                                    HighlightTokenSerializer, json
                                                )
                                            )
                                        }
                                        else -> error("Unknown type: ${element::class.java.name}")
                                    }
                                }
                            }
                        } finally {
                            result.release()
                        }
                        if (continuation.isActive) continuation.resume(tokens)
                    } catch (error: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            } catch (error: java.util.concurrent.RejectedExecutionException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    fun destroy() {
        if (!destroyed.compareAndSet(false, true)) return
        runCatching {
            executor.execute {
                if (contextDelegate.isInitialized()) {
                    context.destroy()
                }
            }
        }
        executor.shutdown()
    }
}

private val format by lazy {
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
}

sealed class HighlightToken {
    data class Plain(
        val content: String,
    ) : HighlightToken()

    @Serializable
    sealed class Token() : HighlightToken() {
        @Serializable
        data class StringContent(
            val content: String,
            val type: String,
            val length: Int,
        ) : Token()

        @Serializable
        data class StringListContent(
            val content: List<String>,
            val type: String,
            val length: Int,
        ) : Token()

        @Serializable
        data class Nested(
            val content: List<Token>,
            val type: String,
            val length: Int,
        ) : Token()
    }
}

object HighlightTokenSerializer : KSerializer<HighlightToken.Token> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HighlightToken.Token")

    override fun serialize(
        encoder: Encoder,
        value: HighlightToken.Token
    ) {
        // not used
    }

    override fun deserialize(decoder: Decoder): HighlightToken.Token {
        val jsonDecoder = decoder as JsonDecoder
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content
            ?: error("Missing type field in HighlightToken.Token")
        val length = jsonObject["length"]?.jsonPrimitive?.int
            ?: error("Missing length field in HighlightToken.Token")
        val content = jsonObject["content"]
            ?: error("Missing content field in HighlightToken.Token")

        return when (content) {
            is JsonArray -> {
                val nestedContent = arrayListOf<HighlightToken.Token>()

                content.forEach { part ->
                    if (part is JsonPrimitive) {
                        nestedContent += HighlightToken.Token.StringContent(
                            content = part.content,
                            type = type,
                            length = length,
                        )
                    } else if (part is JsonObject) {
                        nestedContent += format.decodeFromJsonElement(
                            HighlightTokenSerializer,
                            part
                        )
                    } else {
                        error("unknown content part type: $content / $part")
                    }
                }

                HighlightToken.Token.Nested(
                    content = nestedContent,
                    type = type,
                    length = length,
                )
            }

            is JsonPrimitive -> {
                val stringContent = content.content
                HighlightToken.Token.StringContent(
                    content = stringContent,
                    type = type,
                    length = length,
                )
            }

            else -> error("Unknown content type: ${content::class.java.name}")
        }
    }
}
