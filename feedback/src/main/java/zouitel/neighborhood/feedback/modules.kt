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
import org.koin.dsl.module

val feedbackModule = module {
    factory { FeedbackScreenModel(get()) }

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

    worker {
        FeedbackWorker(androidContext(), get(), get())
    }
}

