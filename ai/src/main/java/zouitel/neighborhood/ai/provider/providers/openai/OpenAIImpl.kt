package zouitel.neighborhood.ai.provider.providers.openai

import kotlinx.coroutines.flow.Flow
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.provider.TextGenerationParams
import zouitel.neighborhood.ai.ui.MessageChunk
import zouitel.neighborhood.ai.ui.UIMessage

interface OpenAIImpl {
    suspend fun generateText(
        providerSetting: ProviderSetting.GlobalAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk

    suspend fun streamText(
        providerSetting: ProviderSetting.GlobalAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk>
}
