package zouitel.neighborhood.repository.model

data class Provider(
    val id: Long,
    val type: Short,
    val logoUrl: String?,
    val name: String,
    val apiKey: String,
    val apiLink: String,
    val proxy: String,
    val balanceOption: String,
    val builtIn: Boolean,
    val description: String,
    val baseUrl: String,
    val vertexAI: Boolean,
    val privateKey: String,
    val serviceAccountEmail: String,
    val location: String,
    val projectId: String,
    val chatCompletionsPath: String?,
    val useResponseApi: Boolean
)
