package zouitel.neighborhood.search.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.search.note_card.NoteCard
import zouitel.neighborhood.search.utils.SearchTopBar
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.search.utils.SelectionTopBar
import zouitel.neighborhood.search.utils.prioritiesList
import zouitel.neighborhood.search.utils.typesList
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonIcons.CIRCLE
import zouitel.neighborhood.ui.CommonIcons.CIRCLE_18
import zouitel.neighborhood.ui.CommonIcons.CIRCLE_48
import zouitel.neighborhood.ui.CommonIcons.TINT_SLASH
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.workspace.model.MediaType

class SearchScreen(val currentTag: SearchTypes.Tag? = null): Screen {

    @Composable
    override fun Content() {
        Search(
            searchModel = getScreenModel(),
            settingsModel = getScreenModel(),
            tagModel = getScreenModel(),
            audioModel = getScreenModel(),
            linkModel = getScreenModel()
        )
    }

    @Composable
    private fun Search(
        searchModel: SearchScreenModel,
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        audioModel: AudioScreenModel,
        linkModel: LinkScreenModel
    ) {
        val bottomNav = LocalBottomSheetNavigator.current
        val auth = FirebaseAuth.getInstance()

        LaunchedEffect(auth, currentTag) {
            searchModel.initializeUser(auth.currentUser?.uid.toString())
        }

        currentTag?.let {
            searchModel.updateSearchType(SearchTypes.Tag(it.label))
            searchModel.updateSearchTitle("Search within \"${it.label}\"")
        }

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val tags by remember(tagModel, tagModel::observeAll).collectAsState()
        val links by remember(linkModel, linkModel::observeAll).collectAsState()
        val notes by remember(searchModel, searchModel::notes).collectAsState()
        val medias = remember(notes) { notes.filter { it.data.medias.isNotEmpty() } }
        val tasks = remember(notes) { notes.filter { it.data.tasks.isNotEmpty() } }
        val audios = remember(notes) { notes.filter { it.data.audios.isNotEmpty() } }
        val reminders = remember(notes) { notes.filter { it.data.reminders.isNotEmpty() } }
        val searchWithinResource = stringResource(R.string.search_within)
        val propertyResource = stringResource(R.string.priority)
        val mediaResource = stringResource(R.string.media)
        val colourResource = stringResource(R.string.colour)

        val notesByPriority by remember(searchModel, searchModel::notesByPriority).collectAsState()
        val notesColors = remember(notes) {
            notes.map { it.data.background }.distinct()
        }
        val notesImages by remember(notes) {
            derivedStateOf {
                medias.map { it.data.medias }
                    .flatten()
                    .filterNot { it.type == MediaType.IMAGE }
                    .map { it.uri }
                    .distinct()
            }
        }
        val uiState by remember(searchModel, searchModel::uiState).collectAsState()
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val listLayoutState = rememberLazyListState()
        val gridLayoutState = rememberLazyStaggeredGridState()

        val filteredNotes by remember(notes) {
            derivedStateOf {
                when (uiState.typeState) {
                    SearchTypes.Audios -> audios

                    is SearchTypes.Color -> notes.filter {
                        it.data.background == (uiState.typeState as SearchTypes.Color).value
                    }
                    is SearchTypes.Links -> notes.filter { note ->
                        links.any { link -> link.uid == note.data.uid }
                    }

                    is SearchTypes.Image -> notes.filter { note ->
                        note.data.medias.any()
                    }

                    SearchTypes.Reminders -> reminders

                    is SearchTypes.Tag -> notes.filter { note ->
                        note.tags.any { tag -> tag.label == (uiState.typeState as SearchTypes.Tag).label }
                    }

                    SearchTypes.Tasks -> tasks

                    is SearchTypes.Text -> notes.filter {
                        it.data.title.contains(uiState.text, true) ||
                                it.data.description.contains(uiState.text, true)
                    }

                    SearchTypes.Medias -> medias

                    is SearchTypes.Properties -> notes.filter { note ->
                        note.data.priority == (uiState.typeState as SearchTypes.Properties).value
                    }
                }
            }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = bottomNav.isVisible),
            topBar = {
                if (uiState.isSelection) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SelectionTopBar(
                            searchModel = searchModel,
                            dataModel = getScreenModel(),
                            noteAndTagModel = getScreenModel(),
                            tagModel = tagModel
                        ) {}
                    }
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SearchTopBar(
                            searchModel = searchModel,
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.text.isNotEmpty() || uiState.typeState !is SearchTypes.Text) {
                    if (settings.ordination == CommonConstants.LIST) {
                        LazyColumn(
                            state = listLayoutState,
                            contentPadding = paddingValues,
                            modifier = Modifier
                                .animateContentSize()
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filteredNotes,
                                key = { it.data.uid }
                            ) { entity ->
                                NoteCard(
                                    settingsModel = settingsModel,
                                    audioModel = audioModel,
                                    linkModel = linkModel,
                                    searchModel = searchModel,
                                    note = entity
                                )
                            }
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            state = gridLayoutState,
                            contentPadding = paddingValues,
                            modifier = Modifier
                                .animateContentSize()
                                .fillMaxSize(),
                            columns = StaggeredGridCells.Fixed(2),
                            verticalItemSpacing = 6.dp,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filteredNotes,
                                key = { it.data.uid }
                            ) { entity ->
                                NoteCard(
                                    settingsModel = settingsModel,
                                    audioModel = audioModel,
                                    linkModel = linkModel,
                                    searchModel = searchModel,
                                    note = entity
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .animateContentSize()
                            .fillMaxSize()
                    ) {
                        item {
                            val labels = listOf(
                                stringResource(R.string.links),
                                stringResource(R.string.reminders),
                                stringResource(R.string.audios),
                                stringResource(R.string.tasks),
                                stringResource(R.string.medias),
                                stringResource(R.string.priorities)
                            )

                            Text(
                                text = stringResource(R.string.types),
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            ContextualFlowRow(
                                itemCount = typesList.size,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) { index ->
                                val type = typesList[index]
                                val typeTitles = listOf(
                                    stringResource(R.string.links),
                                    stringResource(R.string.reminders),
                                    stringResource(R.string.audios),
                                    stringResource(R.string.tasks),
                                    stringResource(R.string.medias),
                                    stringResource(R.string.priorities)
                                )
                                FilterChip(
                                    selected = false,
                                    enabled = type.isAvailable(
                                        when(type.types) {
                                            SearchTypes.Links -> links
                                            SearchTypes.Audios -> audios
                                            SearchTypes.Reminders -> reminders
                                            SearchTypes.Tasks -> tasks
                                            is SearchTypes.Medias -> medias
                                            is SearchTypes.Properties -> notesByPriority
                                            else -> emptyList()
                                        }
                                    ),
                                    onClick = {
                                            searchModel.updateSearchType(type.types)
                                            searchModel.updateSearchTitle(typeTitles[index])
                                    },
                                    label = {
                                        Text(
                                            text = labels[index],
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(type.icon),
                                            contentDescription = null
                                        )
                                    },
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.padding(horizontal = 3.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        iconColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                        if (tags.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.tags),
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                                ContextualFlowRow(
                                    itemCount = tags.size,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) { index ->
                                    val tag = tags[index]

                                    if(notes.fastAny { note -> note.tags.fastAny { it.label == tag.label } })
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            searchModel.updateSearchType(SearchTypes.Tag(tag.label))
                                            searchModel.updateSearchTitle("$searchWithinResource \"${tag.label}\"")
                                        },
                                        label = {
                                            Text(
                                                text = tag.label,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        leadingIcon = {
                                            tag.color?.let {
                                                Icon(
                                                    painter = painterResource(CIRCLE_18),
                                                    contentDescription = null,
                                                    tint = Color(it)
                                                )
                                            }
                                        },
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                        if (notes.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.priority),
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Absolute.Left
                                ) {
                                    val priorities = listOf(
                                        stringResource(R.string.non_priority),
                                        stringResource(R.string.low_priority),
                                        stringResource(R.string.medium_priority),
                                        stringResource(R.string.high_priority),
                                        stringResource(R.string.urgent_priority)
                                    )
                                    prioritiesList
                                        .filter { it.isAvailable(notes) }
                                        .forEachIndexed { index, property ->
                                            Column {
                                                IconButton({
                                                    searchModel.updateSearchType(SearchTypes.Properties(property.index))
                                                    searchModel.updateSearchTitle("$searchWithinResource $propertyResource \"${priorities[property.index]}\"")
                                                }) {
                                                    Icon(
                                                        painter = painterResource(CIRCLE),
                                                        contentDescription = null,
                                                        tint = Color(property.color)
                                                    )
                                                }
                                                Text(
                                                    text = priorities[property.index],
                                                    fontStyle = MaterialTheme.typography.labelSmall.fontStyle,
                                                    color = Color(property.color).copy(alpha = .5f)
                                                )
                                            }
                                            Spacer(Modifier.width(5.dp))
                                        }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                        if (notesImages.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.medias),
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                                ContextualFlowRow(
                                    itemCount = notesImages.size,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) { index ->
                                    val uri = notesImages[index]
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                searchModel.updateSearchType(SearchTypes.Image(uri))
                                                searchModel.updateSearchTitle("$searchWithinResource $mediaResource.")
                                            }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                        if (notes.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.colours),
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                                ContextualFlowRow(
                                    itemCount = notesColors.size,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) { index ->
                                    val color = notesColors[index]
                                    IconButton({
                                        searchModel.updateSearchType(SearchTypes.Color(color))
                                        searchModel.updateSearchTitle("$searchWithinResource $colourResource.")
                                    }) {
                                        if (color == MaterialTheme.colorScheme.surface.toArgb()) {
                                            Icon(
                                                painter = painterResource(id = TINT_SLASH),
                                                contentDescription = null,
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = CIRCLE_48),
                                                contentDescription = null,
                                                tint = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                                            )
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