package com.example

import com.example.audio.NarratorProsodyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarratorProsodyFormatterTest {

    @Test
    fun `smooths all-caps words while preserving real acronyms`() {
        val input = "ATENÇÃO: informe seu CPF e PIX com calma."
        val result = NarratorProsodyFormatter.formatForNarrator(input, naturalProsody = true)
        assertTrue(result.contains("atenção"))
        assertTrue(result.contains("CPF"))
        assertTrue(result.contains("PIX"))
    }

    @Test
    fun `expands common abbreviations and turns dashes into breathing pauses`() {
        val input = "Dr. João — ex.: cap. 3; pág. 12..."
        val result = NarratorProsodyFormatter.formatForNarrator(input, naturalProsody = true)
        assertTrue(result.contains("doutor João"))
        assertTrue(result.contains("por exemplo"))
        assertTrue(result.contains("capítulo 3"))
        assertTrue(result.contains("página 12"))
    }

    @Test
    fun `passthrough when natural prosody disabled`() {
        val input = "ATENÇÃO — texto literal *sem* alteração."
        val result = NarratorProsodyFormatter.formatForNarrator(input, naturalProsody = false)
        assertEquals(input, result)
    }
}
