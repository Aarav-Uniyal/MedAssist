package com.example.medassist.domain

import com.example.medassist.domain.vault.PdfChunks
import com.example.medassist.domain.vault.VaultConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PdfChunksTest {
    @Test fun chunkCount_roundsUp() {
        assertEquals(0, PdfChunks.chunkCount(0, 10))
        assertEquals(1, PdfChunks.chunkCount(1, 10))
        assertEquals(1, PdfChunks.chunkCount(10, 10))
        assertEquals(2, PdfChunks.chunkCount(11, 10))
    }

    @Test fun maxFile_fitsRulesLimitOf12Pieces() =
        assertEquals(12, PdfChunks.chunkCount(VaultConfig.MAX_FILE_BYTES))

    @Test fun pieceSize_fitsInFirestoreDocument() = assertTrue(VaultConfig.CHUNK_BYTES < 1_048_576 - 10_000)

    @Test fun splitThenJoin_givesOriginalBytes() {
        val bytes = Random(1).nextBytes(25)
        val pieces = PdfChunks.split(bytes, 10)
        assertEquals(listOf(10, 10, 5), pieces.map { it.size })
        assertArrayEquals(bytes, pieces.reduce { a, b -> a + b })
    }

    @Test fun chunkIds_matchRulesFormat() =
        assertEquals(listOf("abc_0", "abc_1"), PdfChunks.chunkIds("abc", 2))

    @Test fun sha256_knownValue() = assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        PdfChunks.sha256Hex("abc".toByteArray()),
    )
}
