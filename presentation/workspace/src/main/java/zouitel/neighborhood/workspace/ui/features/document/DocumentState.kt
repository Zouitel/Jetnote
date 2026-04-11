package zouitel.neighborhood.workspace.ui.features.document

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ── States ────────────────────────────────────────────────────────────────────
sealed class DocumentState {
    data object Idle    : DocumentState()
    data object Loading : DocumentState()
    data class  Ready(val pageCount: Int, val fileName: String) : DocumentState()
    data class  Error(val message: String) : DocumentState()
}

// ── ScreenModel ───────────────────────────────────────────────────────────────
class DocumentScreenModel : ScreenModel {

    private val _state = MutableStateFlow<DocumentState>(DocumentState.Idle)
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    // LRU-style page bitmap cache keyed by page index
    private val cache     = LinkedHashMap<Int, Bitmap>(16, 0.75f, true)
    private val cacheLock = Mutex()
    private val MAX_CACHE = 12

    // Active render jobs so we can cancel if the user scrolls past quickly
    private val renderJobs = mutableMapOf<Int, Job>()

    private var document      : Document? = null
    private var cachedFilePath: String?   = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Opens a document from a content URI.
     * Copies it to the app cache dir first (MuPDF needs a real file path).
     */
    fun openDocument(context: Context, uri: Uri, displayName: String) {
        screenModelScope.launch {
            _state.value = DocumentState.Loading
            runCatching {
                val localPath = copyUriToCache(context, uri, displayName)
                cachedFilePath = localPath
                val doc = withContext(Dispatchers.IO) { Document.openDocument(localPath) }
                document = doc
                _state.value = DocumentState.Ready(doc.countPages(), displayName)
            }.onFailure {
                _state.value = DocumentState.Error(it.message ?: "Failed to open document")
            }
        }
    }

    /**
     * Returns a rendered [Bitmap] for [pageIndex].
     * If a cached bitmap exists it is returned immediately; otherwise the page
     * is rendered at [scale]× on the IO dispatcher and cached.
     *
     * @param scale  Render density multiplier (2f = high DPI, good default).
     */
    suspend fun renderPage(pageIndex: Int, scale: Float = 2f): Bitmap? {
        cacheLock.withLock { cache[pageIndex] }?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val page   = document?.loadPage(pageIndex) ?: return@withContext null
                val matrix = Matrix(scale, scale)
                val bmp    = AndroidDrawDevice.drawPage(page, matrix)

                cacheLock.withLock {
                    // Evict oldest entry when cache is full
                    if (cache.size >= MAX_CACHE) {
                        val oldest = cache.keys.first()
                        cache.remove(oldest)?.recycle()
                    }
                    cache[pageIndex] = bmp
                }
                bmp
            }.getOrNull()
        }
    }

    /** Pre-renders surrounding pages so scrolling feels instant. */
    fun prefetchAround(pageIndex: Int, pageCount: Int) {
        val range = (pageIndex - 1).coerceAtLeast(0)..(pageIndex + 2).coerceAtMost(pageCount - 1)
        range.forEach { idx ->
            if (!renderJobs.containsKey(idx)) {
                renderJobs[idx] = screenModelScope.launch { renderPage(idx) }
            }
        }
    }

    /** Resolves a human-readable display name from a content URI. */
    fun resolveDisplayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } ?: uri.lastPathSegment
    }.getOrNull() ?: "document"

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDispose() {
        super.onDispose()
        renderJobs.values.forEach { it.cancel() }
        cache.values.forEach { it.recycle() }
        cache.clear()
        document?.destroy()
        document = null
        cachedFilePath?.let { File(it).delete() }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private suspend fun copyUriToCache(context: Context, uri: Uri, name: String): String =
        withContext(Dispatchers.IO) {
            val dest = File(context.cacheDir, "mupdf_${System.currentTimeMillis()}_$name")
            context.contentResolver.openInputStream(uri)?.use { src ->
                FileOutputStream(dest).use { dst -> src.copyTo(dst) }
            }
            dest.absolutePath
        }
}