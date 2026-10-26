package com.example.medassist.domain

import com.example.medassist.domain.validation.AuthValidator
import com.example.medassist.domain.validation.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    @Test fun email_empty_isRejected() {
        assertEquals(FieldError.EMAIL_EMPTY, AuthValidator.validateEmail(""))
        assertEquals(FieldError.EMAIL_EMPTY, AuthValidator.validateEmail("   "))
    }

    @Test fun email_withoutAtOrDomain_isRejected() {
        listOf("asha", "asha@", "@example.com", "asha@example", "as ha@example.com").forEach {
            assertEquals("for \"$it\"", FieldError.EMAIL_INVALID, AuthValidator.validateEmail(it))
        }
    }

    @Test fun email_valid_isAccepted_evenWithSurroundingSpaces() {
        assertNull(AuthValidator.validateEmail("asha@example.com"))
        assertNull(AuthValidator.validateEmail("  asha.k+test@mail.example.co.in "))
    }

    @Test fun name_rules() {
        assertEquals(FieldError.NAME_EMPTY, AuthValidator.validateName("  "))
        assertEquals(FieldError.NAME_TOO_LONG, AuthValidator.validateName("a".repeat(61)))
        assertNull(AuthValidator.validateName("Asha"))
        assertNull(AuthValidator.validateName("आशा शर्मा")) // Hindi names are fine
    }

    @Test fun newPassword_rules() {
        assertEquals(FieldError.PASSWORD_EMPTY, AuthValidator.validateNewPassword(""))
        assertEquals(FieldError.PASSWORD_TOO_SHORT, AuthValidator.validateNewPassword("abc123"))
        assertEquals(FieldError.PASSWORD_WEAK, AuthValidator.validateNewPassword("onlyletters"))
        assertEquals(FieldError.PASSWORD_WEAK, AuthValidator.validateNewPassword("12345678"))
        assertNull(AuthValidator.validateNewPassword("abcd1234"))
    }

    @Test fun signInPassword_onlyNeedsToBePresent() {
        assertEquals(FieldError.PASSWORD_EMPTY, AuthValidator.validatePasswordPresent(""))
        assertNull(AuthValidator.validatePasswordPresent("x"))
    }

    @Test fun confirm_rules() {
        assertEquals(FieldError.CONFIRM_EMPTY, AuthValidator.validateConfirm("abcd1234", ""))
        assertEquals(FieldError.PASSWORDS_MISMATCH, AuthValidator.validateConfirm("abcd1234", "abcd1235"))
        assertNull(AuthValidator.validateConfirm("abcd1234", "abcd1234"))
    }
}
