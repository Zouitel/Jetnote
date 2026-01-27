package zouitel.neighborhood.assistant.mapper

import zouitel.neighborhood.ai.provider.BalanceOption
import zouitel.neighborhood.ai.provider.HttpProxy
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.domain.model.Provider as OutProvider
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.provider.ProviderSetting.*
import zouitel.neighborhood.assistant.utils.JsonInstant

class ProviderMapper {
    fun fromDomain(providers: List<OutProvider>) = providers.map { fromDomain(it) }

    fun fromDomain(provider: OutProvider): ProviderSetting {
        val proxy = JsonInstant.decodeFromString<HttpProxy>(provider.proxy)
        val balanceOption = JsonInstant.decodeFromString<BalanceOption>(provider.balanceOption)

        return when (provider.type) {
            1.toShort() -> {
                Google(
                    id = provider.id,
                    logoUrl = provider.logoUrl,
                    name = provider.name,
                    apiKey = provider.apiKey,
                    apiLink = provider.apiLink,
                    proxy = proxy,
                    balanceOption = balanceOption,
                    builtIn = provider.builtIn,
                    description = provider.description,
                    baseUrl = provider.baseUrl,
                    vertexAI = provider.vertexAI,
                    privateKey = provider.privateKey,
                    serviceAccountEmail = provider.serviceAccountEmail,
                    location = provider.location,
                    projectId = provider.projectId
                )
            }
            2.toShort() -> {
                Claude(
                    id = provider.id,
                    logoUrl = provider.logoUrl,
                    name = provider.name,
                    apiKey = provider.apiKey,
                    apiLink = provider.apiLink,
                    proxy = proxy,
                    balanceOption = balanceOption,
                    builtIn = provider.builtIn,
                    description = provider.description,
                    baseUrl = provider.baseUrl
                )
            }
            3.toShort() -> {
                GlobalAI(
                    id = provider.id,
                    logoUrl = provider.logoUrl,
                    name = provider.name,
                    apiKey = provider.apiKey,
                    apiLink = provider.apiLink,
                    proxy = proxy,
                    balanceOption = balanceOption,
                    builtIn = provider.builtIn,
                    description = provider.description,
                    baseUrl = provider.baseUrl,
                    chatCompletionsPath = provider.chatCompletionsPath,
                    useResponseApi = provider.useResponseApi
                )
            }
            else -> throw Exception("make it toast.")
        }
    }

    fun toDomain(provider: Google): OutProvider {
        val proxy = JsonInstant.encodeToString<HttpProxy>(provider.proxy)
        val balanceOption = JsonInstant.encodeToString<BalanceOption>(provider.balanceOption)
        println(provider.apiKey)
        println(provider.vertexAI)
        return OutProvider(
            id = provider.id,
            type = 1,
            logoUrl = provider.logoUrl,
            name = provider.name,
            apiKey = provider.apiKey,
            apiLink = provider.apiLink,
            proxy = proxy,
            balanceOption = balanceOption,
            builtIn = provider.builtIn,
            description = provider.description,
            baseUrl = provider.baseUrl,
            vertexAI = provider.vertexAI,
            privateKey = provider.privateKey,
            serviceAccountEmail = provider.serviceAccountEmail,
            location = provider.location,
            projectId = provider.projectId,
            chatCompletionsPath = null,
            useResponseApi = false
        )

    }

    fun toDomain(provider: Claude): OutProvider {
        JsonInstant.encodeToString<List<Model>>(provider.models)
        val proxy = JsonInstant.encodeToString<HttpProxy>(provider.proxy)
        val balanceOption = JsonInstant.encodeToString<BalanceOption>(provider.balanceOption)
        return OutProvider(
            id = provider.id,
            type = 2,
            logoUrl = provider.logoUrl,
            name = provider.name,
            apiKey = provider.apiKey,
            apiLink = provider.apiLink,
            proxy = proxy,
            balanceOption = balanceOption,
            builtIn = provider.builtIn,
            description = provider.description,
            baseUrl = provider.baseUrl,
            vertexAI = false,
            privateKey = "",
            serviceAccountEmail = "",
            location = "",
            projectId = "",
            chatCompletionsPath = null,
            useResponseApi = false
        )
    }

    fun toDomain(provider: GlobalAI): OutProvider {
        val proxy = JsonInstant.encodeToString<HttpProxy>(provider.proxy)
        val balanceOption = JsonInstant.encodeToString<BalanceOption>(provider.balanceOption)
        return OutProvider(
            id = provider.id,
            type = 3,
            logoUrl = provider.logoUrl,
            name = provider.name,
            apiKey = provider.apiKey,
            apiLink = provider.apiLink,
            proxy = proxy,
            balanceOption = balanceOption,
            builtIn = provider.builtIn,
            description = provider.description,
            baseUrl = provider.baseUrl,
            vertexAI = false,
            privateKey = "",
            serviceAccountEmail = "",
            location = "",
            projectId = "",
            chatCompletionsPath = provider.chatCompletionsPath,
            useResponseApi = provider.useResponseApi
        )
    }
}