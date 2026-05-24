package zouitel.neighborhood.search.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.search.note_card.NoteCard
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.search.utils.SelectionTopBar
import zouitel.neighborhood.search.utils.prioritiesList
import zouitel.neighborhood.search.utils.typesList
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonIcons.CIRCLE
import zouitel.neighborhood.ui.CommonIcons.CIRCLE_18
import zouitel.neighborhood.ui.CommonIcons.CIRCLE_48
import zouitel.neighborhood.ui.CommonIcons.TINT_SLASH
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.workspace.model.Note
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel

class SearchScreen(val currentTag: SearchTypes.Tag? = null) : Screen {

    @Composable
    override fun Content() {
        Search(
            currentTag,
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }
}

@Composable
fun BrowseContent(
    searchModel: SearchScreenModel,
    tags: List<zouitel.neighborhood.tag.model.Tag>,
    allNotes: List<Note>,
    audioNotes: List<Note>,
    reminderNotes: List<Note>,
    mediaNotes: List<Note>,
    imageUris: List<String>,
    noteColors: List<Int?>,
    notesByPriority: List<Note>,
    links: List<zouitel.neighborhood.link.model.Link>,
) {
    val searchWithinRes = stringResource(R.string.search_within)
    val priorityRes     = stringResource(R.string.priority)
    val mediaRes        = stringResource(R.string.media)
    val colourRes       = stringResource(R.string.colour)

    LazyColumn(
        modifier = Modifier
            .animateContentSize()
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp), // FAB clearance only
    ) {

        // ── Types ──────────────────────────────────────────────────────────────
        item {
            SectionTitle(stringResource(R.string.types))
            ContextualFlowRow(
                itemCount = typesList.size,
                modifier  = Modifier.padding(horizontal = 10.dp),
            ) { index ->
                val type  = typesList[index]
                val label = when (index) {
                    0    -> stringResource(R.string.links)
                    1    -> stringResource(R.string.reminders)
                    2    -> stringResource(R.string.audios)
                    3    -> stringResource(R.string.medias)
                    4    -> stringResource(R.string.priorities)
                    else -> type.title
                }
                val available = type.isAvailable(
                    when (type.types) {
                        SearchTypes.Links         -> links
                        SearchTypes.Audios        -> audioNotes
                        SearchTypes.Reminders     -> reminderNotes
                        SearchTypes.Medias        -> mediaNotes
                        is SearchTypes.Properties -> notesByPriority
                        else                      -> emptyList()
                    }
                )
                FilterChip(
                    selected    = false,
                    enabled     = available,
                    onClick     = {
                        searchModel.updateSearchType(type.types)
                        searchModel.setFieldText(label)
                    },
                    label       = {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    },
                    leadingIcon = {
                        Icon(painterResource(type.icon), contentDescription = null)
                    },
                    shape    = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(horizontal = 3.dp),
                    colors   = FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor  = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Tags ───────────────────────────────────────────────────────────────
        if (tags.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.tags))
                ContextualFlowRow(
                    itemCount = tags.size,
                    modifier  = Modifier.padding(horizontal = 10.dp),
                ) { index ->
                    val tag          = tags[index]
                    val isUsedInNotes = allNotes.fastAny { note ->
                        note.tags.fastAny { it.label == tag.label }
                    }
                    FilterChip(
                        selected    = false,
                        enabled     = isUsedInNotes,
                        onClick     = {
                            searchModel.updateSearchType(SearchTypes.Tag(tag.label))
                            searchModel.setFieldText("$searchWithinRes \"${tag.label}\"")
                        },
                        label       = {
                            Text(tag.label, style = MaterialTheme.typography.labelLarge)
                        },
                        leadingIcon = {
                            tag.color?.let {
                                Icon(
                                    painterResource(CIRCLE_18),
                                    contentDescription = null,
                                    tint = Color(it),
                                )
                            }
                        },
                        shape    = MaterialTheme.shapes.large,
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Priorities ─────────────────────────────────────────────────────────
        if (notesByPriority.isNotEmpty()) {
            item {
                val priorityLabels = listOf(
                    stringResource(R.string.non_priority),
                    stringResource(R.string.low_priority),
                    stringResource(R.string.medium_priority),
                    stringResource(R.string.high_priority),
                    stringResource(R.string.urgent_priority),
                )
                SectionTitle(stringResource(R.string.priority))
                Row(
                    modifier              = Modifier.padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Absolute.Left,
                ) {
                    prioritiesList
                        .filter { it.isAvailable(allNotes) }
                        .forEach { property ->
                            Column {
                                IconButton(onClick = {
                                    searchModel.updateSearchType(
                                        SearchTypes.Properties(property.index)
                                    )
                                    searchModel.setFieldText(
                                        "$searchWithinRes $priorityRes " +
                                                "\"${priorityLabels[property.index]}\""
                                    )
                                }) {
                                    Icon(
                                        painterResource(CIRCLE),
                                        contentDescription = null,
                                        tint = Color(property.color),
                                    )
                                }
                                Text(
                                    text      = priorityLabels[property.index],
                                    fontStyle = MaterialTheme.typography.labelSmall.fontStyle,
                                    color     = Color(property.color).copy(alpha = .55f),
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                        }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Image thumbnails ───────────────────────────────────────────────────
        if (imageUris.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.medias))
                ContextualFlowRow(
                    itemCount             = imageUris.size,
                    modifier              = Modifier.padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) { index ->
                    val uri = imageUris[index]
                    AsyncImage(
                        model              = uri,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                searchModel.updateSearchType(SearchTypes.Image(uri))
                                searchModel.setFieldText("$searchWithinRes $mediaRes.")
                            },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Colours ────────────────────────────────────────────────────────────
        if (noteColors.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.colours))
                ContextualFlowRow(
                    itemCount = noteColors.size,
                    modifier  = Modifier.padding(horizontal = 10.dp),
                ) { index ->
                    val color = noteColors[index]
                    IconButton(onClick = {
                        searchModel.updateSearchType(SearchTypes.Color(color))
                        searchModel.setFieldText("$searchWithinRes $colourRes.")
                    }) {
                        if (color == MaterialTheme.colorScheme.surface.toArgb()) {
                            Icon(painterResource(TINT_SLASH), contentDescription = null)
                        } else {
                            Icon(
                                painterResource(CIRCLE_48),
                                contentDescription = null,
                                tint = color?.let { Color(it) }
                                    ?: MaterialTheme.colorScheme.surface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Search(
    currentTag: SearchTypes.Tag?,
    searchModel: SearchScreenModel,
    settingsModel: SettingsScreenModel,
    tagModel: TagScreenModel,
    audioModel: AudioScreenModel,
    linkModel: LinkScreenModel,
    dataModel: DataScreenModel,
    noteAndTagModel: NoteAndTagScreenModel,
) {
    val bottomNav = LocalBottomSheetNavigator.current

    LaunchedEffect(Unit) {
        searchModel.initializeUser(FirebaseAuth.getInstance().currentUser?.uid)
    }
    LaunchedEffect(currentTag) {
        currentTag?.let { tag ->
            searchModel.updateSearchType(SearchTypes.Tag(tag.label))
            searchModel.setFieldText("\"${tag.label}\"")
        }
    }

    val settings        by remember(settingsModel, settingsModel::observe).collectAsState()
    val tags            by remember(tagModel, tagModel::observeAll).collectAsState()
    val links           by remember(linkModel, linkModel::observeAll).collectAsState()
    val uiState         by remember(searchModel, searchModel::uiState).collectAsState()
    val allNotes        by remember(searchModel, searchModel::allNotes).collectAsState()
    val notesByPriority by remember(searchModel, searchModel::notesByPriority).collectAsState()

    val audioNotes    = remember(allNotes) { allNotes.filter { it.data.audios.isNotEmpty() } }
    val reminderNotes = remember(allNotes) { allNotes.filter { it.data.reminders.isNotEmpty() } }
    val mediaNotes    = remember(allNotes) { allNotes.filter { it.data.medias.isNotEmpty() } }
    val noteColors    = remember(allNotes) { allNotes.map { it.data.background }.distinct() }
    val imageUris     = remember(mediaNotes) {
        mediaNotes
            .flatMap { it.data.medias }
            .filter { it.type == MediaType.IMAGE }
            .map { it.uri }
            .distinct()
    }

    val queryFlow = remember(searchModel) {
        snapshotFlow { searchModel.fieldState.text.toString().trim() }
    }

    val isActiveSearch by remember(searchModel) {
        combine(searchModel.uiState, queryFlow) { state, query ->
            when (state.typeState) {
                is SearchTypes.Text -> query.isNotBlank()
                else                -> true
            }
        }
    }.collectAsState(initial = false)

    val filteredNotes by remember(searchModel, allNotes, notesByPriority, links) {
        combine(searchModel.uiState, queryFlow) { state, query ->
            when (val type = state.typeState) {
                is SearchTypes.Text -> {
                    if (query.isBlank()) emptyList()
                    else allNotes.filter { it.matchesQuery(query) }
                }
                is SearchTypes.Tag  -> allNotes.filter { note ->
                    note.tags.any { it.label.equals(type.label, ignoreCase = true) }
                }
                SearchTypes.Audios    -> audioNotes
                SearchTypes.Reminders -> reminderNotes
                SearchTypes.Medias    -> mediaNotes
                SearchTypes.Links     -> {
                    val linkedUids = links.map { it.uid }.toHashSet()
                    allNotes.filter { it.data.uid in linkedUids }
                }
                is SearchTypes.Image -> allNotes.filter { note ->
                    if (type.path.isBlank())
                        note.data.medias.any { it.type == MediaType.IMAGE }
                    else
                        note.data.medias.any { media ->
                            media.type == MediaType.IMAGE &&
                                    media.uri.contains(type.path, ignoreCase = true)
                        }
                }
                is SearchTypes.Color -> allNotes.filter { note ->
                    note.data.background == type.value
                }
                is SearchTypes.Properties -> when (type.value) {
                    0    -> notesByPriority
                    else -> allNotes.filter { it.data.priority == type.value }
                }
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    }.collectAsState(initial = emptyList())

    // ── Root layout ────────────────────────────────────────────────────────────
    //
    // ⚠️  NO Scaffold here. NO nestedScroll here.
    //
    // This composable renders inside ExpandedFullScreenContainedSearchBar which
    // is already inside MainScreen's Scaffold+nestedScroll.  Adding a second
    // Scaffold with its own enterAlwaysScrollBehavior created two nested-scroll
    // consumers competing for the same scroll events — the root cause of the
    // scroll jitter / stutter reported.
    //
    // SelectionTopBar is placed inline at the top of a Column instead of in a
    // Scaffold topBar slot, which is equivalent visually and avoids the conflict.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .reportable(settings.isShakable)
            .cloudy(enabled = bottomNav.isVisible),
    ) {

        // Inline top bar — only visible in multi-select mode.
        if (uiState.isSelection) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SelectionTopBar(
                    searchModel     = searchModel,
                    dataModel       = dataModel,
                    noteAndTagModel = noteAndTagModel,
                    tagModel        = tagModel,
                    onDelete        = {},
                )
            }
        }

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier         = Modifier.fillMaxSize(),
        ) {
            if (isActiveSearch) {
                if (settings.layout == CommonConstants.LIST) {
                    LazyColumn(
                        state               = rememberLazyListState(),
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(
                            start  = 8.dp,
                            end    = 8.dp,
                            top    = 8.dp,
                            bottom = 80.dp,   // FAB clearance
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredNotes, key = { it.data.uid }) { note ->
                            NoteCard(
                                settingsModel = settingsModel,
                                audioModel    = audioModel,
                                linkModel     = linkModel,
                                searchModel   = searchModel,
                                note          = note,
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        state                 = rememberLazyStaggeredGridState(),
                        modifier              = Modifier.fillMaxSize(),
                        columns               = StaggeredGridCells.Fixed(2),
                        contentPadding        = PaddingValues(
                            start  = 8.dp,
                            end    = 8.dp,
                            top    = 8.dp,
                            bottom = 80.dp,   // FAB clearance
                        ),
                        verticalItemSpacing   = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredNotes, key = { it.data.uid }) { note ->
                            NoteCard(
                                settingsModel = settingsModel,
                                audioModel    = audioModel,
                                linkModel     = linkModel,
                                searchModel   = searchModel,
                                note          = note,
                            )
                        }
                    }
                }
            } else {
                BrowseContent(
                    searchModel     = searchModel,
                    tags            = tags,
                    allNotes        = allNotes,
                    audioNotes      = audioNotes,
                    reminderNotes   = reminderNotes,
                    mediaNotes      = mediaNotes,
                    imageUris       = imageUris,
                    noteColors      = noteColors,
                    notesByPriority = notesByPriority,
                    links           = links,
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text       = text,
        fontSize   = MaterialTheme.typography.titleLarge.fontSize,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier.padding(horizontal = 10.dp),
    )
}

private fun Note.matchesQuery(query: String): Boolean {
    if (data.title.contains(query, ignoreCase = true)) return true
    return data.blocks.any { block ->
        (block.content as? BlockContent.Text)?.text?.contains(query, ignoreCase = true) == true
    }
}