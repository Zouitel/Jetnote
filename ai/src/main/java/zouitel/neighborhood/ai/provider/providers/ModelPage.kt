package zouitel.neighborhood.ai.provider.providers

import zouitel.neighborhood.ai.provider.Model

data class ModelPage(
    val models: List<Model>,
    val hasMore: Boolean,
    val nextPage: Int
)