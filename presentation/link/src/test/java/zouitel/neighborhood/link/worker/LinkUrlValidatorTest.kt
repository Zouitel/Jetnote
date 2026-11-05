package zouitel.neighborhood.link.worker

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkUrlValidatorTest {

    @Test
    fun acceptsHttpAndHttpsUrls() {
        assertEquals("https", LinkUrlValidator.parseHttpUri("example.com/path").scheme)
        assertEquals("http", LinkUrlValidator.parseHttpUri("http://example.com").scheme)
        assertEquals("https", LinkUrlValidator.parseHttpUri("https://example.com").scheme)
    }

    @Test
    fun rejectsUnsupportedSchemesAndCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            LinkUrlValidator.parseHttpUri("file:///etc/passwd")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LinkUrlValidator.parseHttpUri("ftp://example.com/file")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LinkUrlValidator.parseHttpUri("https://user:password@example.com")
        }
    }

    @Test
    fun rejectsPrivateAndReservedIpv4Addresses() {
        listOf(
            "0.0.0.0",
            "10.0.0.1",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.1.1",
            "172.16.0.1",
            "192.168.1.1",
            "192.0.2.1",
            "198.18.0.1",
            "203.0.113.1",
            "224.0.0.1"
        ).forEach { ip ->
            assertFalse("$ip must be blocked", LinkUrlValidator.isPublicAddress(InetAddress.getByName(ip)))
        }
    }

    @Test
    fun rejectsLoopbackAndDocumentationIpv6Addresses() {
        listOf("::1", "fc00::1", "fe80::1", "2001:db8::1").forEach { ip ->
            assertFalse("$ip must be blocked", LinkUrlValidator.isPublicAddress(InetAddress.getByName(ip)))
        }
    }

    @Test
    fun acceptsPublicAddresses() {
        assertTrue(LinkUrlValidator.isPublicAddress(InetAddress.getByName("1.1.1.1")))
        assertTrue(LinkUrlValidator.isPublicAddress(InetAddress.getByName("2606:4700:4700::1111")))
    }
}
