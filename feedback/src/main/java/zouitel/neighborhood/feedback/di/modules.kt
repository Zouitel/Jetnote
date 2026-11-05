package zouitel.neighborhood.feedback.di

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
import zouitel.neighborhood.feedback.ui.FeedbackScreenModel
import zouitel.neighborhood.feedback.worker.FeedbackUploadWorker
import zouitel.neighborhood.feedback.worker.FeedbackWorker

val feedbackModule = module {
    factory { FeedbackScreenModel(get()) }

    // No GitHub authorization header or privileged credential is shipped in the app.
    single {
        HttpClient(CIO) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    isLenient = false
                    ignoreUnknownKeys = true
                })
            }
            defaultRequest {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        }
    }

    worker {
        FeedbackWorker(
            context = androidContext(),
            params = get(),
            githubClient = get(),
            repoOwner = "Zouitel",
            repoName = "Jetnote"
        )
    }

    workerOf(::FeedbackUploadWorker)
}
