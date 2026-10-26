package com.example.medassist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.medassist.R
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.theme.DarkCharcoal
import com.example.medassist.ui.theme.ErrorRed
import com.example.medassist.ui.theme.ErrorTint
import com.example.medassist.ui.theme.Spacing

/** A message for the whole form (for example "Incorrect email or password"). Icon + text, never colour alone. */
@Composable
fun FormErrorBanner(message: String, modifier: Modifier = Modifier) {
    MessageBanner(
        message = message,
        background = ErrorTint,
        iconRes = R.drawable.ic_warning,
        iconTint = ErrorRed,
        modifier = modifier,
    )
}

/** A calm, positive or neutral note (for example "Verification email sent"). */
@Composable
fun FormInfoBanner(message: String, modifier: Modifier = Modifier) {
    MessageBanner(
        message = message,
        background = AquaTint,
        iconRes = R.drawable.ic_mail,
        iconTint = AquaDeep,
        modifier = modifier,
    )
}

@Composable
private fun MessageBanner(
    message: String,
    background: androidx.compose.ui.graphics.Color,
    iconRes: Int,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    AnnounceForNarration(message)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(Spacing.md)
            // TalkBack reads the message out as soon as it appears.
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkCharcoal,
        )
    }
}
