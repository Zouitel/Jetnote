package zouitel.neighborhood.repository.model

data class Model(
    val uid: String,
    val modelId: String,
    val providerId: Long,
    val name: String,
    val logoUrl: String?,
    val type: Short,
    val customHeaders: String,
    val customBodies: String,
    val inputModalities: String,
    val outputModalities: String,
    val abilities: String,
    val tools: String,
    val providerOverwrite: String?
)