package zouitel.neighborhood.link.worker

import android.content.Context
import androidx.compose.ui.util.fastForEach
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.LinkUseCase
import zouitel.neighborhood.link.mapper.LinkMapper
import zouitel.neighborhood.link.model.Link
import zouitel.neighborhood.link.utils.Constants.UID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

class LinkWorker(
    context: Context,
    workerParameters: WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher,
    private val insert: LinkUseCase.Insert,
    private val linkMapper: LinkMapper
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = withContext(ioDeprecated) {
        val uid = inputData.getString(UID)?.trim().orEmpty()
        val text = inputData.getString(TEXT).orEmpty()

        if (uid.isBlank() || text.isBlank() || text.length > MAX_TEXT_LENGTH) {
            return@withContext Result.failure()
        }

        val urls = extractUrls(text)
        if (urls.isEmpty()) return@withContext Result.success()

        val links = mutableListOf<Link>()
        var retryableFailure = false

        for (url in urls) {
            try {
                links += fetchLinkMetadata(url)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: LinkFetchException) {
                retryableFailure = retryableFailure || failure.retryable
            } catch (_: IOException) {
                retryableFailure = true
            } catch (_: Exception) {
                // An invalid or unsupported URL must not prevent other previews being saved.
            }
        }

        if (links.isEmpty()) {
            return@withContext if (retryableFailure) Result.retry() else Result.failure()
        }

        try {
            links.fastForEach { link ->
                insert.invoke(linkMapper.toDomain(link.copy(uid = uid)))
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    suspend fun fetchLinkMetadata(urlString: String): Link = withContext(ioDeprecated) {
        val document = fetchDocument(urlString)
        val finalUri = URI(document.location()).normalize()
        val domain = finalUri.host.removePrefix("www.")

        val title = document.select("meta[property=og:title]").attr("content")
            .ifEmpty { document.select("meta[name=twitter:title]").attr("content") }
            .ifEmpty { document.title() }
            .take(MAX_METADATA_LENGTH)

        val description = document.select("meta[property=og:description]").attr("content")
            .ifEmpty { document.select("meta[name=twitter:description]").attr("content") }
            .ifEmpty { document.select("meta[name=description]").attr("content") }
            .take(MAX_METADATA_LENGTH)

        val imageUrl = document.select("meta[property=og:image]").attr("abs:content")
            .ifEmpty { document.select("meta[name=twitter:image]").attr("abs:content") }
            .take(MAX_URL_LENGTH)

        val iconUrl = document.select("link[rel~=icon]").attr("abs:href")
            .ifEmpty { document.select("link[rel~=shortcut icon]").attr("abs:href") }
            .ifEmpty { finalUri.resolve("/favicon.ico").toString() }
            .take(MAX_URL_LENGTH)

        Link(
            url = finalUri.toString(),
            title = title,
            description = description,
            image = imageUrl,
            icon = iconUrl,
            domain = domain
        )
    }

    fun extractUrls(text: String): List<String> {
        if (text.length > MAX_TEXT_LENGTH) return emptyList()

        val urlPattern = Regex(
            pattern = """(https?://[^\\s]+)|([a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?\\.)+[a-zA-Z]{2,}(/[^\\s]*)?""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        return urlPattern.findAll(text)
            .map { it.value.trim().trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}') }
            .filter { it.isNotBlank() && it.length <= MAX_URL_LENGTH }
            .distinct()
            .take(MAX_URL_COUNT)
            .toList()
    }

    private fun fetchDocument(rawUrl: String): Document {
        var uri = parseHttpUri(rawUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            ensurePublicDestination(uri)
            val response = try {
                Jsoup.connect(uri.toString())
                    .userAgent(USER_AGENT)
                    .timeout(REQUEST_TIMEOUT_MS)
                    .maxBodySize(MAX_RESPONSE_BYTES)
                    .followRedirects(false)
                    .execute()
            } catch (failure: IOException) {
                throw LinkFetchException("Network request failed", retryable = true, cause = failure)
            }

            val status = response.statusCode()
            if (status in 300..399) {
                if (redirectCount == MAX_REDIRECTS) {
                    throw LinkFetchException("Too many redirects", retryable = false)
                }
                val location = response.header("Location")
                    ?: throw LinkFetchException("Redirect without a location", retryable = false)
                uri = parseHttpUri(uri.resolve(location).toString())
                return@repeat
            }
            if (status !in 200..299) {
                throw LinkFetchException(
                    "HTTP $status",
                    retryable = status == 429 || status >= 500
                )
            }
            return response.parse()
        }
        throw LinkFetchException("Unable to fetch URL", retryable = false)
    }

    private fun parseHttpUri(value: String): URI {
        val normalized = if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
            value
        } else {
            "https://$value"
        }
        val uri = try {
            URI(normalized).normalize()
        } catch (failure: Exception) {
            throw LinkFetchException("Malformed URL", retryable = false, cause = failure)
        }
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank() ||
            uri.userInfo != null || uri.port !in -1..65535
        ) {
            throw LinkFetchException("Unsupported URL", retryable = false)
        }
        return uri
    }

    private fun ensurePublicDestination(uri: URI) {
        val addresses = try {
            InetAddress.getAllByName(uri.host)
        } catch (failure: IOException) {
            throw LinkFetchException("DNS lookup failed", retryable = true, cause = failure)
        }
        if (addresses.isEmpty() || addresses.any { !it.isPublicAddress() }) {
            throw LinkFetchException("Non-public network destinations are not allowed", retryable = false)
        }
    }

    private fun InetAddress.isPublicAddress(): Boolean {
        if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
            isSiteLocalAddress || isMulticastAddress
        ) return false

        val bytes = address
        return when (this) {
            is Inet4Address -> {
                val a = bytes[0].toInt() and 0xff
                val b = bytes[1].toInt() and 0xff
                val c = bytes[2].toInt() and 0xff
                !(a == 0 || a == 10 || a == 127 || a >= 224 ||
                    (a == 100 && b in 64..127) ||
                    (a == 169 && b == 254) ||
                    (a == 172 && b in 16..31) ||
                    (a == 192 && (b == 0 || b == 168)) ||
                    (a == 198 && b in 18..19) ||
                    (a == 198 && b == 51 && c == 100) ||
                    (a == 203 && b == 0 && c == 113))
            }
            is Inet6Address -> {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                // Unique-local (fc00::/7), link-local (fe80::/10), and documentation (2001:db8::/32).
                !(first and 0xfe == 0xfc ||
                    first == 0xfe && second and 0xc0 == 0x80 ||
                    first == 0x20 && second == 0x01 &&
                    bytes[2].toInt() == 0x0d && bytes[3].toInt() == 0xb8)
            }
            else -> false
        }
    }

    private class LinkFetchException(
        message: String,
        val retryable: Boolean,
        cause: Throwable? = null
    ) : Exception(message, cause)

    private companion object {
        const val TEXT = "text"
        const val MAX_TEXT_LENGTH = 20_000
        const val MAX_URL_LENGTH = 2_048
        const val MAX_URL_COUNT = 20
        const val MAX_METADATA_LENGTH = 2_000
        const val MAX_RESPONSE_BYTES = 1_048_576
        const val MAX_REDIRECTS = 5
        const val REQUEST_TIMEOUT_MS = 10_000
        const val USER_AGENT = "Mozilla/5.0 (compatible; JetnoteLinkPreview/1.0)"
    }
}
