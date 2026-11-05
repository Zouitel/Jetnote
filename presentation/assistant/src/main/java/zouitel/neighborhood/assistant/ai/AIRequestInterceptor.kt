package zouitel.neighborhood.assistant.ai

import zouitel.neighborhood.assistant.utils.JsonInstant
import zouitel.neighborhood.assistant.utils.jsonPrimitiveOrNull
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val SILICONFLOW_HOST = "api.siliconflow.cn"
private const val AI_PROXY_HOST = "us-central1-jetnote-cloud-service.cloudfunctions.net"
private const val AI_PROXY_PREFIX = "/aiProxy"
private val MANAGED_KEY_PLACEHOLDERS = setOf("Bearer", "Bearer sk-")

/**
 * Routes requests which rely on Jetnote's managed SiliconFlow key through the
 * authenticated backend. Requests with a user's own provider key remain direct.
 *
 * The Firebase ID token is short-lived and is never persisted by this interceptor.
 */
class AIRequestInterceptor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseAuth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != SILICONFLOW_HOST) return chain.proceed(request)

        val path = request.url.encodedPath
        if (path !in setOf("/v1/chat/completions", "/v1/models")) {
            return chain.proceed(request)
        }

        val authorization = request.header("Authorization")?.trim()
        if (authorization !in MANAGED_KEY_PLACEHOLDERS) {
            // Bring-your-own-key requests are paid directly by the user and do not
            // consume Jetnote's managed-key quota.
            return chain.proceed(request)
        }

        if (path == "/v1/chat/completions") {
            val bodyJson = request.readBodyAsJson()
            val model = bodyJson
                ?.jsonObject
                ?.get("model")
                ?.jsonPrimitiveOrNull
                ?.content
                ?: bodyJson
                    ?.jsonObject
                    ?.get("currentModel")
                    ?.jsonPrimitiveOrNull
                    ?.content

            val freeModels = remoteConfig.getString("silicon_cloud_free_models")
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()

            if (!model.isNullOrBlank() && model !in freeModels) {
                throw IOException("This model is not available through Jetnote's managed AI service.")
            }
        }

        val idToken = getFirebaseIdToken()
        val proxyUrl = request.url.newBuilder()
            .scheme("https")
            .host(AI_PROXY_HOST)
            .port(443)
            .encodedPath("$AI_PROXY_PREFIX$path")
            .build()

        val authenticatedRequest = request.newBuilder()
            .url(proxyUrl)
            .header("Authorization", "Bearer $idToken")
            .header("X-Jetnote-Client", "android")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    @Synchronized
    private fun getFirebaseIdToken(): String {
        try {
            // Jetnote uses an anonymous Firebase session until the user links
            // a permanent sign-in provider.
            val user = firebaseAuth.currentUser
                ?: Tasks.await(firebaseAuth.signInAnonymously(), 10, TimeUnit.SECONDS).user
                ?: throw IOException("Firebase authentication did not return a user.")

            return Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS).token
                ?: throw IOException("Firebase authentication did not return an ID token.")
        } catch (error: Exception) {
            if (error is IOException) throw error
            throw IOException("Unable to authenticate the AI request.", error)
        }
    }
}

private fun Request.readBodyAsJson(): JsonElement? {
    val contentType = body?.contentType()
    if (contentType?.type == "application" && contentType.subtype == "json") {
        val buffer = Buffer()
        body?.writeTo(buffer)
        return JsonInstant.parseToJsonElement(buffer.readUtf8())
    }
    return null
}
