package zouitel.neighborhood.markdown.ui.renderers

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import zouitel.neighborhood.markdown.action.CloseSlashCommand
import zouitel.neighborhood.markdown.action.UpdateSlashCommandSession
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.markdown.registry.BlockCallbacks
import zouitel.neighborhood.markdown.action.ConvertBlockType
import zouitel.neighborhood.markdown.action.UpdateBlockContent
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.richtext.SpanMapper
import zouitel.neighborhood.markdown.richtext.SpanMaintenanceTextObserver
import zouitel.neighborhood.markdown.slash.SlashCommandTextObserver
import zouitel.neighborhood.markdown.ui.observers.ListAutoDetectObserver
import zouitel.neighborhood.markdown.ui.BackspaceAwareTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import zouitel.neighborhood.markdown.ui.LocalBlockSpanStates
import zouitel.neighborhood.markdown.ui.LocalBlockTextStates
import zouitel.neighborhood.markdown.ui.LocalEditorStateHolder
import zouitel.neighborhood.markdown.ui.LocalFormattingActions
import zouitel.neighborhood.markdown.ui.LocalSlashCaretRect
import zouitel.neighborhood.markdown.ui.LocalSlashCommandExecutor
import zouitel.neighborhood.markdown.ui.LocalSlashHighlightedCommandId
import zouitel.neighborhood.markdown.ui.LocalSlashPopupItems
import zouitel.neighborhood.markdown.ui.LocalSlashSessionAnchorBlockId
import zouitel.neighborhood.markdown.ui.visibleSelection
import zouitel.neighborhood.markdown.ui.visibleText
import zouitel.neighborhood.markdown.state.TextEditHistoryTracker
import zouitel.neighborhood.markdown.state.captureCheckpoint
import zouitel.neighborhood.markdown.state.captureFocusedEditingUiState
import zouitel.neighborhood.markdown.theme.LocalCascadeTheme

/**
 * Shared text editing composable used by renderers that need formattable text input.
 *
 * Encapsulates all text editing infrastructure: [TextFieldState] lookup, span state,
 * [OutputTransformation], focus management, [BackspaceAwareTextField], and the
 * unfocused tap overlay for cursor placement.
 *
 * **Focus contract:** this composable does NOT call `clearFocus()` when [isFocused]
 * becomes false. The hosting composable (currently [CascadeEditor][zouitel.neighborhood.markdown.ui.CascadeEditor])
 * is responsible for clearing Compose focus when `focusedBlockId` becomes null.
 * This avoids keyboard blink during block-to-block focus transitions.
 *
 * @param block The block being rendered
 * @param isFocused Whether this block currently has editor focus
 * @param textStyle The text style to apply
 * @param modifier Modifier for the root container
 * @param callbacks Block interaction callbacks
 */
