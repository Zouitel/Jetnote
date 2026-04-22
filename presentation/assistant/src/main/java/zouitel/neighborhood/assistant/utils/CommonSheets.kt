package zouitel.neighborhood.assistant.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.assistant.model.AssistantAffectScope
import zouitel.neighborhood.assistant.model.AssistantRegex
import zouitel.neighborhood.assistant.model.QuickMessage
import zouitel.neighborhood.assistant.model.SheetContent
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConfirm
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonInsert
import zouitel.neighborhood.ui.CommonTextField
import kotlinx.serialization.json.JsonPrimitive
import zouitel.neighborhood.logic.R

@Composable
fun SheetLayout(
    content: SheetContent,
    onClose: () -> Unit
) {
    when (content) {
        is SheetContent.Quick -> QuickMessageSheet(
            content.quickMessage,
            onClose,
            content.onDone
        )

        is SheetContent.Regex -> MessageRegexSheet(
            content.messageRegex,
            onClose,
            content.onDone
        )

        is SheetContent.Body -> BodySheet(content.body, onClose, content.onDone)
        is SheetContent.Header -> HeaderSheet(content.header, onClose, content.onDone)
//        is SheetContent.Modes -> ModelsListSheet(content.models, onClose, content.onDone)
        is SheetContent.Reset -> ResetSheet(onClose, content.confirm)
        is SheetContent.Save -> SaveSheet(content.onSave, content.onDiscard, onClose)
        else -> Unit
    }
}

