package com.example.medassist.ui.vault

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.medassist.R
import com.example.medassist.data.model.MedicalDocument
import java.text.DateFormat
import java.util.Date

/** "12 Mar 2026 · 1.2 MB", in the phone's language. */
@Composable
fun documentMeta(document: MedicalDocument): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(document.uploadedAtMillis))
    return stringResource(R.string.vault_doc_meta, date, Formatter.formatShortFileSize(context, document.sizeBytes))
}

@Composable
fun fileSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)
