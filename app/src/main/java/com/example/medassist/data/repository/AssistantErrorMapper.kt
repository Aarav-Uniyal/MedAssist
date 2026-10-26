package com.example.medassist.data.repository

import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PermissionMissingException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.SocketTimeoutException

/** Turns Firebase AI Logic / network exceptions into an [AssistantError] the screen can explain. */
fun assistantErrorFor(error: Throwable): AssistantError {
    val chain = generateSequence(error) { it.cause }.take(5).toList()
    val text = chain.joinToString(" ") { it.message.orEmpty() }.lowercase()
    return when {
        text.contains("app check") || text.contains("appcheck") || text.contains("app-check") -> AssistantError.APP_CHECK
        chain.any { it is QuotaExceededException } || text.contains("resource_exhausted") -> AssistantError.QUOTA
        chain.any { it is PromptBlockedException || it is ContentBlockedException || it is ResponseStoppedException } ->
            AssistantError.BLOCKED
        chain.any {
            it is ServiceDisabledException || it is APINotConfiguredException ||
                it is InvalidAPIKeyException || it is PermissionMissingException
        } -> AssistantError.NOT_SET_UP
        chain.any { it is UnsupportedUserLocationException } -> AssistantError.REGION
        chain.any { it is RequestTimeoutException || it is SocketTimeoutException || it is TimeoutCancellationException } ->
            AssistantError.TIMEOUT
        chain.any { it is IOException } -> AssistantError.NETWORK
        chain.any { it is ServerException } -> AssistantError.SERVER
        else -> AssistantError.UNKNOWN
    }
}
