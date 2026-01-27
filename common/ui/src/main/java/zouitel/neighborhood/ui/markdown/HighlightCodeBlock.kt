package zouitel.neighborhood.ui.markdown

import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import zouitel.neighborhood.ui.hightlight.HighlightText
import zouitel.neighborhood.ui.hightlight.Highlighter
import zouitel.neighborhood.ui.hightlight.LocalHighlighter
import zouitel.neighborhood.ui.hightlight.buildHighlightText
import zouitel.neighborhood.ui.webview.WebViewPageScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun HighlightCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    completeCodeBlock: Boolean = true,
    style: TextStyle? = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
) {
    val darkMode = LocalDarkMode.current
    val colorPalette = if (darkMode) AtomOneDarkPalette else AtomOneLightPalette
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(code.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
    ) {
        HighlightCodeActions(
            language = language,
            scope = scope,
            clipboardManager = clipboardManager,
            code = code,
            createDocumentLauncher = createDocumentLauncher,
            navigator = navigator
        )
        if (completeCodeBlock && language == "mermaid") {
            Mermaid(
                code = code,
                modifier = Modifier.fillMaxWidth(),
            )
            return
        }
        val textStyle = LocalTextStyle.current.merge(style)
        SelectionContainer {
            HighlightText(
                code = code,
                language = language,
                modifier = Modifier
                    .horizontalScroll(scrollState),
                fontSize = textStyle.fontSize,
                lineHeight = textStyle.lineHeight,
                colors = colorPalette,
                overflow = TextOverflow.Visible,
                softWrap = false,
                fontFamily = JetbrainsMono
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun HighlightCodeActions(
    language: String,
    scope: CoroutineScope,
    clipboardManager: Clipboard,
    code: String,
    createDocumentLauncher: ManagedActivityResultLauncher<String, Uri?>,
    navigator: Navigator?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
                .copy(alpha = 0.5f),
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    scope.launch {
                        clipboardManager.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText("code", code),
                            )
                        )
                    }
                }
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Save",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    val extension = when (language.lowercase()) {
                        "kotlin" -> "kt"
                        "java" -> "java"
                        "python" -> "py"
                        "javascript" -> "js"
                        "typescript" -> "ts"
                        "cpp", "c++" -> "cpp"
                        "c" -> "c"
                        "html" -> "html"
                        "css" -> "css"
                        "xml" -> "xml"
                        "json" -> "json"
                        "yaml", "yml" -> "yml"
                        "markdown", "md" -> "md"
                        "sql" -> "sql"
                        "sh", "bash" -> "sh"
                        else -> "txt"
                    }
                    createDocumentLauncher.launch(
                        "code_${
                            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }.$extension"
                    )
                }
            )

            Text(
                text = "Copy Code",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    scope.launch {
                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("code", code)))
                    }
                }
            )

            if (language == "html") {
                Text(
                    text = "Preview",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable {
                            navigator?.push(WebViewPageScreen(content = code.base64Encode()))
                        }
                )
            }
        }
    }
}

class HighlightCodeVisualTransformation(
    val language: String,
    val highlighter: Highlighter,
    val darkMode: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = try {
            val colorPalette = if (darkMode) AtomOneDarkPalette else AtomOneLightPalette
            if (text.text.isEmpty()) {
                AnnotatedString("")
            } else {
                runBlocking {
                    val tokens = highlighter.highlight(text.text, language)
                    buildAnnotatedString {
                        tokens.forEach { token ->
                            buildHighlightText(token, colorPalette)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AnnotatedString(text.text)
        }

        return TransformedText(
            text = annotatedString,
            offsetMapping = OffsetMapping.Identity
        )
    }

    companion object {
        @Composable
        fun regex() = HighlightCodeVisualTransformation(
            language = "regex",
            highlighter = LocalHighlighter.current,
            darkMode = LocalDarkMode.current,
        )
    }
}
