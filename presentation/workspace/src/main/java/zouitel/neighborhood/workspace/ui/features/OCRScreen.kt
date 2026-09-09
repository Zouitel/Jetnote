package zouitel.neighborhood.workspace.ui.features

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.MediaType

/**
 * Full-page scan screen for Jetnote.
 *
 * Drop this into your NavHost like:
 *
 *   composable("scan?noteId={noteId}") { backStack ->
 *       val noteId = backStack. arguments?.getString("noteId")?.toLongOrNull() ?: -1L
 *       OCRScreen(
 *           noteId = noteId,
 *           onNavigateBack = { navController.popBackStack() }
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
class OCRScreen(val noteId: String = "", val data: MutableState<Data>? = null) : Screen {

    @Composable
    override fun Content() {
        OCR(koinScreenModel())
    }

    @Composable
    private fun OCR(OCRModel: OCRScreenModel) {
        val uiState  by OCRModel.uiState.collectAsStateWithLifecycle()
        val context  = LocalContext.current
        val activity = context as? Activity
        val navigator = LocalNavigator.currentOrThrow

        // ── ML Kit launcher ───────────────────────────────────────────────────────
        val scanLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                if (scanResult != null) OCRModel.onScanResult(scanResult)
            } else if (result.resultCode != Activity.RESULT_CANCELED) {
                OCRModel.onScanError("Scan cancelled or failed.")
            }
        }

        // ── Auto-launch scanner on first composition ───────────────────────────
        var launchTriggered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!launchTriggered && activity != null) {
                launchTriggered = true
                OCRModel.getScannerClient()
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        OCRModel.onScanError(e.localizedMessage ?: "Could not start scanner")
                    }
            }
        }

        // ── Error toast ───────────────────────────────────────────────────────────
        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                OCRModel.clearError()
            }
        }

        // ── Write saved PDF into data.medias, then navigate back ─────────────
        LaunchedEffect(uiState.savedPdfPath) {
            val path = uiState.savedPdfPath ?: return@LaunchedEffect
            if (path.isNotBlank()) {
                data?.value = data.value.copy(
                    medias = data.value.medias.plusElement(
                        Media(
                            uri   = path,
                            type  = MediaType.DOCUMENT,
                            color = Color.Transparent.toArgb()
                        )
                    )
                )
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Document Scanner") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(CommonIcons.DOCUMENT_36),
                                contentDescription = "Back"
                            )
                        }
                    },
                )
            }
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {

                // ── Empty state ───────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.scannedPages.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(CommonIcons.DOCUMENT_36),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Waiting for scanner…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                activity?.let { act ->
                                    OCRModel.getScannerClient()
                                        .getStartScanIntent(act)
                                        .addOnSuccessListener { intentSender ->
                                            scanLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            OCRModel.onScanError(e.localizedMessage ?: "Error")
                                        }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(CommonIcons.DOCUMENT_36),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Open Scanner")
                        }
                    }
                }

                // ── Result UI ─────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.scannedPages.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 4 },
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        // Page count summary
                        ScanSummaryCard(
                            pageCount = uiState.pageCount,
                            hasPdf = uiState.pdfUri != null
                        )

                        // Scanned page thumbnails
                        Text(
                            text = "Scanned Pages",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(uiState.scannedPages) { index, uri ->
                                ScannedPageThumbnail(uri = uri.toString(), pageNumber = index + 1)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Action buttons
                        ScanActionButtons(
                            isSaving = uiState.isSaving,
                            onSaveToNote = { OCRModel.saveToNote(noteId = noteId) },
                            onRescan = {
                                OCRModel.resetScan()
                                activity?.let { act ->
                                    OCRModel.getScannerClient()
                                        .getStartScanIntent(act)
                                        .addOnSuccessListener { intentSender ->
                                            scanLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            OCRModel.onScanError(e.localizedMessage ?: "Error")
                                        }
                                }
                            },
                            onDiscard = {
                                OCRModel.resetScan()
                            }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

// ─── Sub-components ──────────────────────────────────────────────────────────
    @Composable
    private fun ScanSummaryCard(pageCount: Int, hasPdf: Boolean) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(CommonIcons.CHECK),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan Complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$pageCount ${if (pageCount == 1) "page" else "pages"} scanned" +
                                if (hasPdf) " · PDF ready" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (hasPdf) {
                    Icon(
                        painter = painterResource(CommonIcons.INFO),
                        contentDescription = "PDF available",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun ScannedPageThumbnail(uri: String, pageNumber: Int) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Page $pageNumber",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "p.$pageNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }

    @Composable
    private fun ScanActionButtons(
        isSaving: Boolean,
        onSaveToNote: () -> Unit,
        onRescan: () -> Unit,
        onDiscard: () -> Unit,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSaveToNote,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Saving…")
                } else {
                    Text("Save to Note")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan Again")
                }
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}