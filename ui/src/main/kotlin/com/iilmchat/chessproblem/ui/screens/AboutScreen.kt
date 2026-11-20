package com.iilmchat.chessproblem.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iilmchat.chessproblem.ui.R
import com.iilmchat.chessproblem.ui.components.ChessTopBar
import com.iilmchat.chessproblem.ui.components.InfoPill
import com.iilmchat.chessproblem.ui.components.ResponsiveScaffoldContainer
import com.iilmchat.chessproblem.ui.components.SectionHeader

private val LICENSE_FILES = listOf(
    "NOTICE",
    "Apache-2.0.txt",
    "BSD-2-Clause.txt",
    "CC0-1.0.txt",
    "OFL-1.1.txt",
)

private fun readLicense(context: Context, fileName: String): String = runCatching {
    context.assets.open("licenses/$fileName").bufferedReader().use { it.readText() }
}.getOrElse {
    context.getString(R.string.about_license_unavailable)
}

@Composable
fun AboutScreen(
    appVersion: String,
    contentVersion: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showLicensePicker by remember { mutableStateOf(false) }
    var openedLicense by remember { mutableStateOf<Pair<String, String>?>(null) }

    ResponsiveScaffoldContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            ChessTopBar(
                title = stringResource(R.string.about_title),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "\u265E",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_content_version, contentVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(text = "v$appVersion")
                    InfoPill(text = stringResource(R.string.about_offline))
                    InfoPill(text = stringResource(R.string.about_languages))
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionHeader(title = stringResource(R.string.about_content_title))
                AboutCard(text = stringResource(R.string.about_content_body))
                SectionHeader(title = stringResource(R.string.about_licenses_title))
                AboutCard(
                    text = stringResource(R.string.about_licenses_body),
                    actionLabel = stringResource(R.string.about_licenses_open),
                    onClick = { showLicensePicker = true },
                )
                SectionHeader(title = stringResource(R.string.about_privacy_title))
                AboutCard(text = stringResource(R.string.about_privacy_body))
                SectionHeader(title = stringResource(R.string.about_source_title))
                AboutCard(text = stringResource(R.string.about_source_body), accent = true)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.about_made),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showLicensePicker) {
        AlertDialog(
            onDismissRequest = { showLicensePicker = false },
            title = { Text(stringResource(R.string.about_licenses_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    LICENSE_FILES.forEach { fileName ->
                        TextButton(
                            onClick = {
                                openedLicense = fileName to readLicense(context, fileName)
                                showLicensePicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = fileName, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensePicker = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }

    val license = openedLicense
    if (license != null) {
        AlertDialog(
            onDismissRequest = { openedLicense = null },
            title = { Text(license.first) },
            text = {
                Text(
                    text = license.second,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { openedLicense = null }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }
}

@Composable
private fun AboutCard(
    text: String,
    accent: Boolean = false,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = if (accent) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            1.dp,
            if (accent) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            },
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
