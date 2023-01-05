package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.domain.entity.ParsedUrl
import com.ivanovsky.passnotes.util.UrlUtils.SECRET_HOST_MASK
import com.ivanovsky.passnotes.util.UrlUtils.extractCleanWebDomain
import com.ivanovsky.passnotes.util.UrlUtils.formatSecretUrl
import com.ivanovsky.passnotes.util.UrlUtils.parseUrl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class UrlUtilsTest {

    @Test
    fun `extractCleanWebDomain should extract domain from url`() {
        assertThat(extractCleanWebDomain("https://domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("https://www.domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("http://domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("http://www.domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("www.domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("domain.com")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("https://www.domain.com/some.action")).isEqualTo("domain.com")
        assertThat(extractCleanWebDomain("https://www.domain.com/some.action?topic")).isEqualTo("domain.com")
    }

    @Test
    fun `extractCleanWebDomain should return null`() {
        assertThat(extractCleanWebDomain("domain")).isEqualTo(null)
    }

    @Test
    fun `parseUrl should parse url correctly`() {
        assertThat(parseUrl("domain.com/path"))
            .isEqualTo(ParsedUrl("https", "domain.com", "/path"))

        assertThat(parseUrl("https://domain.com/path"))
            .isEqualTo(ParsedUrl("https", "domain.com", "/path"))

        assertThat(parseUrl("https://www.domain.com/path"))
            .isEqualTo(ParsedUrl("https", "www.domain.com", "/path"))

        assertThat(parseUrl("http://domain.com/path"))
            .isEqualTo(ParsedUrl("http", "domain.com", "/path"))

        assertThat(parseUrl("https://oauth2:ABCD@domain.com/repo.git"))
            .isEqualTo(ParsedUrl("https", "oauth2:ABCD@domain.com", "/repo.git"))

        assertThat(parseUrl("domain.subdomain.com"))
            .isEqualTo(ParsedUrl("https", "domain.subdomain.com", ""))

        assertThat(parseUrl("https://domain.com:8080/path?argument=value#STATUS"))
            .isEqualTo(ParsedUrl("https", "domain.com:8080", "/path?argument=value#STATUS"))
    }

    @Test
    fun `parseUrl should return null`() {
        assertThat(parseUrl("domain"))
            .isEqualTo(null)

        assertThat(parseUrl("www."))
            .isEqualTo(null)
    }

    @Test
    fun `formatSecretUrl should replace host`() {
        assertThat(formatSecretUrl("https://domain.com/repo.git"))
            .isEqualTo("https://$SECRET_HOST_MASK/repo.git")

        assertThat(formatSecretUrl("https://oauth2:ABCD@gitlab.com/repo.git"))
            .isEqualTo("https://$SECRET_HOST_MASK/repo.git")
    }
}