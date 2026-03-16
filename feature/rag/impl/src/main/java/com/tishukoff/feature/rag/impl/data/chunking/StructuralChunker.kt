package com.tishukoff.feature.rag.impl.data.chunking

internal class StructuralChunker(
    private val maxChunkSize: Int = 1500,
) {

    fun chunk(text: String, fileName: String): List<ChunkResult> {
        val sections = splitBySections(text)
        val chunks = mutableListOf<ChunkResult>()
        var index = 0

        for (section in sections) {
            if (section.content.isBlank()) continue

            if (section.content.length <= maxChunkSize) {
                chunks.add(
                    ChunkResult(
                        text = section.content.trim(),
                        section = section.heading,
                        chunkIndex = index,
                    )
                )
                index++
            } else {
                val subChunks = splitLargeSection(section.content, section.heading, index)
                chunks.addAll(subChunks)
                index += subChunks.size
            }
        }

        return chunks
    }

    private fun splitBySections(text: String): List<Section> {
        val sections = mutableListOf<Section>()
        val lines = text.lines()
        var currentHeading = "intro"
        val currentContent = StringBuilder()

        for (line in lines) {
            if (line.startsWith("## ") || line.startsWith("# ")) {
                if (currentContent.isNotBlank()) {
                    sections.add(Section(currentHeading, currentContent.toString()))
                    currentContent.clear()
                }
                currentHeading = line.removePrefix("###").removePrefix("##").removePrefix("#").trim()
            }
            currentContent.appendLine(line)
        }

        if (currentContent.isNotBlank()) {
            sections.add(Section(currentHeading, currentContent.toString()))
        }

        return sections
    }

    private fun splitLargeSection(
        content: String,
        heading: String,
        startIndex: Int,
    ): List<ChunkResult> {
        val chunks = mutableListOf<ChunkResult>()
        val subSections = content.split(Regex("(?=\n### )"))
        var index = startIndex

        for (sub in subSections) {
            val trimmed = sub.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.length <= maxChunkSize) {
                chunks.add(
                    ChunkResult(
                        text = trimmed,
                        section = heading,
                        chunkIndex = index,
                    )
                )
                index++
            } else {
                val paragraphs = trimmed.split("\n\n")
                val buffer = StringBuilder()
                for (paragraph in paragraphs) {
                    if (buffer.length + paragraph.length > maxChunkSize && buffer.isNotEmpty()) {
                        chunks.add(
                            ChunkResult(
                                text = buffer.toString().trim(),
                                section = heading,
                                chunkIndex = index,
                            )
                        )
                        index++
                        buffer.clear()
                    }
                    buffer.appendLine(paragraph).appendLine()
                }
                if (buffer.isNotBlank()) {
                    chunks.add(
                        ChunkResult(
                            text = buffer.toString().trim(),
                            section = heading,
                            chunkIndex = index,
                        )
                    )
                    index++
                }
            }
        }

        return chunks
    }

    private data class Section(
        val heading: String,
        val content: String,
    )
}
