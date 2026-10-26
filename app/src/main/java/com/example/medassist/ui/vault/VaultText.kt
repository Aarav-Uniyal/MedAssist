package com.example.medassist.ui.vault

import androidx.annotation.StringRes
import com.example.medassist.R
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.DocumentSort
import com.example.medassist.data.repository.VaultError
import com.example.medassist.domain.validation.DocumentFieldError

/** Maps vault errors and choices to localized strings (values/strings.xml and values-hi/strings.xml). */
@StringRes
fun VaultError.messageRes(): Int = when (this) {
    VaultError.NETWORK -> R.string.vault_error_network
    VaultError.SESSION_EXPIRED -> R.string.auth_error_session_expired
    VaultError.PERMISSION_DENIED -> R.string.vault_error_permission
    VaultError.NOT_FOUND -> R.string.vault_error_not_found
    VaultError.QUOTA_EXCEEDED -> R.string.vault_error_quota
    VaultError.FILE_EMPTY -> R.string.vault_error_empty_file
    VaultError.FILE_TOO_LARGE -> R.string.vault_error_too_large
    VaultError.NOT_PDF -> R.string.vault_error_not_pdf
    VaultError.PASSWORD_PROTECTED -> R.string.vault_error_password_protected
    VaultError.CORRUPTED -> R.string.vault_error_corrupted
    VaultError.UNREADABLE -> R.string.vault_error_unreadable
    VaultError.DEVICE_STORAGE_FULL -> R.string.vault_error_device_full
    VaultError.UNKNOWN -> R.string.common_error_generic
}

@StringRes
fun DocumentCategory.labelRes(): Int = when (this) {
    DocumentCategory.BLOOD_TESTS -> R.string.category_blood_tests
    DocumentCategory.PRESCRIPTIONS -> R.string.category_prescriptions
    DocumentCategory.DIAGNOSTIC_REPORTS -> R.string.category_diagnostic_reports
    DocumentCategory.IMAGING_REPORTS -> R.string.category_imaging_reports
    DocumentCategory.MEDICAL_HISTORY -> R.string.category_medical_history
    DocumentCategory.OTHER -> R.string.category_other
}

@StringRes
fun DocumentSort.labelRes(): Int = when (this) {
    DocumentSort.NEWEST -> R.string.vault_sort_newest
    DocumentSort.OLDEST -> R.string.vault_sort_oldest
    DocumentSort.NAME -> R.string.vault_sort_name
    DocumentSort.LARGEST -> R.string.vault_sort_largest
}

@StringRes
fun DocumentFieldError.messageRes(): Int = when (this) {
    DocumentFieldError.NAME_EMPTY -> R.string.vault_error_doc_name_empty
    DocumentFieldError.NAME_TOO_LONG -> R.string.vault_error_doc_name_too_long
    DocumentFieldError.NOTES_TOO_LONG -> R.string.vault_error_notes_too_long
}
