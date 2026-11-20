package com.example.audio

import java.util.Locale

/**
 * Prepares Portuguese (pt-BR) script text for smoother, more natural narrator-like TTS delivery
 * without modifying the visible script shown on screen.
 */
object NarratorProsodyFormatter {

    private val PT_BR = Locale("pt", "BR")

    private val PRESERVED_ACRONYMS = setOf(
        "CPF", "CNPJ", "SUS", "ONU", "EUA", "INSS", "FGTS", "PIX",
        "PDF", "APK", "HTML", "HTTP", "HTTPS", "WIFI", "GPS", "TTS",
        "HDMI", "USB", "CEO", "IBGE", "OAB", "CRM", "CTPS", "RG",
        "CNH", "IPVA", "IPTU", "CLT", "MEI", "CNPJ", "TV", "VIP",
    )

    private val ABBREVIATION_RULES = listOf(
        Regex("""\b[Ee]x\.:\s*""") to "por exemplo, ",
        Regex("""\b[Ee]x\.\s+""") to "por exemplo, ",
        Regex("""\b[Oo]bs\.:\s*""") to "observação, ",
        Regex("""\b[Oo]bs\.\s+""") to "observação, ",
        Regex("""\b[Ee]tc\.?""") to "etcétera",
        Regex("""\b[Cc]ap\.\s*(\d+)""") to "capítulo $1",
        Regex("""\b[Pp][áa]g\.\s*(\d+)""") to "página $1",
        Regex("""\b[Ss]r\.\s+""") to "senhor ",
        Regex("""\b[Ss]ra\.\s+""") to "senhora ",
        Regex("""\b[Dd]r\.\s+""") to "doutor ",
        Regex("""\b[Dd]ra\.\s+""") to "doutora ",
        Regex("""\b[Pp]rof\.\s+""") to "professor ",
    )

    private val ALL_CAPS_WORD = Regex("""\b[\p{Lu}]{4,}\b""")
    private val MARKDOWN_NOISE = Regex("""[*_`#~]+""")
    private val ELLIPSIS_REGEX = Regex("""\.{2,}|…""")
    private val DASH_PAUSE_REGEX = Regex("""\s*[—–]\s*|\s+-\s+""")
    private val SEMICOLON_REGEX = Regex("""\s*;\s*""")
    private val SLASH_BETWEEN_WORDS = Regex("""(\p{L})\s*/\s*(\p{L})""")
    private val MULTI_COMMA_REGEX = Regex("""(,\s*){2,}""")
    private val MULTI_SPACE_REGEX = Regex("""\s{2,}""")

    fun formatForNarrator(text: String, naturalProsody: Boolean): String {
        val trimmed = text.trim()
        if (!naturalProsody || trimmed.isEmpty()) return trimmed

        var result = trimmed

        // Remove markdown/formatting artifacts so TTS doesn't read symbols aloud
        result = MARKDOWN_NOISE.replace(result, "")

        // Expand common pt-BR abbreviations smoothly
        for ((pattern, replacement) in ABBREVIATION_RULES) {
            result = pattern.replace(result, replacement)
        }

        // Convert ALL-CAPS words (>= 4 letters) to lowercase so TTS doesn't spell them out
        result = ALL_CAPS_WORD.replace(result) { match ->
            val word = match.value
            if (word in PRESERVED_ACRONYMS) word else word.lowercase(PT_BR)
        }

        // Turn stiff separators into soft breathing pauses for natural intonation contour
        result = ELLIPSIS_REGEX.replace(result, ", ")
        result = DASH_PAUSE_REGEX.replace(result, ", ")
        result = SEMICOLON_REGEX.replace(result, ", ")
        result = SLASH_BETWEEN_WORDS.replace(result, "$1 ou $2")

        // Normalize parentheses into subtle breathing pauses
        result = result.replace("(", ", ").replace(")", ", ")

        // Clean redundant commas and whitespace
        result = MULTI_COMMA_REGEX.replace(result, ", ")
        result = result.replace(Regex("""\s+([,?!.])"""), "$1")
        result = result.replace(Regex("""^,\s*"""), "")
        result = result.replace(Regex(""",\s*([.?!])$"""), "$1")
        result = MULTI_SPACE_REGEX.replace(result, " ").trim()

        return result.ifEmpty { trimmed }
    }
}
