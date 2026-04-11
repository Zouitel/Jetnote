package zouitel.neighborhood.workspace.ui.features.document

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import zouitel.neighborhood.ui.CommonIcons

/**
 * Voyager screen that renders a PDF / DOCX file.
 *
 * Architecture (inspired by ONLYOFFICE Documents for Android):
 *   • [DocumentViewerScreenModel]  — opens the file, renders pages off-thread, caches bitmaps
 *   • [DocumentPageView]           — custom View per page; zoom via native ScaleGestureDetector
 *   • [DocumentPagesAdapter]       — RecyclerView adapter; scroll is 100% in the RecyclerView
 *   • [DocumentScreen]       — Compose shell that embeds the RecyclerView via AndroidView
 */
data class DocumentScreen(
    val uri         : Uri,
    val displayName : String
) : Screen {

    @Composable
    override fun Content() {
        DocumentViewer(model = getScreenModel())
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DocumentViewer(model: DocumentScreenModel) {
        val context   = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val state     by model.state.collectAsState()
        val scope     = rememberCoroutineScope()

        // Live page number updated by RecyclerView scroll listener
        var currentPage by remember { mutableIntStateOf(1) }

        LaunchedEffect(uri) {
            model.openDocument(context, uri, displayName)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val pageCount = (state as? DocumentState.Ready)?.pageCount
                        Text(
                            text = if (pageCount != null)
                                       "$displayName  ·  $currentPage / $pageCount"
                                   else
                                       displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style    = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                painter = painterResource(CommonIcons.ARROW_LEFT),
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {
                    is DocumentState.Idle,
                    is DocumentState.Loading -> CircularProgressIndicator()

                    is DocumentState.Error -> Text(
                        text     = "Error: ${s.message}",
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )

                    is DocumentState.Ready -> {
                        // ── RecyclerView via AndroidView ───────────────────────
                        // Using AndroidView gives us the real RecyclerView with
                        // its battle-tested fling physics and nested scroll support.
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory  = { ctx ->
                                RecyclerView(ctx).apply {
                                    layoutManager = LinearLayoutManager(ctx)

                                    // Thin horizontal divider between pages
                                    addItemDecoration(
                                        DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
                                    )

                                    adapter = DocumentPagesAdapter(
                                        pageCount = s.pageCount,
                                        model     = model,
                                        scope     = scope
                                    )

                                    // Update the top-bar page counter while scrolling
                                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                                            val lm  = rv.layoutManager as LinearLayoutManager
                                            currentPage = lm.findFirstVisibleItemPosition() + 1
                                        }
                                    })
                                }
                            },
                            // update block is called on recomposition; nothing to update
                            update = {}
                        )
                    }
                }
            }
        }
    }
}