package zouitel.neighborhood.feedback

import zouitel.neighborhood.feedback.Contents.GITHUB_TOKEN
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val feedbackModule = module {
    // ScreenModel
    factory { FeedbackScreenModel(get()) }

    // GitHub HTTP Client
    single {
        HttpClient(CIO) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer $GITHUB_TOKEN")
                header(HttpHeaders.Accept, "application/vnd.github.v3+json")
            }
        }
    }

    // WorkManager Worker with GitHub image upload
    worker {
        FeedbackWorker(
            context = androidContext(),
            params = get(),
            githubClient = get(),
            repoOwner = "Zouitel",    // ← Organization name
            repoName = "Jetnote"       // ← Repository name
        )
    }

    workerOf(::FeedbackUploadWorker)
}

// ============================================================================
// CONFIGURATION GUIDE
// ============================================================================
//
// 1. Replace "YOUR_GITHUB_USERNAME" with your actual GitHub username
//    Example: repoOwner = "JohnDoe"
//
// 2. Replace "YOUR_REPO_NAME" with your repository name
//    Example: repoName = "my-android-app"
//
// 3. Make sure your GITHUB_TOKEN has these permissions:
//    - repo (Full control of private repositories)
//    - OR public_repo (if your repo is public)
//
// 4. Images will be uploaded to: YOUR_REPO/feedback-screenshots/
//    - The folder will be created automatically
//    - Files are named: screenshot_YYYYMMDD_HHMMSS_uniqueid.jpg
//
// 5. Default branch is "main" - if yours is different (e.g., "master"),
//    you can modify it in GitHubImageUploader
//
// ============================================================================