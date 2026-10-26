package com.example.medassist.data.repository

import com.example.medassist.data.model.ReportAnalysis
import com.example.medassist.domain.assistant.AppLanguage

sealed interface ReportAnalysisResult {
    data class Success(val analysis: ReportAnalysis) : ReportAnalysisResult
    data class Failure(val error: AssistantError) : ReportAnalysisResult
}

/** Sends only extracted PDF text to Gemini; it does not persist report text or analysis output. */
interface ReportAnalyzerRepository {
    suspend fun analyze(reportText: String, language: AppLanguage): ReportAnalysisResult
}
