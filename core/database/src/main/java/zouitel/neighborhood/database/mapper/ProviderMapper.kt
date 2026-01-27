package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Provider
import zouitel.neighborhood.repository.model.Provider as OutProvider

class ProviderMapper {
    fun toRepo(providers: List<Provider>) = providers.map { toRepo(it) }

    fun toRepo(provider: Provider) = OutProvider(
        id = provider.id,
        type = provider.type,
        logoUrl = provider.logoUrl,
        name = provider.name,
        apiKey = provider.apiKey,
        apiLink = provider.apiLink,
        proxy = provider.proxy,
        balanceOption = provider.balanceOption,
        builtIn = provider.builtIn,
        description = provider.description,
        baseUrl = provider.baseUrl,
        vertexAI = provider.vertexAI,
        privateKey = provider.privateKey,
        serviceAccountEmail = provider.serviceAccountEmail,
        location = provider.location,
        projectId = provider.projectId,
        chatCompletionsPath = provider.chatCompletionsPath,
        useResponseApi = provider.useResponseApi
    )

    fun fromRepo(provider: OutProvider) = Provider(
        id = provider.id,
        type = provider.type,
        logoUrl = provider.logoUrl,
        name = provider.name,
        apiKey = provider.apiKey,
        apiLink = provider.apiLink,
        proxy = provider.proxy,
        balanceOption = provider.balanceOption,
        builtIn = provider.builtIn,
        description = provider.description,
        baseUrl = provider.baseUrl,
        vertexAI = provider.vertexAI,
        privateKey = provider.privateKey,
        serviceAccountEmail = provider.serviceAccountEmail,
        location = provider.location,
        projectId = provider.projectId,
        chatCompletionsPath = provider.chatCompletionsPath,
        useResponseApi = provider.useResponseApi
    )
}