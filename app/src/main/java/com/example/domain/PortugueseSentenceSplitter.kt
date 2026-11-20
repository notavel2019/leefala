package com.example.domain

/**
 * Splits Brazilian Portuguese scripts into speakable phrases while preserving words,
 * Portuguese diacritics, punctuation, and exact order.
 *
 * Avoids false splits on common Portuguese abbreviations, decimal/grouped numbers,
 * and Biblical references (e.g., "Jo 3:16", "1 Co 13.4", "Sl. 23:1", "cap. 5", "v. 12").
 */
object PortugueseSentenceSplitter {

    private val nonBreakingAbbreviations: Set<String> = setOf(
        // Títulos e abreviações comuns em português
        "sr", "sra", "srs", "sras", "dr", "dra", "drs", "dras",
        "prof", "profa", "profs", "av", "r", "st", "sto", "sta",
        "etc", "pag", "pág", "p", "pp", "cap", "caps", "v", "vv", "vs",
        "ex", "obs", "tel", "art", "arts", "inc", "par", "n", "no", "nº",
        "jr", "ltda", "cia", "vol", "ed", "rev", "trad", "ref",
        "seg", "ter", "qua", "qui", "sex", "sab", "sáb", "dom",
        "jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez",
        "a.c", "d.c", "s.a",
        // Abreviações bíblicas em português
        "gn", "ex", "êx", "lv", "nm", "dt", "js", "jz", "rt", "sm", "rs", "cr",
        "ed", "ne", "et", "jó", "sl", "pv", "ec", "ct", "is", "jr", "lm", "ez", "dn",
        "os", "jl", "am", "ob", "jn", "mq", "na", "hc", "sf", "ag", "zc", "ml",
        "mt", "mc", "lc", "jo", "at", "rm", "co", "gl", "ef", "fp", "cl",
        "ts", "tm", "tt", "fm", "hb", "tg", "pe", "jd", "ap",
    )

    fun split(rawText: String): List<String> {
        val normalized = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val paragraphs = normalized.split(Regex("\n+"))
        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isEmpty()) continue
            result.addAll(splitParagraph(trimmedParagraph))
        }
        return result
    }

    private fun splitParagraph(paragraph: String): List<String> {
        val sentences = mutableListOf<String>()
        val length = paragraph.length
        var start = 0
        var i = 0

        while (i < length) {
            val ch = paragraph[i]
            if (ch == '.' || ch == '!' || ch == '?') {
                // Consume consecutive punctuation like "...", "?!", "!!"
                var punctEnd = i
                while (punctEnd + 1 < length && paragraph[punctEnd + 1] in charArrayOf('.', '!', '?')) {
                    punctEnd++
                }
                val isEllipsis = ch == '.' && (punctEnd - i) >= 2

                // Include closing quotes/brackets right after sentence punctuation
                var closeEnd = punctEnd
                while (closeEnd + 1 < length && paragraph[closeEnd + 1] in charArrayOf('"', '\'', '”', '’', ')', ']', '»')) {
                    closeEnd++
                }

                val nextIdx = closeEnd + 1
                val atEnd = nextIdx >= length
                val followedByWhitespace = !atEnd && paragraph[nextIdx].isWhitespace()

                val shouldSplit = when {
                    atEnd -> true
                    !followedByWhitespace -> false
                    ch == '!' || ch == '?' -> true
                    isEllipsis -> {
                        val nextNonSpace = findNextNonWhitespace(paragraph, nextIdx)
                        nextNonSpace == null || nextNonSpace.isUpperCase() || nextNonSpace in charArrayOf('"', '“', '—', '-')
                    }
                    else -> {
                        // Single dot followed by whitespace: check abbreviations, numbers, initials
                        !isNonBreakingDot(paragraph, i, nextIdx)
                    }
                }

                if (shouldSplit) {
                    val piece = paragraph.substring(start, nextIdx).trim()
                    if (piece.isNotEmpty()) {
                        sentences.add(piece)
                    }
                    // Advance start past whitespace
                    var nextStart = nextIdx
                    while (nextStart < length && paragraph[nextStart].isWhitespace()) {
                        nextStart++
                    }
                    start = nextStart
                    i = nextStart
                    continue
                } else {
                    i = closeEnd + 1
                    continue
                }
            }
            i++
        }

        if (start < length) {
            val tail = paragraph.substring(start).trim()
            if (tail.isNotEmpty()) {
                sentences.add(tail)
            }
        }
        return sentences
    }

    private fun isNonBreakingDot(text: String, dotIndex: Int, afterCloseIndex: Int): Boolean {
        val tokenBefore = extractTokenBefore(text, dotIndex)
        if (tokenBefore.isEmpty()) return false

        val lower = tokenBefore.lowercase()

        // Single letter initial like "J." or "M." followed by capitalized word or another initial
        if (tokenBefore.length == 1 && tokenBefore[0].isLetter()) {
            return true
        }

        // Pure digits before dot: e.g., "1. Introdução" or "cap. 3. 16" or ordinal list
        if (tokenBefore.all { it.isDigit() }) {
            val nextNonSpace = findNextNonWhitespace(text, afterCloseIndex)
            if (nextNonSpace != null && nextNonSpace.isDigit()) {
                return true
            }
        }

        // Known Portuguese / Biblical abbreviation
        if (lower in nonBreakingAbbreviations) {
            return true
        }

        // Multi-part abbreviations like "a.C." or "d.C." or "S.A."
        if (lower.contains('.') && lower.length <= 5) {
            return true
        }

        return false
    }

    private fun extractTokenBefore(text: String, dotIndex: Int): String {
        var idx = dotIndex - 1
        while (idx >= 0) {
            val c = text[idx]
            if (c.isLetterOrDigit() || c == '.' || c == 'º' || c == 'ª') {
                idx--
            } else {
                break
            }
        }
        return text.substring(idx + 1, dotIndex)
    }

    private fun findNextNonWhitespace(text: String, fromIndex: Int): Char? {
        var idx = fromIndex
        while (idx < text.length) {
            if (!text[idx].isWhitespace()) return text[idx]
            idx++
        }
        return null
    }

    /**
     * Merges sentence at [index] with sentence at [index + 1], preserving exact text.
     */
    fun mergeWithNext(sentences: List<String>, index: Int): List<String> {
        if (index < 0 || index >= sentences.lastIndex) return sentences
        val mutable = sentences.toMutableList()
        val combined = "${mutable[index].trimEnd()} ${mutable[index + 1].trimStart()}".trim()
        mutable[index] = combined
        mutable.removeAt(index + 1)
        return mutable
    }

    /**
     * Splits sentence at [index] at the given word boundary [wordSplitIndex] (1..words.size-1).
     */
    fun splitSentenceAtWord(sentences: List<String>, index: Int, wordSplitIndex: Int): List<String> {
        if (index !in sentences.indices) return sentences
        val words = sentences[index].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (wordSplitIndex !in 1 until words.size) return sentences
        val firstPart = words.subList(0, wordSplitIndex).joinToString(" ")
        val secondPart = words.subList(wordSplitIndex, words.size).joinToString(" ")
        val mutable = sentences.toMutableList()
        mutable[index] = firstPart
        mutable.add(index + 1, secondPart)
        return mutable
    }
}
