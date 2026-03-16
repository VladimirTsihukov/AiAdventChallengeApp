package com.tishukoff.feature.rag.impl.data.chunking

internal class FixedSizeChunker(
    private val chunkSize: Int = 1500,
    private val overlap: Int = 200,
) {

    fun chunk(text: String, fileName: String): List<ChunkResult> {
        val cleanText = text.trim()
        if (cleanText.length <= chunkSize) {
            return listOf(
                ChunkResult(
                    text = cleanText,
                    section = extractTitle(cleanText),
                    chunkIndex = 0,
                )
            )
        }

        val chunks = mutableListOf<ChunkResult>()
        var start = 0
        var index = 0

        while (start < cleanText.length) {
            val end = (start + chunkSize).coerceAtMost(cleanText.length)

            val chunkEnd = if (end < cleanText.length) {
                findBreakPoint(cleanText, start, end)
            } else {
                end
            }

            val chunkText = cleanText.substring(start, chunkEnd).trim()
            if (chunkText.isNotEmpty()) {
                chunks.add(
                    ChunkResult(
                        text = chunkText,
                        section = "chunk_$index",
                        chunkIndex = index,
                    )
                )
                index++
            }

            val advance = chunkEnd - overlap
            start = advance.coerceAtLeast(start + chunkSize / 2)
            if (start >= cleanText.length) break
        }

        return chunks
    }

    private fun findBreakPoint(text: String, start: Int, end: Int): Int {
        val searchFrom = start + chunkSize * 2 / 3
        val lastDoubleNewline = text.lastIndexOf("\n\n", end)
        if (lastDoubleNewline > searchFrom) return lastDoubleNewline + 2

        val lastNewline = text.lastIndexOf('\n', end)
        if (lastNewline > searchFrom) return lastNewline + 1

        val lastPeriod = text.lastIndexOf(". ", end)
        if (lastPeriod > searchFrom) return lastPeriod + 2

        return end
    }

    private fun extractTitle(text: String): String {
        val firstLine = text.lineSequence().firstOrNull()?.trim() ?: return "unknown"
        return firstLine.removePrefix("#").trim().take(80)
    }
}

internal data class ChunkResult(
    val text: String,
    val section: String,
    val chunkIndex: Int,
)
