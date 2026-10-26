package com.example.medassist.data

import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.authErrorForCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuthErrorMapperTest {

    @Test fun wrongPassword_andUnknownUser_lookIdentical() {
        // So that nobody can use the login form to discover which emails have accounts.
        assertEquals(AuthError.INVALID_CREDENTIALS, authErrorForCode("ERROR_WRONG_PASSWORD"))
        assertEquals(AuthError.INVALID_CREDENTIALS, authErrorForCode("ERROR_USER_NOT_FOUND"))
        assertEquals(AuthError.INVALID_CREDENTIALS, authErrorForCode("ERROR_INVALID_CREDENTIAL"))
    }

    @Test fun registrationErrors() {
        assertEquals(AuthError.EMAIL_IN_USE, authErrorForCode("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthError.WEAK_PASSWORD, authErrorForCode("ERROR_WEAK_PASSWORD"))
        assertEquals(AuthError.WEAK_PASSWORD, authErrorForCode("ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS"))
        assertEquals(AuthError.INVALID_EMAIL, authErrorForCode("ERROR_INVALID_EMAIL"))
    }

    @Test fun accountAndSessionErrors() {
        assertEquals(AuthError.USER_DISABLED, authErrorForCode("ERROR_USER_DISABLED"))
        assertEquals(AuthError.SESSION_EXPIRED, authErrorForCode("ERROR_USER_TOKEN_EXPIRED"))
        assertEquals(AuthError.TOO_MANY_REQUESTS, authErrorForCode("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(AuthError.NETWORK, authErrorForCode("ERROR_NETWORK_REQUEST_FAILED"))
    }

    @Test fun unknownOrMissingCode_fallsBackToUnknown() {
        assertEquals(AuthError.UNKNOWN, authErrorForCode("SOMETHING_NEW"))
        assertEquals(AuthError.UNKNOWN, authErrorForCode(null))
        assertNotEquals(AuthError.INVALID_CREDENTIALS, authErrorForCode(null))
    }
}
