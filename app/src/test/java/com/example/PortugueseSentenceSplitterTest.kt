package com.example

import com.example.domain.PortugueseSentenceSplitter
import org.junit.Assert.assertEquals
import org.junit.Test

class PortugueseSentenceSplitterTest {

    @Test
    fun preservesPortugueseAccentsAbbreviationsAndBiblicalReferences() {
        val text = "Olá, Sr. João! Hoje vamos meditar em Sl. 23:1 e 1 Co 13.4 com atenção. " +
            "O valor chegou a R$ 1.500,50 no cap. 3, v. 16. Que bênção!"

        val result = PortugueseSentenceSplitter.split(text)

        assertEquals(
            listOf(
                "Olá, Sr. João!",
                "Hoje vamos meditar em Sl. 23:1 e 1 Co 13.4 com atenção.",
                "O valor chegou a R$ 1.500,50 no cap. 3, v. 16.",
                "Que bênção!",
            ),
            result,
        )
    }

    @Test
    fun splitsParagraphsAndManualMergeSplitRoundTrip() {
        val text = "Primeira linha sem ponto final\n\nSegunda frase completa. Terceira frase aqui."
        val initial = PortugueseSentenceSplitter.split(text)
        assertEquals(3, initial.size)

        val merged = PortugueseSentenceSplitter.mergeWithNext(initial, 0)
        assertEquals(2, merged.size)
        assertEquals("Primeira linha sem ponto final Segunda frase completa.", merged[0])

        val splitBack = PortugueseSentenceSplitter.splitSentenceAtWord(merged, 0, 5)
        assertEquals(3, splitBack.size)
        assertEquals("Primeira linha sem ponto final", splitBack[0])
        assertEquals("Segunda frase completa.", splitBack[1])
    }
}
