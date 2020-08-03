package com.ivanovsky.passnotes

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MockTest {

    @Test
    fun `should return three strings`() {
        val resources: ResourceProvider = mockk()
        every { resources.getString(1) }.returns(FIRST)
        every { resources.getString(2) }.returns(SECOND)
        every { resources.getString(3) }.returns(THIRD)

        val consumer = ResourceConsumer(resources)
        consumer.doSomething()

        verify { resources.getString(1) }
        verify { resources.getString(2) }
        verify { resources.getString(3) }
        assertThat(consumer.first).isEqualTo(FIRST)
        assertThat(consumer.second).isEqualTo(SECOND)
        assertThat(consumer.third).isEqualTo(THIRD)
    }

    @Test
    fun `should return two strings`() {
        val resources: ResourceProvider = mockk()
        every { resources.getString(1) }.returns(FIRST)
        every { resources.getString(2) }.returns(SECOND)
        every { resources.getString(3) }.returns(THIRD)

        val consumer = ResourceConsumer(resources)
        consumer.doSomething()

        assertThat(consumer.first).isEqualTo(FIRST)
        assertThat(consumer.second).isEqualTo(SECOND)
    }

    companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val THIRD = "third"
    }
}

class ResourceProvider {

    fun getString(id: Int): String {
        return ""
    }

    fun getString(id: Int, vararg args: Any): String {
        return ""
    }
}

class ResourceConsumer(private val resources: ResourceProvider) {

    lateinit var first: String
    lateinit var second: String
    lateinit var third: String

    fun doSomething() {
        first = resources.getString(1)
        second = resources.getString(2)
        third = resources.getString(3)
    }
}