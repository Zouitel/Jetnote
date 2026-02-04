package zouitel.neighborhood.ai.provider.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import zouitel.neighborhood.ai.provider.ImageGenerationParams
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.provider.Provider
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.provider.TextGenerationParams
import zouitel.neighborhood.ai.provider.providers.openai.ChatCompletionsAPI
import zouitel.neighborhood.ai.provider.providers.openai.ResponseAPI
import zouitel.neighborhood.logic.http.await
import zouitel.neighborhood.logic.http.getByKey
import zouitel.neighborhood.ai.ui.ImageAspectRatio
import zouitel.neighborhood.ai.ui.ImageGenerationItem
import zouitel.neighborhood.ai.ui.ImageGenerationResult
import zouitel.neighborhood.ai.ui.MessageChunk
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.utils.KeyRoulette
import zouitel.neighborhood.ai.utils.ModelLogoProvider
import zouitel.neighborhood.ai.utils.configureClientWithProxy
import zouitel.neighborhood.ai.utils.json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GlobalProvider(
    private val client: OkHttpClient
) : Provider<ProviderSetting.GlobalAI> {
    private val keyRoulette = KeyRoulette.default()
    private val chatCompletionsAPI = ChatCompletionsAPI(client = client, keyRoulette = keyRoulette)
    private val responseAPI = ResponseAPI(client = client)

    // Cache for all models
    private var allModelsCache: List<Model>? = null

    companion object {
        private const val PAGE_SIZE = 20
    }

    override suspend fun listModels(providerSetting: ProviderSetting.GlobalAI): List<Model> =
        withContext(Dispatchers.IO) {
            // If we have cached models, return them
            if (allModelsCache != null) {
                return@withContext allModelsCache!!
            }

            val key = keyRoulette.next(providerSetting.apiKey)
            val request = Request.Builder()
                .url("${providerSetting.baseUrl}/models")
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            val response =
                client.configureClientWithProxy(providerSetting.proxy).newCall(request).await()
            if (!response.isSuccessful) {
                error("Failed to get models: ${response.code} ${response.body?.string()}")
            }

            val bodyStr = response.body?.string() ?: ""
            val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
            val data = bodyJson["data"]?.jsonArray ?: return@withContext emptyList()

            val models = data.mapNotNull { modelJson ->
                val modelObj = modelJson.jsonObject
                val id = modelObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                Model(
                    modelId = id,
                    providerId = 30,
                    displayName = id,
                    logoUrl = ModelLogoProvider.getLogoUrl(id)
                )
            }

            // Cache the models
            allModelsCache = models
            models
        }

    /**
     * Load models with pagination
     * @param providerSetting The provider settings
     * @param page The page number (starts from 0)
     * @return ModelPage containing the models for this page and pagination info
     */
    suspend fun listModelsPaginated(
        providerSetting: ProviderSetting.GlobalAI,
        page: Int = 0
    ): ModelPage = withContext(Dispatchers.IO) {
        // Get all models (using cache if available)
        val allModels = listModels(providerSetting)

        val startIndex = page * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, allModels.size)

        // Check if we're beyond the available models
        if (startIndex >= allModels.size) {
            return@withContext ModelPage(
                models = emptyList(),
                hasMore = false,
                nextPage = page
            )
        }

        val pageModels = allModels.subList(startIndex, endIndex)
        val hasMore = endIndex < allModels.size

        ModelPage(
            models = pageModels,
            hasMore = hasMore,
            nextPage = if (hasMore) page + 1 else page
        )
    }

    /**
     * Clear the models cache (useful when you want to refresh the models list)
     */
    fun clearModelsCache() {
        allModelsCache = null
    }

    override suspend fun getBalance(providerSetting: ProviderSetting.GlobalAI): String = withContext(Dispatchers.IO) {
        val key = keyRoulette.next(providerSetting.apiKey)
        val url = if (providerSetting.balanceOption.apiPath.startsWith("http")) {
            providerSetting.balanceOption.apiPath
        } else {
            "${providerSetting.baseUrl}${providerSetting.balanceOption.apiPath}"
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()
        val response = client.configureClientWithProxy(providerSetting.proxy).newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to get balance: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body.string()
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val value = bodyJson.getByKey(providerSetting.balanceOption.resultPath)
        val digitalValue = value.toFloatOrNull()
        if(digitalValue != null) {
            "%.2f".format(digitalValue)
        } else {
            value
        }
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting.GlobalAI,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): Flow<MessageChunk> = if (providerSetting.useResponseApi!!) {
        responseAPI.streamText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    } else {
        chatCompletionsAPI.streamText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    }

    override suspend fun generateText(
        providerSetting: ProviderSetting.GlobalAI,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): MessageChunk = if (providerSetting.useResponseApi!!) {
        responseAPI.generateText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    } else {
        chatCompletionsAPI.generateText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    }

    override suspend fun generateImage(
        providerSetting: ProviderSetting,
        params: ImageGenerationParams
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        require(providerSetting is ProviderSetting.GlobalAI) {
            "Expected GlobalAI provider setting"
        }

        val key = keyRoulette.next(providerSetting.apiKey)

        val requestBody = json.encodeToString(
            buildJsonObject {
                put("model", params.model.modelId)
                put("prompt", params.prompt)
                put("n", params.numOfImages)
                put("response_format", "b64_json")
                put(
                    "size", when (params.aspectRatio) {
                        ImageAspectRatio.SQUARE -> "1024x1024"
                        ImageAspectRatio.LANDSCAPE -> "1536x1024"
                        ImageAspectRatio.PORTRAIT -> "1024x1536"
                    }
                )
            }
        )

        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/images/generations")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.configureClientWithProxy(providerSetting.proxy).newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to generate image: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val data = bodyJson["data"]?.jsonArray ?: error("No data in response")

        val items = data.map { imageJson ->
            val imageObj = imageJson.jsonObject
            val b64Json = imageObj["b64_json"]?.jsonPrimitive?.contentOrNull
                ?: error("No b64_json in response")

            ImageGenerationItem(
                data = b64Json,
                mimeType = "image/png"
            )
        }

        ImageGenerationResult(items = items)
    }
}