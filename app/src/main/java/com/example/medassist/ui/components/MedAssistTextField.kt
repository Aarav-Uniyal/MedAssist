package com.example.medassist.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.medassist.R
import com.example.medassist.ui.theme.Spacing
import com.example.medassist.ui.theme.accessibleAquaTextColor

/**
 * The app's single-line input. Always shows a visible label (not just a hint that disappears),
 * and shows [error] as text under the field, so an error is never signalled by colour alone.
 *
 * Set [isPassword] for password fields: they hide the text, and get a Show/Hide button
 * (a text button rather than an icon, so it is readable, translatable and easy to tap).
 */
@Composable
fun MedAssistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: () -> Unit = {},
    supportingText: String? = null,
) {
    val showHide = stringResource(if (passwordVisible) R.string.action_hide else R.string.action_show)
    val showHideDescription = stringResource(
        if (passwordVisible) R.string.password_hide_description else R.string.password_show_description,
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            // Lets TalkBack read the error as part of the field instead of as separate text.
            .semantics { if (error != null) semanticsError(error) },
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = {
            val text = error ?: supportingText
            if (text != null) Text(text)
        },
        trailingIcon = if (isPassword) {
            {
                TextButton(
                    onClick = onTogglePasswordVisible,
                    modifier = Modifier.semantics { contentDescription = showHideDescription },
                ) { Text(showHide) }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        // Only take over the keyboard button when it means "submit". For Next we keep Android's
        // default behaviour, which moves the cursor to the next field.
        keyboardActions = if (imeAction == ImeAction.Done) {
            KeyboardActions(onDone = { onImeAction() })
        } else {
            KeyboardActions.Default
        },
        shape = MaterialTheme.shapes.medium,
        // Aqua on white is too faint for a field outline (needs 3:1), so focus uses the deeper aqua.
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = accessibleAquaTextColor(),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = accessibleAquaTextColor(),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorCursorColor = MaterialTheme.colorScheme.error,
            focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
        ),
    )
}
