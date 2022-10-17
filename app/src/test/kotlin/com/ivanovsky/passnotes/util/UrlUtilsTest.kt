package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.util.UrlUtils.extractWebDomain
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `extractWebDomain should extract domain from url`() {
        assertThat(extractWebDomain("https://domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("https://www.domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("http://domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("http://www.domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("www.domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("domain.com")).isEqualTo("domain")
        assertThat(extractWebDomain("domain")).isEqualTo("domain")
    }
}