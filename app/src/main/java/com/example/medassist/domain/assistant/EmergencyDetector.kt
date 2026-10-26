package com.example.medassist.domain.assistant

import java.util.Locale

/**
 * A simple word check that shows the "call 112" banner straight away, before the AI replies.
 * It is a safety net only: the system instruction also tells the AI to point to emergency services.
 */
object EmergencyDetector {
    private val phrases = listOf(
        // English
        "chest pain", "pain in my chest", "heart attack", "can't breathe", "cannot breathe", "can not breathe",
        "not breathing", "trouble breathing", "difficulty breathing", "short of breath", "choking",
        "unconscious", "passed out", "fainted", "stroke", "face drooping", "seizure", "fits",
        "severe bleeding", "heavy bleeding", "bleeding a lot", "overdose", "poisoned", "poisoning",
        "suicide", "suicidal", "kill myself", "end my life", "self harm", "self-harm", "hurt myself",
        // Hindi
        "सीने में दर्द", "छाती में दर्द", "दिल का दौरा", "हार्ट अटैक", "सांस नहीं", "साँस नहीं", "सांस लेने में तकलीफ",
        "साँस लेने में तकलीफ", "बेहोश", "लकवा", "दौरा पड़", "बहुत खून", "ज़हर", "जहर", "आत्महत्या",
        "खुद को मार", "अपनी जान", "खुद को नुकसान",
    )

    fun looksUrgent(text: String): Boolean {
        val normalized = text.lowercase(Locale.ROOT).replace('’', '\'').replace(Regex("\\s+"), " ")
        return phrases.any { normalized.contains(it) }
    }
}
