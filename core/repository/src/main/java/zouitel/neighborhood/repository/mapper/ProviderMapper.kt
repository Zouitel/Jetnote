package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Provider
import zouitel.neighborhood.domain.model.Provider as OutProvider

class ProviderMapper {
    fun toDomain(providers: List<Provider>) = providers.map { toDomain(it) }

    fun toDomain(provider: Provider) = OutProvider(
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

    fun fromDomain(provider: OutProvider) = Provider(
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