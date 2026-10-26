package com.example.medassist.domain

import com.example.medassist.data.repository.VaultError
import com.example.medassist.data.repository.vaultErrorForCode
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultErrorMapperTest {
    @Test fun mapsFirestoreCodes() {
        assertEquals(VaultError.NETWORK, vaultErrorForCode("UNAVAILABLE"))
        assertEquals(VaultError.PERMISSION_DENIED, vaultErrorForCode("PERMISSION_DENIED"))
        assertEquals(VaultError.SESSION_EXPIRED, vaultErrorForCode("UNAUTHENTICATED"))
        assertEquals(VaultError.QUOTA_EXCEEDED, vaultErrorForCode("RESOURCE_EXHAUSTED"))
        assertEquals(VaultError.NOT_FOUND, vaultErrorForCode("NOT_FOUND"))
        assertEquals(VaultError.UNKNOWN, vaultErrorForCode("SOMETHING_NEW"))
        assertEquals(VaultError.UNKNOWN, vaultErrorForCode(null))
    }
}
