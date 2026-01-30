package zouitel.neighborhood.feedback

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.AnimatedCircleCheckbox
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTextField
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedbackScreen : Screen {
    @Composable
    override fun Content() {
        ReportIssue(getScreenModel())
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun ReportIssue(feedbackModel: FeedbackScreenModel) {
        val keyboardManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val bottomNav = LocalBottomSheetNavigator.current

        val titleState = rememberTextFieldState()
        val bodyState = rememberTextFieldState()
        val scope = rememberCoroutineScope()
        val systemLogs = remember { mutableStateOf(false) }
        val imageUri = remember { mutableStateOf<Uri?>(null) }
        val toolTipState = rememberTooltipState(isPersistent = true)
        val labels = remember { mutableStateListOf<Label>() }

        // Submission state tracking
        var isSubmitting by remember { mutableStateOf(false) }
        var submissionStage by remember { mutableStateOf("") }

        // Check if user is authenticated
        val currentUser = remember { FirebaseAuth.getInstance().currentUser }
        val isAuthenticated = currentUser != null

        // ✅ Observe both upload and submission status
        val uploadStatus by feedbackModel.uploadStatus.collectAsState()
        val submissionStatus by feedbackModel.submissionStatus.collectAsState()

        // ✅ NEW: Handle upload completion during submission
        LaunchedEffect(uploadStatus) {
            if (isSubmitting) {
                when (uploadStatus) {
                    is FeedbackScreenModel.UploadStatus.Uploading -> {
                        submissionStage = "Uploading screenshot..."
                    }
                    is FeedbackScreenModel.UploadStatus.Success -> {
                        submissionStage = "Creating GitHub issue..."
                        val driveLink = (uploadStatus as FeedbackScreenModel.UploadStatus.Success).driveLink

                        // Submit the feedback with Drive link
                        feedbackModel.enqueueIssueProcessing(
                            title = titleState.text.toString(),
                            body = if (systemLogs.value) bodyState.text.toString() + "\n\n" + includeLog()
                            else bodyState.text.toString(),
                            labels = labels.map { it.name }.toTypedArray(),
                            imageUri = imageUri.value,
                            driveLink = driveLink
                        )
                    }
                    is FeedbackScreenModel.UploadStatus.Failed -> {
                        submissionStage = "Upload failed, creating issue..."

                        // Submit without Drive link if upload failed
                        feedbackModel.enqueueIssueProcessing(
                            title = titleState.text.toString(),
                            body = if (systemLogs.value) bodyState.text.toString() + "\n\n" + includeLog()
                            else bodyState.text.toString(),
                            labels = labels.map { it.name }.toTypedArray(),
                            imageUri = imageUri.value,
                            driveLink = null
                        )
                    }
                    else -> { /* Idle state */ }
                }
            }
        }

        // ✅ NEW: Handle submission completion
        LaunchedEffect(submissionStatus) {
            when (submissionStatus) {
                is FeedbackScreenModel.SubmissionStatus.Submitting -> {
                    submissionStage = "Creating GitHub issue..."
                }
                is FeedbackScreenModel.SubmissionStatus.Success -> {
                    val success = submissionStatus as FeedbackScreenModel.SubmissionStatus.Success
                    submissionStage = "Issue created!"

                    // Wait briefly to show success message
                    delay(1000)

                    // Reset and close
                    isSubmitting = false
                    feedbackModel.resetSubmissionState()
                    bottomNav.pop()
                }
                is FeedbackScreenModel.SubmissionStatus.Failed -> {
                    val failed = submissionStatus as FeedbackScreenModel.SubmissionStatus.Failed
                    submissionStage = "Failed: ${failed.reason}"

                    // Wait to show error
                    delay(3000)

                    // Reset state but don't close (let user retry)
                    isSubmitting = false
                    feedbackModel.resetSubmissionState()
                }
                else -> { /* Idle */ }
            }
        }

        // Gallery picker (just selects image, doesn't upload)
        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            imageUri.value = uri
        }

        // Calculate if submit is enabled
        val isSendEnabled = titleState.text.isNotBlank() && !isSubmitting

        Scaffold(
            modifier = Modifier.fillMaxSize().cloudy(enabled = toolTipState.isVisible),
            bottomBar = {
                BottomAppBar(
                    contentPadding = PaddingValues(10.dp),
                    containerColor = Color.Transparent
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (isSendEnabled) {
                                    isSubmitting = true

                                    // Check if there's an image to upload
                                    if (imageUri.value != null && isAuthenticated) {
                                        // Start upload first
                                        submissionStage = "Uploading screenshot..."
                                        feedbackModel.uploadScreenshotToDrive(imageUri.value!!)
                                        // LaunchedEffect will handle the rest
                                    } else {
                                        // No image or not authenticated, submit directly
                                        submissionStage = "Creating GitHub issue..."
                                        feedbackModel.enqueueIssueProcessing(
                                            title = titleState.text.toString(),
                                            body = if (systemLogs.value) bodyState.text.toString() + "\n\n" + includeLog()
                                            else bodyState.text.toString(),
                                            labels = labels.map { it.name }.toTypedArray(),
                                            imageUri = imageUri.value,
                                            driveLink = null
                                        )
                                        // LaunchedEffect will handle completion
                                    }
                                }
                            }
                        },
                        enabled = isSendEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CommonColors.prime,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(submissionStage, style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Submit Feedback!", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    CommonTextField(
                        state = titleState,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Issue Title*",
                        textSize = 23.sp,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        imeAction = ImeAction.Next,
                        keyboardActions = {
                            keyboardManager.moveFocus(FocusDirection.Next)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    ContextualFlowRow(defaultLabels.size) { index ->
                        InputChip(
                            defaultLabels[index] in labels,
                            {
                                if (labels.contains(defaultLabels[index])) {
                                    labels.minusAssign(defaultLabels[index])
                                } else {
                                    labels.plusAssign(defaultLabels[index])
                                }
                            },
                            {
                                Text(defaultLabels[index].name)
                            },
                            Modifier.padding(2.dp),
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = defaultLabels[index].color.copy(alpha = .3f),
                                disabledSelectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                selectedLabelColor = defaultLabels[index].color,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                item {
                    Box(contentAlignment = Alignment.TopEnd) {
                        CommonTextField(
                            state = bodyState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(220.dp),
                            placeholder = "Issue Description(Optional)",
                            textSize = 18.sp,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            imeAction = ImeAction.Done,
                            keyboardActions = {
                                keyboardController?.hide()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    ElevatedCard(
                        onClick = {
                            if (!isSubmitting) {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when {
                                imageUri.value != null -> {
                                    AsyncImage(
                                        model = imageUri.value,
                                        contentDescription = "screenshot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isAuthenticated) {
                                        Text(
                                            text = "✓ Screenshot selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "Will upload to Drive on submit",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.alpha(.6f)
                                        )
                                    } else {
                                        Text(
                                            text = "✓ Screenshot selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "Will upload to GitHub on submit",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.alpha(.6f)
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        painter = painterResource(CommonIcons.PHOTO_CAPTURE),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .alpha(.5f)
                                            .padding(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Select a screenshot",
                                        modifier = Modifier.alpha(.5f)
                                    )

                                    if (isAuthenticated) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Will upload to Drive on submit",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.alpha(.4f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Will upload to GitHub on submit",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.alpha(.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    TooltipBox(
                        TooltipDefaults.rememberRichTooltipPositionProvider(),
                        {
                            RichTooltip(
                                title = {
                                    Text(logsTitle, style = MaterialTheme.typography.titleMedium)
                                }
                            ){
                                Text(logsDescription)
                            }
                        },
                        toolTipState
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedCircleCheckbox(
                                systemLogs.value,
                                MaterialTheme.colorScheme.onSurface,
                                MaterialTheme.colorScheme.surface
                            ) { value ->
                                systemLogs.value = value
                            }

                            Text(
                                text = "Include system logs?",
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        toolTipState.show()
                                    }
                                }.padding(5.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}