package com.example.medassist.data.model

/** Structured, temporary output from the AI report explanation. It is not automatically persisted. */
data class ReportAnalysis(
    val overview: String,
    val importantFindings: List<String>,
    val medicalTerms: List<String>,
    val testValues: List<ReportTestValue>,
    val questionsForDoctor: List<String>,
    val limitations: List<String>,
)

/** Values the AI reports as appearing in the provided PDF text; always verify these against the source. */
data class ReportTestValue(
    val testName: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    val reportFlag: String,
)
