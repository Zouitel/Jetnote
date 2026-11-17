package zouitel.neighborhood.feedback.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import com.github.takahirom.roborazzi.captureRoboImage

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FeedbackScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val feedbackModel = mockk<FeedbackScreenModel>(relaxed = true).also { model ->
        every { model.uploadStatus } returns MutableStateFlow(FeedbackScreenModel.UploadStatus.Idle)
        every { model.submissionStatus } returns MutableStateFlow(FeedbackScreenModel.SubmissionStatus.Idle)
    }

    @Test
    fun feedbackScreenDefaultState() {
        composeRule.setContent {
            MaterialTheme {
                BottomSheetNavigator {
                    FeedbackScreen().ReportIssue(feedbackModel, isAuthenticatedOverride = false)
                }
            }
        }

        composeRule.onRoot().captureRoboImage()
    }
}
