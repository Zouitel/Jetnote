package zouitel.neighborhood.link.worker

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * URL policy shared by link preview fetching and unit tests.
 *
 * DNS checks reduce SSRF exposure but do not pin the resolved address to the
 * subsequent Jsoup socket connection; DNS rebinding remains a residual risk.
 */
internal object LinkUrlValidator {

    fun parseHttpUri(value: String): URI {
        val hasExplicitScheme = EXPLICIT_SCHEME.containsMatchIn(value) && !HOST_AND_PORT.matches(value)
        val normalized = if (hasExplicitScheme) {
            value
        } else if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            value
        } else {
            "https://$value"
        }

        val uri = try {
            URI(normalized).normalize()
        } catch (failure: Exception) {
            throw IllegalArgumentException("Malformed URL", failure)
        }

        if (uri.scheme?.lowercase() !in listOf("http", "https") ||
            uri.host.isNullOrBlank() || uri.userInfo != null ||
            uri.port !in -1..65535
        ) {
            throw IllegalArgumentException("Unsupported URL")
        }
        return uri
    }

    fun validatePublicDestination(uri: URI) {
        val addresses = InetAddress.getAllByName(uri.host)
        require(addresses.isNotEmpty() && addresses.all(::isPublicAddress)) {
            "Non-public network destinations are not allowed"
        }
    }

    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) return false

        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val a = bytes[0].toInt() and 0xff
                val b = bytes[1].toInt() and 0xff
                val c = bytes[2].toInt() and 0xff
                !(a == 0 || a == 10 || a == 127 || a >= 224 ||
                    (a == 100 && b in 64..127) ||
                    (a == 169 && b == 254) ||
                    (a == 172 && b in 16..31) ||
                    (a == 192 && (b == 0 || b == 168)) ||
                    (a == 192 && b == 88 && c == 99) ||
                    (a == 198 && b in 18..19) ||
                    (a == 198 && b == 51 && c == 100) ||
                    (a == 203 && b == 0 && c == 113))
            }
            is Inet6Address -> {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                !(first and 0xfe == 0xfc ||
                    first == 0xfe && second and 0xc0 == 0x80 ||
                    first == 0x20 && second == 0x01 &&
                    (bytes[2].toInt() and 0xff) == 0x0d &&
                    (bytes[3].toInt() and 0xff) == 0xb8)
            }
            else -> false
        }
    }

    private val EXPLICIT_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val HOST_AND_PORT = Regex("^[^/:?#]+:\\d+(?:[/?#].*)?$")
}
