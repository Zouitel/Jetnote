package zouitel.neighborhood.link.worker

import android.content.Context
import androidx.compose.ui.util.fastForEach
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.LinkUseCase
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.link.mapper.LinkMapper
import zouitel.neighborhood.link.model.Link
import zouitel.neighborhood.link.utils.Constants.UID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL
import kotlin.text.ifEmpty

class LinkWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher,
    private val insert: LinkUseCase.Insert,
    private val linkMapper: LinkMapper
): CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = withContext(ioDeprecated) {
        return@withContext try {

            val uid = inputData.getString(UID) ?: DEFAULT_TXT
            val text = inputData.getString("text") ?: DEFAULT_TXT

            val urls = extractUrls(text)
            if (urls.isEmpty()) return@withContext Result.failure()

            runCatching {
                val results = mutableListOf<Link>()
                urls.fastForEach { url ->
                    try {
                        results.add(fetchLinkMetadata(url))
                    } catch (_: Exception) {
                        // Continue with other URLs even if one fails
                    }
                }

                if (results.isEmpty()) error("Failed to fetch any link previews")

                results
            }.onFailure {
                error("Failed to fetch links: ${it.message}")
            }.onSuccess {
                it.fastForEach { link ->
                    insert.invoke(linkMapper.toDomain(link.copy(uid = uid)))
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    suspend fun fetchLinkMetadata(urlString: String): Link = withContext(Dispatchers.IO) {
        val url = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            "https://$urlString"
        } else {
            urlString
        }

        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get()

        val domain = URL(url).host.removePrefix("www.")

        val title = doc.select("meta[property=og:title]").attr("content")
            .ifEmpty { doc.select("meta[name=twitter:title]").attr("content") }
            .ifEmpty { doc.title() }

        val description = doc.select("meta[property=og:description]").attr("content")
            .ifEmpty { doc.select("meta[name=twitter:description]").attr("content") }
            .ifEmpty { doc.select("meta[name=description]").attr("content") }

        val imageUrl = doc.select("meta[property=og:image]").attr("content")
            .ifEmpty { doc.select("meta[name=twitter:image]").attr("content") }

        val iconUrl = doc.select("link[rel~=icon]").attr("abs:href")
            .ifEmpty { doc.select("link[rel~=shortcut icon]").attr("abs:href") }
            .ifEmpty { "$url/favicon.ico" }

        Link(
            url = url,
            title = title,
            description = description,
            image = imageUrl,
            icon = iconUrl,
            domain = domain
        )
    }

    fun extractUrls(text: String): List<String> {
        val urlPattern = Regex(
            pattern = """(https?://[^\s]+)|([a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?\.)+[a-zA-Z]{2,}(/[^\s]*)?""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        return urlPattern.findAll(text)
            .map { it.value.trim() }
            .distinct()
            .toList()
    }
}