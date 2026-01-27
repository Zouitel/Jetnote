package zouitel.neighborhood.assistant.ai.transformers

import zouitel.neighborhood.ai.provider.Modality
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

private const val TAG = "OcrTransformer"

object OcrTransformer : InputMessageTransformer, KoinComponent {
//    private val cache by lazy {
//        val context = get<Context>()
//        val json = Json { allowStructuredMapKeys = true }
//        val store = SingleFileCacheStore(
//            file = File(context.cacheDir, "ocr_cache.json"),
//            keySerializer = String.serializer(),
//            valueSerializer = String.serializer(),
//            json = json
//        )
//        LruCache(
//            capacity = 64,
//            store = store,
//            deleteOnEvict = true,
//            preloadFromStore = true,
//            expireAfterWriteMillis = 3.days.inWholeMilliseconds,
//        )
//    }

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        if (ctx.model.inputModalities.contains(Modality.IMAGE)) {
            // 如果模型支持图片输入，直接返回原始消息
            return messages
        }

        // 如果模型不支持图片输入，进行OCR转换
        return withContext(Dispatchers.IO) {
            messages.map { message ->
                message.copy(
                    parts = message.parts.map { part ->
                        when {
                            part is UIMessagePart.Image && part.url.startsWith("file:") -> {
                                UIMessagePart.Text("performOcr(part)")
                            }

                            else -> part
                        }
                    }
                )
            }
        }
    }

//    suspend fun performOcr(part: UIMessagePart.Image): String = runCatching {
//        // Check cache first
//        cache.get(part.url)?.let { cachedResult ->
//            Log.i(TAG, "performOcr: Using cached result for ${part.url}")
//            return cachedResult
//        }
//
//        val settings = get<SettingsStore>().settingsFlow.value
//        val currentModel = settings.findModelById(settings.ocrModelId) ?: return "[Image]"
//        val providerSetting = currentModel.findProvider(settings.providers) ?: return "[Image]"
//        val provider = get<ProviderManager>().getProviderByType(providerSetting)
//        val result = provider.generateText(
//            providerSetting = providerSetting,
//            messages = listOf(
//                UIMessage.system(settings.ocrPrompt),
//                UIMessage(
//                    role = MessageRole.USER,
//                    parts = listOf(UIMessagePart.Image(part.url))
//                )
//            ),
//            params = TextGenerationParams(
//                currentModel = currentModel,
//            ),
//        )
//        val content = result.choices[0].message?.toText() ?: "[ERROR, OCR failed]"
//        Log.i(TAG, "performOcr: $content")
//        val ocrResult = """
//            <image_file_ocr>
//               $content
//            </image_file_ocr>
//            * The image_file_ocr tag contains a description of an image that the user uploaded to you, not the user's prompt.
//        """.trimIndent()
//
//        // Cache the result
//        cache.put(part.url, ocrResult)
//        return ocrResult
//    }.getOrElse {
//        "[ERROR, OCR failed: $it]"
//    }
}
