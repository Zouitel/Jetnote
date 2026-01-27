package zouitel.neighborhood.assistant.mapper

import zouitel.neighborhood.ai.provider.BuiltInTools
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.provider.Modality
import zouitel.neighborhood.ai.provider.ModelAbility
import zouitel.neighborhood.ai.provider.ModelType
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.assistant.utils.JsonInstant
import zouitel.neighborhood.ai.provider.Model as ProviderModel
import zouitel.neighborhood.domain.model.Model as OutModel

class ModelMapper {
    fun fromDomain(models: List<OutModel>) = models.map { fromDomain(it) }

    fun fromDomain(model: OutModel?): ProviderModel {
        val type = when (model?.type) {
            1.toShort() -> ModelType.CHAT
            2.toShort() -> ModelType.IMAGE
            3.toShort() -> ModelType.EMBEDDING
            else -> error("Unknown currentModel type: ${model?.type}")
        }
        val customHeaders = JsonInstant.decodeFromString<List<CustomHeader>>(model.customHeaders)
        val customBodies = JsonInstant.decodeFromString<List<CustomBody>>(model.customBodies)
        val inputModalities = JsonInstant.decodeFromString<List<Modality>>(model.inputModalities)
        val outputModalities = JsonInstant.decodeFromString<List<Modality>>(model.outputModalities)
        val abilities = JsonInstant.decodeFromString<List<ModelAbility>>(model.abilities)
        val tools = JsonInstant.decodeFromString<Set<BuiltInTools>>(model.tools)
        val providerOverwrite =
            model.providerOverwrite?.let { JsonInstant.decodeFromString<ProviderSetting?>(it) }
        return ProviderModel(
            uid = model.uid,
            modelId = model.modelId,
            providerId = model.providerId,
            displayName = model.name,
            logoUrl = model.logoUrl,
            type = type,
            customHeaders = customHeaders,
            customBodies = customBodies,
            inputModalities = inputModalities,
            outputModalities = outputModalities,
            abilities = abilities,
            tools = tools,
            providerOverwrite = providerOverwrite
        )
    }

    fun toDomain(model: ProviderModel): OutModel {
        val type = when (model.type) {
            ModelType.CHAT -> 1.toShort()
            ModelType.IMAGE -> 2.toShort()
            ModelType.EMBEDDING -> 3.toShort()
        }
        val customHeaders = JsonInstant.encodeToString<List<CustomHeader>>(model.customHeaders)
        val customBodies = JsonInstant.encodeToString<List<CustomBody>>(model.customBodies)
        val inputModalities = JsonInstant.encodeToString<List<Modality>>(model.inputModalities)
        val outputModalities = JsonInstant.encodeToString<List<Modality>>(model.outputModalities)
        val abilities = JsonInstant.encodeToString<List<ModelAbility>>(model.abilities)
        val tools = JsonInstant.encodeToString<Set<BuiltInTools>>(model.tools)
        val providerOverwrite =
            model.providerOverwrite?.let { JsonInstant.encodeToString<ProviderSetting?>(it) }
        return OutModel(
            uid = model.uid.toString(),
            modelId = model.modelId,
            providerId = model.providerId,
            name = model.displayName,
            logoUrl = model.logoUrl,
            type = type,
            customHeaders = customHeaders,
            customBodies = customBodies,
            inputModalities = inputModalities,
            outputModalities = outputModalities,
            abilities = abilities,
            tools = tools,
            providerOverwrite = providerOverwrite
        )
    }
}