@Composable
internal fun TextBlockField(
    block: Block,
    isFocused: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    callbacks: BlockCallbacks,
) {
    val textContent = block.content as? BlockContent.Text ?: return
    val colors = LocalCascadeTheme.current.colors
    val focusRequester = remember { FocusRequester() }
    var hasComposeFocus by remember { mutableStateOf(false) }
    val stateHolder = LocalEditorStateHolder.current

    // Get TextFieldState from the shared holder.
    // generation key invalidates the cache when loadFromJson clears all states.
    val textStates = LocalBlockTextStates.current
    val textFieldState = remember(block.id, textStates.generation) {
        textStates.getOrCreate(block.id, textContent.text)
    }
    val formattingActions = LocalFormattingActions.current
    val slashCommandExecutor = LocalSlashCommandExecutor.current
    val slashSessionAnchorBlockId = LocalSlashSessionAnchorBlockId.current
    val slashHighlightedCommandId = LocalSlashHighlightedCommandId.current
    val slashCaretRectHolder = LocalSlashCaretRect.current
    val slashPopupItems = LocalSlashPopupItems.current

    // Get span state from the shared holder — same generation-key rationale.
    val spanStates = LocalBlockSpanStates.current
    val spanState = remember(block.id, spanStates.generation) {
        spanStates.getOrCreate(block.id, textContent.spans, textContent.text.length)
    }
    val inlineCodeBackground = colors.inlineCodeBackground
    val highlightBackground = colors.highlight
    val baseDecoration = textStyle.textDecoration
    val outputTransformation = remember(block.id, spanState, inlineCodeBackground, highlightBackground, baseDecoration) {
        OutputTransformation {
            SpanMapper.run { applyStyles(spanState.value, inlineCodeBackground, highlightBackground, baseDecoration) }
        }
    }
    val spanTextObserver = remember(block.id, textStates, spanStates) {
        SpanMaintenanceTextObserver(
            blockId = block.id,
            textStates = textStates,
            spanStates = spanStates,
            initialVisibleText = textFieldState.visibleText(),
        )
    }
    val slashTextObserver = remember(block.id, callbacks, textStates.generation) {
        SlashCommandTextObserver(
            blockId = block.id,
            onOpen = { id, range, query -> callbacks.onSlashCommand(id, range, query) },
            onUpdate = { query, range -> callbacks.dispatch(UpdateSlashCommandSession(query, range)) },
            onClose = { callbacks.dispatch(CloseSlashCommand) },
            initialVisibleText = textFieldState.visibleText(),
        )
    }
    val textHistoryTracker = remember(block.id, stateHolder, textStates.generation, spanStates.generation) {
        // Generation only changes on hard runtime resets (load/replay clear paths).
        // By the time this tracker is recreated, the holder has already restored
        // the authoritative runtime state for the current composition pass.
        TextEditHistoryTracker(
            initialCheckpoint = stateHolder.captureCheckpoint(textStates, spanStates),
        )
    }
    DisposableEffect(block.id, stateHolder, textHistoryTracker) {
        // Toolbar-driven formatting bypasses this composable's snapshotFlow, so
        // the holder needs a back-reference to the live tracker for this block.
        stateHolder.registerTextHistoryTracker(block.id, textHistoryTracker)
        onDispose {
            stateHolder.unregisterTextHistoryTracker(block.id, textHistoryTracker)
        }
    }
    val isCurrentlyList = block.type is BlockType.BulletList || block.type is BlockType.NumberedList
    val listAutoDetectObserver = remember(
        block.id,
        isCurrentlyList,
        callbacks,
        textStates,
        spanStates,
        textHistoryTracker,
    ) {
        ListAutoDetectObserver(
            isListBlock = { isCurrentlyList },
            onListDetected = { newType, prefixLength ->
                stateHolder.runStructuralHistoryTransaction(textStates, spanStates) {
                    textHistoryTracker.noteBatchBreaker()
                    // Remove trigger prefix and adjust spans
                    spanStates.adjustForRangeReplacement(
                        blockId = block.id,
                        start = 0,
                        endExclusive = prefixLength,
                        replacementLength = 0,
                    )
                    val newText = textStates.replaceVisibleRange(
                        blockId = block.id,
                        start = 0,
                        endExclusive = prefixLength,
                        replacement = "",
                        cursorPositionAfter = 0,
                    )
                    // Sync snapshot with cleaned text + adjusted spans before converting
                    if (newText != null) {
                        val spans = spanStates.getSpans(block.id)
                        callbacks.dispatch(UpdateBlockContent(block.id, BlockContent.Text(newText, spans)))
                    }
                    callbacks.dispatch(ConvertBlockType(block.id, newType))
                }
            },
            initialVisibleText = textFieldState.visibleText(),
        )
    }

    LaunchedEffect(isFocused, textHistoryTracker, stateHolder, textStates, spanStates) {
        textHistoryTracker.noteFocusChanged(
            stateHolder.captureCheckpoint(textStates, spanStates)
        )
        if (isFocused) {
            focusRequester.requestFocus()
        }
        // Don't call clearFocus() here when isFocused becomes false.
        // When focus moves to another block, that block's requestFocus()
        // moves Compose focus automatically, keeping the keyboard open.
        // CascadeEditor handles clearFocus() when focusedBlockId becomes null.
    }

    // Keep observer tracking in sync when slash session closes or moves externally.
    LaunchedEffect(slashSessionAnchorBlockId, slashTextObserver, block.id) {
        if (slashSessionAnchorBlockId != block.id && slashTextObserver.isTracking) {
            slashTextObserver.notifySessionClosed()
        }
    }

    // Combined text + selection observation keeps ordering deterministic.
    // We observe raw text snapshots and only derive visible text when text actually changes.
    LaunchedEffect(
        textFieldState,
        spanTextObserver,
        slashTextObserver,
        listAutoDetectObserver,
        stateHolder,
        textHistoryTracker,
    ) {
        var lastObservedVisibleText = textFieldState.visibleText()
        var lastObservedTextSnapshot = textFieldState.text
        fun noteSelectionState(selection: TextRange) {
            textHistoryTracker.noteSelectionChanged(
                selection = selection,
                ui = captureFocusedEditingUiState(stateHolder.state, textStates, spanStates),
            )
            slashTextObserver.onSelectionChanged(selection.start, selection.end)
        }

        snapshotFlow {
            Pair(textFieldState.text, textFieldState.visibleSelection())
        }.collect { (currentTextSnapshot, selection) ->
            val textSnapshotChanged =
                currentTextSnapshot !== lastObservedTextSnapshot ||
                    currentTextSnapshot.length != lastObservedTextSnapshot.length
            val currentVisibleText = if (textSnapshotChanged) {
                visibleTextFromSnapshot(currentTextSnapshot)
            } else {
                lastObservedVisibleText
            }

            if (stateHolder.isApplyingHistory) {
                if (textSnapshotChanged) {
                    lastObservedVisibleText = currentVisibleText
                    lastObservedTextSnapshot = currentTextSnapshot
                }
                // Replay baseline sync is driven by the holder:
                // - BlockTextEntry replay patches the registered tracker directly
                // - Structural replay clears runtime holders, forcing tracker recreation
                return@collect
            }

            if (textSnapshotChanged) {
                // Peek before span observer consumes the commit
                val isProgrammatic = textStates.hasPendingProgrammaticCommit(block.id)
                val cursor = if (selection.collapsed) selection.start else -1
                if (currentVisibleText != lastObservedVisibleText) {
                    slashTextObserver.onTextChanged(currentVisibleText, isProgrammatic, cursor)
                    spanTextObserver.onCommittedVisibleText(currentVisibleText)
                    if (isProgrammatic) {
                        textHistoryTracker.onProgrammaticCommit(
                            stateHolder.captureCheckpoint(textStates, spanStates)
                        )
                    } else {
//                        val afterCheckpoint = stateHolder.captureCheckpoint(textStates, spanStates)
//                        textHistoryTracker.onUserTextCommit(afterCheckpoint)?.let { push ->
//                            stateHolder.pushHistoryEntry(push.entry, push.policy)
//                        }
                        // Sync runtime text + spans back into the immutable snapshot on every
                        // keystroke. Without this, EditorState.blocks only updates on structural
                        // actions (split, merge, slash commands), so content typed into the first
                        // block appears "lost" until the next structural event forces a sync.
                        val currentSpans = spanStates.getSpans(block.id)
                        callbacks.dispatch(
                            UpdateBlockContent(
                                blockId = block.id,
                                content = BlockContent.Text(
                                    text = currentVisibleText,
                                    spans = currentSpans,
                                ),
                            )
                        )
                        val afterCheckpoint = stateHolder.captureCheckpoint(textStates, spanStates)
                        textHistoryTracker.onUserTextCommit(afterCheckpoint)?.let { push ->
                            stateHolder.pushHistoryEntry(push.entry, push.policy)
                        }
                    }
                    listAutoDetectObserver.onTextChanged(currentVisibleText, isProgrammatic)
                    lastObservedVisibleText = currentVisibleText
                } else {
                    // Snapshot identity can change without visible text mutation
                    // (e.g. setText with identical content during split).
                    // Consume any stale programmatic commit so it doesn't cause
                    // the next real user edit to be misidentified as programmatic.
//                    if (isProgrammatic) {
//                        textStates.consumeProgrammaticCommit(block.id)
//                        textHistoryTracker.onProgrammaticCommit(
//                            stateHolder.captureCheckpoint(textStates, spanStates)
//                        )
//                    } else {
//                        noteSelectionState(selection)
//                    }

                    if (isProgrammatic) {
                        textHistoryTracker.onProgrammaticCommit(
                            stateHolder.captureCheckpoint(textStates, spanStates)
                        )
                    } else {
                        // ... our earlier fix
                        val currentSpans = spanStates.getSpans(block.id)
                        callbacks.dispatch(
                            UpdateBlockContent(
                                blockId = block.id,
                                content = BlockContent.Text(
                                    text = currentVisibleText,
                                    spans = currentSpans
                                )
                            )
                        )
                        val afterCheckpoint = stateHolder.captureCheckpoint(textStates, spanStates)
                        textHistoryTracker.onUserTextCommit(afterCheckpoint)?.let { push ->
                            stateHolder.pushHistoryEntry(push.entry, push.policy)
                        }
                    }
                }
                lastObservedTextSnapshot = currentTextSnapshot
            } else {
                // Selection-only change: check slash session cursor validity
                noteSelectionState(selection)
            }
        }
    }

    // ── Span-only sync ────────────────────────────────────────────────────────────
// Bold / italic / highlight / code applied via toolbar mutate spanState.value
// without touching textFieldState, so the main snapshotFlow never fires for
// them. This observer watches spanState directly and syncs every span mutation
// into EditorState.blocks so the save path sees current formatting.
    LaunchedEffect(block.id, spanState, textFieldState, spanStates, callbacks) {
        snapshotFlow { spanState.value }
            .distinctUntilChanged()
            .drop(1) // skip initial emission — snapshot already in sync on first composition
            .collect {
                if (stateHolder.isApplyingHistory) return@collect
                callbacks.dispatch(
                    UpdateBlockContent(
                        blockId = block.id,
                        content = BlockContent.Text(
                            text = textFieldState.visibleText(),
                            spans = spanStates.getSpans(block.id),
                        ),
                    )
                )
            }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val isSlashAnchor = slashSessionAnchorBlockId == block.id

    // Report caret rect in window coordinates when this block is the slash anchor.
    LaunchedEffect(isSlashAnchor, textLayoutResult, layoutCoordinates, textFieldState.selection) {
        if (!isSlashAnchor) {
            // Only the previous anchor owner can clear the shared caret rect.
            slashCaretRectHolder.clear(block.id)
            return@LaunchedEffect
        }
        val layout = textLayoutResult ?: return@LaunchedEffect
        val coords = layoutCoordinates ?: return@LaunchedEffect
        if (!coords.isAttached) return@LaunchedEffect

        val cursorOffset = textFieldState.selection.start
        val safeOffset = cursorOffset.coerceIn(0, layout.layoutInput.text.length)
        val localCursorRect = layout.getCursorRect(safeOffset)
        val windowTopLeft = coords.localToWindow(
            Offset(localCursorRect.left, localCursorRect.top)
        )
        val windowBottomRight = coords.localToWindow(
            Offset(localCursorRect.right, localCursorRect.bottom)
        )
        slashCaretRectHolder.update(
            blockId = block.id,
            caretRect = Rect(
                left = windowTopLeft.x,
                top = windowTopLeft.y,
                right = windowBottomRight.x,
                bottom = windowBottomRight.y,
            )
        )
    }

    val cursorBrush = remember(colors.cursor) { SolidColor(colors.cursor) }

    val keyHandler = remember(formattingActions, callbacks, textHistoryTracker, stateHolder) {
        TextBlockKeyHandler(
            formattingActions = formattingActions,
            callbacks = callbacks,
            isSlashAnchor = { isSlashAnchor },
            slashHighlightedCommandId = { slashHighlightedCommandId },
            slashPopupItems = { slashPopupItems },
            slashCommandExecutor = { slashCommandExecutor },
            onBatchBreaker = { textHistoryTracker.noteBatchBreaker() },
            onPasteShortcutDetected = { textHistoryTracker.noteExplicitPaste() },
            onUndo = stateHolder::undo,
            onRedo = stateHolder::redo,
        )
    }
    var handledPhysicalEnterKey by remember { mutableStateOf<Key?>(null) }

    fun handleEditorEnter(cursorPosition: Int) {
        val highlightedItemId = slashHighlightedCommandId
        val highlightedItem = if (highlightedItemId != null) {
            slashPopupItems.firstOrNull { it.id == highlightedItemId }
        } else {
            null
        }
        val executor = slashCommandExecutor
        if (
            slashSessionAnchorBlockId == block.id &&
            highlightedItem != null &&
            executor != null
        ) {
            textHistoryTracker.noteBatchBreaker()
            executor.execute(highlightedItem)
            return
        }
        textHistoryTracker.noteBatchBreaker()
        callbacks.onEnter(block.id, cursorPosition)
    }

    fun handlePhysicalEnterKeyEvent(keyEvent: KeyEvent): Boolean {
        val enterKey = when (keyEvent.key) {
            Key.Enter, Key.NumPadEnter -> keyEvent.key
            else -> null
        } ?: return false

        if (keyEvent.isShiftPressed) return false

        return when (keyEvent.type) {
            KeyEventType.KeyUp -> {
                if (handledPhysicalEnterKey == enterKey) {
                    handledPhysicalEnterKey = null
                    true
                } else {
                    false
                }
            }
            KeyEventType.KeyDown -> {
                if (handledPhysicalEnterKey != enterKey) {
                    handledPhysicalEnterKey = enterKey
                    handleEditorEnter(textFieldState.visibleSelection().start)
                }
                true
            }
            else -> true
        }
    }

    Box(modifier = modifier) {
        BackspaceAwareTextField(
            state = textFieldState,
            modifier = Modifier.fillMaxWidth()
                .onGloballyPositioned { coords -> layoutCoordinates = coords }
                .onPreviewKeyEvent {
                    keyHandler.onPreviewKeyEvent(it) || handlePhysicalEnterKeyEvent(it)
                }
                .onFocusChanged { focusState ->
                    val wasFocused = hasComposeFocus
                    hasComposeFocus = focusState.isFocused
                    if (focusState.isFocused && !isFocused) {
                        callbacks.onFocus(block.id)
                    }
                    if (!focusState.isFocused && wasFocused) {
                        slashTextObserver.onFocusLost()
                        // KeyUp after a split lands on the new block (focus has already
                        // migrated), so it never clears this block's KeyDown marker.
                        // Reset on focus loss so a later re-focus can handle Enter again.
                        handledPhysicalEnterKey = null
                    }
                },
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            outputTransformation = outputTransformation,
            onTextLayout = { result -> textLayoutResult = result() },
            focusRequester = focusRequester,
            onBackspaceAtStart = {
                textHistoryTracker.noteBatchBreaker()
                callbacks.onBackspaceAtStart(block.id)
            },
            onEnterPressed = { cursorPosition ->
                handleEditorEnter(cursorPosition)
            }
        )

        if (!isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(block.id) {
                        detectTapGestures(
                            onTap = { offset ->
                                val cursorIndex = textLayoutResult?.getOffsetForPosition(offset)
                                    ?: textFieldState.text.length
                                textFieldState.edit {
                                    selection = TextRange(cursorIndex)
                                }
                                // Route focus through editor callbacks so selection mode
                                // can veto focus (focus/selection are mutually exclusive).
                                callbacks.onFocus(block.id)
                            }
                        )
                    }
            )
        }
    }
}

private const val ZWSP_SENTINEL: Char = '\u200B'

private fun visibleTextFromSnapshot(text: CharSequence): String {
    if (text.isEmpty()) return ""
    return if (text[0] == ZWSP_SENTINEL) {
        text.subSequence(1, text.length).toString()
    } else {
        text.toString()
    }
}
