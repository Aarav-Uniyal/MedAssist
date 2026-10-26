package com.example.medassist.domain.vault

import java.security.MessageDigest

/** Pure helpers for splitting a PDF into Firestore-sized pieces. No Android or Firebase here, so it is unit-tested. */
object PdfChunks {

    fun chunkCount(sizeBytes: Long, chunkBytes: Int = VaultConfig.CHUNK_BYTES): Int {
        require(sizeBytes >= 0 && chunkBytes > 0)
        return ((sizeBytes + chunkBytes - 1) / chunkBytes).toInt()
    }

    fun split(bytes: ByteArray, chunkBytes: Int = VaultConfig.CHUNK_BYTES): List<ByteArray> =
        (0 until chunkCount(bytes.size.toLong(), chunkBytes)).map { index ->
            val start = index * chunkBytes
            bytes.copyOfRange(start, minOf(start + chunkBytes, bytes.size))
        }

    /** Chunk documents are named "<documentId>_<index>", so they can be found without a query. */
    fun chunkId(documentId: String, index: Int): String = "${documentId}_$index"

    fun chunkIds(documentId: String, chunkCount: Int): List<String> =
        (0 until chunkCount).map { chunkId(documentId, it) }

    fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
