package com.example.medassist.data.repository

import android.util.Log
import com.example.medassist.data.model.ReportAnalysis
import com.example.medassist.data.model.ReportTestValue
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.domain.assistant.ReportAnalysisPrompt
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Uses the same Firebase AI Logic + Remote Config model selection as the Phase 4 assistant. */
class FirebaseReportAnalyzerRepository(
    private val modelName: suspend () -> String,
) : ReportAnalyzerRepository {

    private val lock = Mutex()
    private var cachedKey: String? = null
    private var cachedModel: GenerativeModel? = null

    override suspend fun analyze(reportText: String, language: AppLanguage): ReportAnalysisResult {
        if (reportText.isBlank()) return ReportAnalysisResult.Failure(AssistantError.EMPTY_REPLY)

        return try {
            val name = modelName()
            val model = modelFor(name, language)
            val response = model.generateContent(ReportAnalysisPrompt.userPrompt(reportText)).text
            val analysis = response?.let { ReportAnalysisJsonParser.parse(it) }
            if (analysis == null) {
                ReportAnalysisResult.Failure(AssistantError.EMPTY_REPLY)
            } else {
                ReportAnalysisResult.Success(analysis)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Report analysis timed out")
            ReportAnalysisResult.Failure(AssistantError.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never log report text or any report values; health information is sensitive.
            Log.w(TAG, "Report analysis failed: ${e.javaClass.simpleName}")
            ReportAnalysisResult.Failure(assistantErrorFor(e))
        }
    }

    private suspend fun modelFor(name: String, language: AppLanguage): GenerativeModel = lock.withLock {
        val key = "$name|${language.tag}|report-json"
        cachedModel?.takeIf { cachedKey == key } ?: Firebase.ai(
            backend = GenerativeBackend.googleAI(),
        ).generativeModel(
            modelName = name,
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = MAX_OUTPUT_TOKENS
                responseMimeType = "application/json"
                responseSchema = reportSchema()
                thinkingConfig = thinkingConfig {
                    thinkingLevel = if (name.endsWith("-flash-lite")) ThinkingLevel.MINIMAL else ThinkingLevel.LOW
                }
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
            ),
            systemInstruction = content { text(ReportAnalysisPrompt.systemInstruction(language)) },
            requestOptions = RequestOptions(timeoutInMillis = AssistantConfig.REQUEST_TIMEOUT_MILLIS),
        ).also {
            cachedKey = key
            cachedModel = it
        }
    }

    private fun reportSchema(): Schema {
        val testValue = Schema.obj(
            mapOf(
                "test_name" to Schema.string(),
                "value" to Schema.string(),
                "unit" to Schema.string(),
                "reference_range" to Schema.string(),
                "report_flag" to Schema.string(),
            ),
        )
        return Schema.obj(
            mapOf(
                "overview" to Schema.string(),
                "important_findings" to Schema.array(Schema.string()),
                "medical_terms" to Schema.array(Schema.string()),
                "test_values" to Schema.array(testValue),
                "questions_for_doctor" to Schema.array(Schema.string()),
                "limitations" to Schema.array(Schema.string()),
            ),
        )
    }

    private companion object {
        const val TAG = "MedAssistReportAI"
        const val MAX_OUTPUT_TOKENS = 2048
    }
}

/** Defensive JSON parsing: schema-constrained output is still treated as untrusted model output. */
private object ReportAnalysisJsonParser {
    fun parse(raw: String): ReportAnalysis? = runCatching {
        val jsonText = raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(jsonText)
        val overview = root.safeString("overview", MAX_SECTION_CHARS)
        val important = root.stringList("important_findings")
        val terms = root.stringList("medical_terms")
        val questions = root.stringList("questions_for_doctor")
        val limitations = root.stringList("limitations")
        val testValues = root.optJSONArray("test_values")?.let { array ->
            (0 until minOf(array.length(), MAX_TEST_VALUES)).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val value = ReportTestValue(
                    testName = item.safeString("test_name", MAX_FIELD_CHARS),
                    value = item.safeString("value", MAX_FIELD_CHARS),
                    unit = item.safeString("unit", MAX_FIELD_CHARS),
                    referenceRange = item.safeString("reference_range", MAX_FIELD_CHARS),
                    reportFlag = item.safeString("report_flag", MAX_FIELD_CHARS),
                )
                value.takeIf {
                    it.testName.isNotBlank() || it.value.isNotBlank() || it.unit.isNotBlank() ||
                        it.referenceRange.isNotBlank() || it.reportFlag.isNotBlank()
                }
            }
        }.orEmpty()

        if (overview.isBlank() && important.isEmpty() && terms.isEmpty() && testValues.isEmpty() &&
            questions.isEmpty() && limitations.isEmpty()
        ) {
            null
        } else {
            ReportAnalysis(
                overview = overview,
                importantFindings = important,
                medicalTerms = terms,
                testValues = testValues,
                questionsForDoctor = questions,
                limitations = limitations,
            )
        }
    }.getOrNull()

    private fun JSONObject.safeString(key: String, limit: Int): String =
        optString(key, "").trim().take(limit)

    private fun JSONObject.stringList(key: String): List<String> = optJSONArray(key).asStringList()

    private fun JSONArray?.asStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until minOf(length(), MAX_LIST_ITEMS)).mapNotNull { index ->
            optString(index, "").trim().take(MAX_FIELD_CHARS).takeIf(String::isNotBlank)
        }
    }

    private const val MAX_SECTION_CHARS = 1800
    private const val MAX_FIELD_CHARS = 500
    private const val MAX_LIST_ITEMS = 10
    private const val MAX_TEST_VALUES = 20
}
