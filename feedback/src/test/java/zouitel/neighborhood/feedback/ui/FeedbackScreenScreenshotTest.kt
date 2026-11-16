package zouitel.neighborhood.feedback.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class FeedbackScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val feedbackModel = mockk<FeedbackScreenModel>(relaxed = true)

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef")
                    .setApiKey("screenshot-test-key")
                    .setProjectId("jetnote-screenshot-tests")
                    .build()
            )
        }

        every { feedbackModel.uploadStatus } returns MutableStateFlow(FeedbackScreenModel.UploadStatus.Idle)
        every { feedbackModel.submissionStatus } returns MutableStateFlow(FeedbackScreenModel.SubmissionStatus.Idle)
    }

    @Test
    fun feedbackScreenDefaultState() {
        composeRule.setContent {
            MaterialTheme {
                BottomSheetNavigator {
                    FeedbackScreen().ReportIssue(feedbackModel)
                }
            }
        }

        composeRule.onRoot().captureRoboImage()
    }
}
