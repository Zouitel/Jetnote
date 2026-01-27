package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Model
import zouitel.neighborhood.repository.model.Model as OutModel

class ModelMapper {
    fun toRepo(models: List<Model>) = models.map { toRepo(it) }

    fun toRepo(model: Model?) = OutModel(
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

    fun fromRepo(model: OutModel) = Model(
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