package com.thelastpickle.tlpcluster.commands.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class AZConverterTest {

    val converter = AZConverter()

    val abc = listOf("a", "b", "c")

    @Test
    fun testNoBreak() {
        assertThat(converter.convert("abc")).isEqualTo(abc)
    }

    @Test
    fun testEmpty() {
        assertThat(converter.convert("")).isEmpty()
    }

    @Test
    fun testCommas() {
        assertThat(converter.convert("a,b,c")).isEqualTo(abc)
    }

    @Test
    fun testStupidMix() {
        assertThat(converter.convert("a    b    , c")).isEqualTo(abc)
    }
}