@Composable
internal fun QuickMessageSheet(
    quickMessage: QuickMessage,
    onClose: () -> Unit,
    onDone: (QuickMessage) -> Unit
) {
    var message by remember { mutableStateOf(quickMessage) }
    ModalBottomSheet(onClose) {
        Text(
            stringResource(R.string.message),
            Modifier.padding(10.dp, 20.dp),
            style = MaterialTheme.typography.titleLarge
        )
        CommonTextField(
            message.title,
            { message = message.copy(title = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.title)
        )
        CommonTextField(
            message.content,
            { message = message.copy(content = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.content)
        )
        CommonInsert(message.title.isNotEmpty() && message.content.isNotEmpty()) {
            onDone(message)
            onClose()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageRegexSheet(
    regex: AssistantRegex,
    onClose: () -> Unit,
    onDone: (AssistantRegex) -> Unit
) {
    var currentRegex by remember { mutableStateOf(regex) }
    val sheetState = rememberModalBottomSheetState(true)

    ModalBottomSheet(onClose, sheetState = sheetState) {
        Row(
            Modifier.fillMaxWidth()
                .padding(8.dp),
            Arrangement.SpaceBetween
        ) {
            Text(
                "Regex",
                style = MaterialTheme.typography.titleLarge
            )
            Switch(
                currentRegex.enabled,
                {
                    currentRegex = currentRegex.copy(enabled = it)
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = CommonColors.prime,
                    checkedThumbColor = Color.White
                )
            )
        }
        CommonTextField(
            currentRegex.name,
            { currentRegex = currentRegex.copy(name = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.name_sort).uppercase()
        )
        CommonTextField(
            currentRegex.findRegex,
            { currentRegex = currentRegex.copy(findRegex = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.find)
        )
        CommonTextField(
            currentRegex.replaceString,
            { currentRegex = currentRegex.copy(replaceString = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.replace),
            textColor = MaterialTheme.colorScheme.error
        )

        Text(
            stringResource(R.string.effecting_scopes),
            Modifier.padding(8.dp, 20.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Column(Modifier.padding(horizontal = 8.dp)) {
            Row {
                FilledTonalButton(
                    {
                        currentRegex = if (AssistantAffectScope.ASSISTANT in currentRegex.affectingScope) currentRegex.copy(
                            affectingScope = currentRegex.affectingScope.minusElement(
                                AssistantAffectScope.ASSISTANT
                            )
                        ) else currentRegex.copy(
                            affectingScope = currentRegex.affectingScope.plusElement(
                                AssistantAffectScope.ASSISTANT
                            )
                        )
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            if (AssistantAffectScope.ASSISTANT in currentRegex.affectingScope) CommonColors.prime else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (AssistantAffectScope.ASSISTANT in currentRegex.affectingScope) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    AnimatedVisibility(AssistantAffectScope.ASSISTANT in currentRegex.affectingScope) {
                        Icon(
                            painterResource(CommonIcons.CHECK_18),
                            null,
                            modifier = Modifier.padding(horizontal = 5.dp)
                        )
                    }

                    Text(
                        stringResource(R.string.assistant).lowercase(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                Spacer(Modifier.width(20.dp))

                FilledTonalButton({
                    currentRegex = if (AssistantAffectScope.USER in currentRegex.affectingScope) currentRegex.copy(
                        affectingScope = currentRegex.affectingScope.minusElement(
                            AssistantAffectScope.USER
                        )
                    ) else currentRegex.copy(
                        affectingScope = currentRegex.affectingScope.plusElement(
                            AssistantAffectScope.USER
                        )
                    )
                },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            if (AssistantAffectScope.USER in currentRegex.affectingScope) CommonColors.prime else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (AssistantAffectScope.USER in currentRegex.affectingScope) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    AnimatedVisibility(AssistantAffectScope.USER in currentRegex.affectingScope) {
                        Icon(
                            painterResource(CommonIcons.CHECK_18),
                            null,
                            modifier = Modifier.padding(horizontal = 5.dp)
                        )
                    }

                    Text(
                        stringResource(R.string.user),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            FilledTonalButton({
                currentRegex = currentRegex.copy(visualOnly = currentRegex.visualOnly.not())
            },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor =
                        if (currentRegex.visualOnly) CommonColors.prime else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (currentRegex.visualOnly) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                )
            ) {
                AnimatedVisibility(currentRegex.visualOnly) {
                    Icon(
                        painterResource(CommonIcons.CHECK_18),
                        null,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                }

                Text(
                    stringResource(R.string.visual_only),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        CommonInsert(
            currentRegex.name.isNotEmpty() &&
                    currentRegex.findRegex.isNotEmpty() &&
                    currentRegex.replaceString.isNotEmpty()
        ) {
            onDone(currentRegex)
            onClose()
        }
    }
}

@Composable
internal fun BodySheet(
        currentBody: CustomBody,
        onClose: () -> Unit,
        onDone: (CustomBody) -> Unit
    ) {
        var key by remember { mutableStateOf(currentBody.key) }
        var value by remember { mutableStateOf(currentBody.value.toString().removeSurrounding("\"")) }

        ModalBottomSheet(onClose) {
            Text(
                stringResource(R.string.body),
                Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleLarge
            )
            CommonTextField(
                key,
                { key = it },
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                stringResource(R.string.key)
            )
            CommonTextField(
                value,
                { value = it },
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                "\"${stringResource(R.string.value)}\""
            )
            CommonInsert(key.isNotEmpty() && value.isNotEmpty()) {
                onDone(currentBody.copy(key = key, value = JsonPrimitive(value)))
                onClose()
            }
        }
    }

@Composable
internal fun HeaderSheet(
    currentHeader: CustomHeader,
    onClose: () -> Unit,
    onDone: (CustomHeader) -> Unit
) {
    var header by remember { mutableStateOf(currentHeader) }
    ModalBottomSheet(onClose) {
        Text(stringResource(R.string.header), Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge)
        CommonTextField(
            header.name,
            { header = header.copy(name = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.name_sort).uppercase()
        )
        CommonTextField(
            header.value,
            { header = header.copy(value = it) },
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            stringResource(R.string.value)
        )
        CommonInsert(header.name.isNotEmpty() && header.value.isNotEmpty()) {
            onDone(header)
            onClose()
        }
    }
}

@Composable
internal fun ResetSheet(
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(onClose) {
        CommonActivationItem(
            stringResource(R.string.reset_description),
            null,
            null,
            null,
            content = {
                CommonConfirm {
                    onConfirm()
                    onClose()
                }
            }
        )
    }
}

@Composable
internal fun SaveSheet(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.save_changes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.save_changes_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDiscard()
                        onClose()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.dont_save))
                }

                Button(
                    onClick = {
                        onSave()
                        onClose()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommonColors.prime,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
