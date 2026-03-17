package com.tishukoff.feature.rag.impl.data.local

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object EmbeddingConverter {

    fun toByteArray(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (value in embedding) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }
}
