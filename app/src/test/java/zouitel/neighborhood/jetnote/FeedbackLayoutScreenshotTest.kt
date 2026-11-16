package zouitel.neighborhood.jetnote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Deterministic screenshot contract for the feedback form's main layout regions.
 *
 * Keep this fixture aligned with FeedbackScreen when its Koin/Firebase-backed
 * state is extracted behind a previewable UI state. This test deliberately
 * avoids network, Firebase, Drive uploads and live user data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class FeedbackLayoutScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun feedbackFormLayout() {
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    FeedbackFormScreenshotFixture()
                }
            }
        }

        composeRule.onRoot().captureRoboImage()
    }
}

@Composable
private fun FeedbackFormScreenshotFixture() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Report an issue", style = MaterialTheme.typography.headlineSmall)
        Text("Describe the problem", style = MaterialTheme.typography.bodyLarge)
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InputChip(selected = true, onClick = {}, label = { Text("Bug") })
            InputChip(selected = false, onClick = {}, label = { Text("UI") })
            InputChip(selected = false, onClick = {}, label = { Text("Other") })
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Add a screenshot", style = MaterialTheme.typography.titleMedium)
                Text("Attach an image to help explain the issue.")
            }
        }
        Text("Include system logs", style = MaterialTheme.typography.bodyLarge)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                "Describe the issue…",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Submit feedback")
        }
    }
}
