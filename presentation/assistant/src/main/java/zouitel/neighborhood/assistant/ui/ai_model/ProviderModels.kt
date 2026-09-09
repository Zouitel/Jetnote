package zouitel.neighborhood.assistant.ui.ai_model

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.provider.ProviderManager
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.provider.providers.GlobalProvider
import zouitel.neighborhood.ai.registry.ModelRegistry
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import zouitel.neighborhood.ai.provider.providers.ClaudeProvider
import zouitel.neighborhood.ai.provider.providers.GoogleProvider
import zouitel.neighborhood.assistant.utils.Avatar
import kotlin.uuid.Uuid

@Composable
internal fun ModelsList(
    provider: ProviderSetting,
    providerManager: ProviderManager,
    modelsModel: ModelsScreenModel
) {
    val providerInstance = providerManager.getProviderByType(provider)
    var isInitialLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMorePages by remember { mutableStateOf(true) }
    var loadedModels by remember { mutableStateOf<List<Model>>(emptyList()) }
    val localModels by remember(modelsModel, modelsModel::observeAll).collectAsState(emptyList())
    val scope = rememberCoroutineScope()

    // Load first page on composition
    LaunchedEffect(provider) {
        isInitialLoading = true
        currentPage = 0
        hasMorePages = true
        loadedModels = emptyList()

        runCatching {
            // Check if provider supports pagination
            when (providerInstance) {
                is GlobalProvider if provider is ProviderSetting.GlobalAI -> {
                    val page = providerInstance.listModelsPaginated(provider, 0)
                    loadedModels = page.models.sortedBy { it.modelId }
                    hasMorePages = page.hasMore
                    currentPage = page.nextPage
                }

                is GoogleProvider if provider is ProviderSetting.Google -> {
                    val page = providerInstance.listModelsPaginated(provider, 0)
                    loadedModels = page.models.sortedBy { it.modelId }
                    hasMorePages = page.hasMore
                    currentPage = page.nextPage
                }

                is ClaudeProvider if provider is ProviderSetting.Claude -> {
                    val page = providerInstance.listModelsPaginated(provider, 0)
                    loadedModels = page.models.sortedBy { it.modelId }
                    hasMorePages = page.hasMore
                    currentPage = page.nextPage
                }

                else -> {
                    // Fallback to old method for providers without pagination
                    loadedModels = providerInstance
                        .listModels(provider)
                        .sortedBy { it.modelId }
                    hasMorePages = false
                }
            }
        }.onSuccess {
            isInitialLoading = false
        }.onFailure {
            it.printStackTrace()
            isInitialLoading = false
        }
    }

    // Function to load more models
    val loadMoreModels: () -> Unit = {
        if (!isLoadingMore && hasMorePages) {
            scope.launch {
                isLoadingMore = true
                runCatching {
                    // Check if provider supports pagination
                    when (val providerInstance = providerManager.getProviderByType(provider)) {
                        is GlobalProvider if provider is ProviderSetting.GlobalAI -> {
                            val page = providerInstance.listModelsPaginated(provider, currentPage)
                            loadedModels = (loadedModels + page.models).sortedBy { it.modelId }
                            hasMorePages = page.hasMore
                            currentPage = page.nextPage
                        }

                        is GoogleProvider if provider is ProviderSetting.Google -> {
                            val page = providerInstance.listModelsPaginated(provider, currentPage)
                            loadedModels = (loadedModels + page.models).sortedBy { it.modelId }
                            hasMorePages = page.hasMore
                            currentPage = page.nextPage
                        }

                        is ClaudeProvider if provider is ProviderSetting.Claude -> {
                            val page = providerInstance.listModelsPaginated(provider, currentPage)
                            loadedModels = (loadedModels + page.models).sortedBy { it.modelId }
                            hasMorePages = page.hasMore
                            currentPage = page.nextPage
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                }.also {
                    isLoadingMore = false
                }
            }
        }
    }

    if (isInitialLoading) {
        Row(Modifier.fillMaxWidth().heightIn(100.dp), Arrangement.Center) {
            CircularWavyProgressIndicator(
                color = CommonColors.prime,
                trackColor = CommonColors.prime.copy(alpha = .5f)
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            AddModels(loadedModels, localModels) { model ->
                if (model.modelId in localModels.map { it.modelId }) {
                    modelsModel.delete(model.modelId)
                } else {
                    val inputs = ModelRegistry.MODEL_INPUT_MODALITIES.getData(model.modelId)
                    val outputs = ModelRegistry.MODEL_OUTPUT_MODALITIES.getData(model.modelId)
                    val abilities = ModelRegistry.MODEL_ABILITIES.getData(model.modelId)

                    val final = model.copy(
                        uid = Uuid.random().toString(),
                        providerId = provider.id,
                        inputModalities = inputs,
                        outputModalities = outputs,
                        abilities = abilities
                    )
                    modelsModel.upsert(final)
                }
            }

            // Load More Button
            if (hasMorePages || isLoadingMore) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CommonColors.prime
                        )
                    } else {
                        Button(
                            onClick = loadMoreModels,
                            border = BorderStroke(1.dp, CommonColors.prime),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = CommonColors.prime
                            )
                        ) {
                            Text(stringResource(zouitel.neighborhood.logic.R.string.load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddModels(
    loadedModels: List<Model>,
    localModels: List<Model>,
    onClicked: (Model) -> Unit
) {
    // Create a Set of existing model IDs for O(1) lookup instead of O(n)
    val existingModelIds by remember(localModels) {
        derivedStateOf {
            localModels
                .map { it.modelId }
                .toSet()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${stringResource(zouitel.neighborhood.logic.R.string.available_models)}: ${loadedModels.size}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            ContextualFlowColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                itemCount = loadedModels.size,
                maxLines = 1,
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center,
                itemHorizontalAlignment = Alignment.CenterHorizontally
            ) { index ->
                val model = loadedModels.getOrNull(index) ?: return@ContextualFlowColumn

                // O(1) lookup using Set instead of O(n) with any()
                val modelKey = model.modelId
                val isExisting = modelKey in existingModelIds

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(45.dp)
                            .clickable { onClicked(model) }
                            .clip(MaterialTheme.shapes.extraLarge)
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExisting) CommonColors.prime
                            else MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = if (isExisting) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(45.dp).padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                isLoading = false,
                                logo = model.logoUrl,
                                modelName = model.displayName
                            ) { }

                            Text(
                                text = model.modelId,
                                style = TextStyle(
                                    fontSize = 19.sp,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}