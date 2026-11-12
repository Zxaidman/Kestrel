package io.github.zxaidman.kestrel.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.zxaidman.kestrel.diagnostics.DisplaySection
import io.github.zxaidman.kestrel.diagnostics.StorageSection
import io.github.zxaidman.kestrel.ui.theme.BackHeader

/**
 * The settings a person changes, and nothing else.
 *
 * Display and Files used to live on the diagnostics screen, between raw stick readings and Shizuku's
 * version number — the one page every user saw first, and the one page that was for the developer
 * (`CRIT-2`). They are here now, and only here, so each setting has one place it is changed.
 *
 * The pad's own settings — size, stick shaping, trigger feel — stay in the layout editor, beside the
 * pad they change. A slider is judged by looking at the thing it moves.
 */
@Composable
public fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader("Settings", onBack)
        DisplaySection(context)
        StorageSection(context)
        Text(
            text = "The pad's size, stick shaping and trigger feel are set in the layout editor, " +
                "beside the pad they change.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}
