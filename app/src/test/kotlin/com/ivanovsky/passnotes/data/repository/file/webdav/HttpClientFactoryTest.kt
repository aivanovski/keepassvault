package com.ivanovsky.passnotes.data.repository.file.webdav

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpClientFactoryTest {

    @Test
    fun `filterEnabledProtocols removes SSL and legacy TLS protocols while retaining TLSv1 2 and TLSv1 3`() {
        val filteredProtocols = HttpClientFactory.filterEnabledProtocols(
            arrayOf(
                "SSL",
                "SSLv2",
                "SSLv2Hello",
                "SSLv3",
                "TLSv1",
                "TLSv1.0",
                "TLSv1.1",
                "TLSv1.2",
                "TLSv1.3"
            )
        )

        assertEquals(listOf("TLSv1.2", "TLSv1.3"), filteredProtocols.toList())
    }

    @Test
    fun `filterEnabledProtocols retains an unknown future TLS protocol`() {
        val filteredProtocols = HttpClientFactory.filterEnabledProtocols(
            arrayOf("TLSv1.4")
        )

        assertEquals(listOf("TLSv1.4"), filteredProtocols.toList())
    }

    @Test(expected = IllegalStateException::class)
    fun `filterEnabledProtocols fails closed when only legacy protocols are enabled`() {
        HttpClientFactory.filterEnabledProtocols(
            arrayOf(
                "SSL",
                "SSLv2",
                "SSLv2Hello",
                "SSLv3",
                "TLSv1",
                "TLSv1.0",
                "TLSv1.1"
            )
        )
    }
}
