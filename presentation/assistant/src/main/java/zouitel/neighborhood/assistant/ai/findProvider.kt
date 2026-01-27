package zouitel.neighborhood.assistant.ai

import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.provider.ProviderSetting

fun Model.findProvider(providers: List<ProviderSetting>, checkOverwrite: Boolean = true): ProviderSetting? {
    val provider = findModelProviderFromList(providers) ?: return null
    val providerOverwrite = this.providerOverwrite
    if (checkOverwrite && providerOverwrite != null) {
        return providerOverwrite.copyProvider(proxy = provider.proxy, models = emptyList())
    }
    return provider
}

//private fun Model.findModelProviderFromList(providers: List<ProviderSetting>): ProviderSetting? {
//    providers.forEach { setting ->
//        setting.models.forEach { model ->
//            if (model.uid == this.uid) {
//                return setting
//            }
//        }
//    }
//    return null
//}
private fun Model.findModelProviderFromList(providers: List<ProviderSetting>): ProviderSetting? {
    providers.forEach { prov ->
        if (prov.id == this.providerId) {
            return prov
        }
    }
    return null
}