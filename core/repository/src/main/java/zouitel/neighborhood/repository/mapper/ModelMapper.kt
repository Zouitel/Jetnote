package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Model
import zouitel.neighborhood.domain.model.Model as OutModel

class ModelMapper {
    fun toDomain(models: List<Model>) = models.map { toDomain(it) }

    fun toDomain(model: Model?) = OutModel(
        uid = model?.uid ?: "",
        modelId = model?.modelId ?: "",
        providerId = model?.providerId ?: 0,
        name = model?.name ?: "",
        logoUrl = model?.logoUrl ?: "",
        type = model?.type ?: 1,
        customHeaders = model?.customHeaders ?: "[]",
        customBodies = model?.customBodies ?: "[]",
        inputModalities = model?.inputModalities ?: "[]",
        outputModalities = model?.outputModalities ?: "[]",
        abilities = model?.abilities ?: "[]",
        tools = model?.tools ?: "[]",
        providerOverwrite = model?.providerOverwrite
    )

    fun fromDomain(model: OutModel) = Model(
        uid = model.uid,
        modelId = model.modelId,
        providerId = model.providerId,
        name = model.name,
        logoUrl = model.logoUrl,
        type = model.type,
        customHeaders = model.customHeaders,
        customBodies = model.customBodies,
        inputModalities = model.inputModalities,
        outputModalities = model.outputModalities,
        abilities = model.abilities,
        tools = model.tools,
        providerOverwrite = model.providerOverwrite
    )
}