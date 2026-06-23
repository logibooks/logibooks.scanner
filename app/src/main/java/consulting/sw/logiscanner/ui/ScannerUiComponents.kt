// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import consulting.sw.logiscanner.BuildConfig
import consulting.sw.logiscanner.R
import consulting.sw.logiscanner.net.ParcelCheckStatusProjectionKinds

// Light theme check status pill colors
private val CheckStatusRedBackground = Color(0x24F44336)
private val CheckStatusRedText = Color(0xFFB71C1C)
private val CheckStatusRedBorder = Color(0xFFC62828)
private val CheckStatusBlueBackground = Color(0x2E2196F3)
private val CheckStatusBlueText = Color(0xFF0D47A1)
private val CheckStatusBlueBorder = Color(0xFF1565C0)
private val CheckStatusGreenBackground = Color(0x2E4CAF50)
private val CheckStatusGreenText = Color(0xFF1B5E20)
private val CheckStatusGreenBorder = Color(0xFF2E7D32)

// Dark theme check status pill colors (lighter tones for contrast on dark surfaces)
private val CheckStatusDarkRedBackground = Color(0x30EF5350)
private val CheckStatusDarkRedText = Color(0xFFFF8A80)
private val CheckStatusDarkRedBorder = Color(0xFFEF5350)
private val CheckStatusDarkBlueBackground = Color(0x3042A5F5)
private val CheckStatusDarkBlueText = Color(0xFF90CAF9)
private val CheckStatusDarkBlueBorder = Color(0xFF42A5F5)
private val CheckStatusDarkGreenBackground = Color(0x3066BB6A)
private val CheckStatusDarkGreenText = Color(0xFFA5D6A7)
private val CheckStatusDarkGreenBorder = Color(0xFF66BB6A)

internal val AutoFollowOnIconColor = Color(0xFF4CAF50)
internal val AutoFollowOffIconColor = Color(0xFFFF9800)
internal val KgtModeOnIconColor = AutoFollowOnIconColor
internal val KgtModeOffIconColor = AutoFollowOffIconColor

@Composable
internal fun DismissibleMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.dismiss_message),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
internal fun StatusPill(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
internal fun CheckStatusPill(
    text: String,
    style: CheckStatusStyle,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = style.content,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(style.background, shape)
            .border(style.borderWidth, style.border, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

internal data class CheckStatusStyle(
    val background: Color,
    val content: Color,
    val border: Color,
    val borderWidth: androidx.compose.ui.unit.Dp = 1.dp
)

@Composable
internal fun checkStatusStyle(kind: Int?): CheckStatusStyle {
    val darkTheme = isSystemInDarkTheme()
    return when (kind) {
        ParcelCheckStatusProjectionKinds.NOT_CHECKED -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkBlueBackground,
            CheckStatusDarkBlueText,
            CheckStatusDarkBlueBorder
        ) else CheckStatusStyle(
            CheckStatusBlueBackground,
            CheckStatusBlueText,
            CheckStatusBlueBorder
        )
        ParcelCheckStatusProjectionKinds.RESTRICTION,
        ParcelCheckStatusProjectionKinds.DEFECT -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkRedBackground,
            CheckStatusDarkRedText,
            CheckStatusDarkRedBorder
        ) else CheckStatusStyle(
            CheckStatusRedBackground,
            CheckStatusRedText,
            CheckStatusRedBorder
        )
        ParcelCheckStatusProjectionKinds.CHECKED -> if (darkTheme) CheckStatusStyle(
            CheckStatusDarkGreenBackground,
            CheckStatusDarkGreenText,
            CheckStatusDarkGreenBorder
        ) else CheckStatusStyle(
            CheckStatusGreenBackground,
            CheckStatusGreenText,
            CheckStatusGreenBorder
        )
        else -> CheckStatusStyle(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
internal fun VersionFooter() {
    Text(
        text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
internal fun PrinterStatusMessages(
    loading: Boolean,
    message: String?,
    error: String?
) {
    if (loading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
    error?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    message?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
