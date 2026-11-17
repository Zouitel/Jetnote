package zouitel.neighborhood.navigator.templates

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockId
import zouitel.neighborhood.workspace.mapper.BlocksJson
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.workplace.TemplateLibraryScreenModel
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreen

class TemplateGalleryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val libraryModel = koinScreenModel<TemplateLibraryScreenModel>()
        val savedTemplates by libraryModel.savedTemplates.collectAsState()
        var query by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("All") }

        LaunchedEffect(libraryModel) {
            libraryModel.observe()
        }

        val savedById = savedTemplates.associateBy { it.templateId }
        val builtInTemplates = NoteTemplateCatalog.templates.map { template ->
            template.copy(isFavorite = savedById[template.id]?.isFavorite == true)
        }
        val personalTemplates = savedTemplates.filter { it.isCustom }.map { saved ->
            NoteTemplate(
                id = saved.templateId,
                title = saved.title,
                description = saved.description,
                category = saved.category,
                preview = "Personal template",
                blocks = {
                    runCatching {
                        BlocksJson.decodeFromString<List<Block>>(saved.blocksJson)
                            .map { it.copy(id = BlockId.generate()) }
                    }.getOrElse { listOf(Block.paragraph()) }
                },
                isFavorite = saved.isFavorite,
                isCustom = true
            )
        }
        val allTemplates = builtInTemplates + personalTemplates
        val categories = (
            listOf("All", "Favorites", "My templates") +
                NoteTemplateCatalog.categories.drop(1) +
                personalTemplates.map { it.category }
            ).distinct()

        val visibleTemplates = remember(query, category, savedTemplates) {
            allTemplates.filter { template ->
                val categoryMatches = when (category) {
                    "All" -> true
                    "Favorites" -> template.isFavorite
                    "My templates" -> template.isCustom
                    else -> template.category == category
                }
                categoryMatches && (
                    query.isBlank() ||
                        template.title.contains(query, ignoreCase = true) ||
                        template.description.contains(query, ignoreCase = true) ||
                        template.category.contains(query, ignoreCase = true)
                    )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Note templates") },
                    navigationIcon = {
                        TextButton(onClick = { navigator.pop() }) {
                            Text("Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { navigator.push(WorkplaceScreen(Data(), true)) }) {
                            Text("Blank note")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    "Start with a structure that fits what you're writing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search templates") }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (visibleTemplates.isEmpty()) {
                    Text(
                        "No templates match your search.",
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleTemplates, key = { it.id }) { template ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    navigator.push(WorkplaceScreen(template.toNoteData(), true))
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            template.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            if (template.isCustom) "Yours" else template.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        template.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        template.preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                libraryModel.setFavorite(
                                                    templateId = template.id,
                                                    title = template.title,
                                                    description = template.description,
                                                    category = template.category,
                                                    blocks = template.blocks(),
                                                    isCustom = template.isCustom,
                                                    favorite = !template.isFavorite
                                                )
                                            }
                                        ) {
                                            Text(if (template.isFavorite) "Unfavorite" else "Favorite")
                                        }
                                        if (template.isCustom) {
                                            TextButton(
                                                onClick = { libraryModel.deleteTemplate(template.id) }
                                            ) {
                                                Text("Delete")
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                navigator.push(WorkplaceScreen(template.toNoteData(), true))
                                            }
                                        ) {
                                            Text("Use template")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
