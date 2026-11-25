package io.github.halcyonashes.minimala.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.settings.CalendarSystem
import io.github.halcyonashes.minimala.core.settings.ClockStyle
import io.github.halcyonashes.minimala.core.settings.DateStyle
import io.github.halcyonashes.minimala.core.settings.FontWeightOption
import io.github.halcyonashes.minimala.core.settings.LauncherSettings
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.theme.bundledFonts
import io.github.halcyonashes.minimala.theme.bundledPalettes
import io.github.halcyonashes.minimala.theme.paletteById
import io.github.halcyonashes.minimala.ui.clampDayWindow
import io.github.halcyonashes.minimala.ui.displayLabel

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppEntry>,
    onBack: () -> Unit,
    onSelectPalette: (String) -> Unit,
    onSelectFont: (String) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onFontWeightChange: (FontWeightOption) -> Unit,
    onClockScaleChange: (Float) -> Unit,
    onClockStyleChange: (ClockStyle) -> Unit,
    onDateStyleChange: (DateStyle) -> Unit,
    onCalendarSystemChange: (CalendarSystem) -> Unit,
    onBackgroundOpacityChange: (Float) -> Unit,
    onDayWindowChange: (Int, Int) -> Unit,
    onToggleHidden: (AppEntry) -> Unit,
) {
    val palette = LocalLauncherPalette.current

    // Sliders drag a local value and persist once on release: persisting from onValueChange
    // queued a full DataStore write per drag frame, and the thumb only moved once the stored
    // value came back, so it trailed the finger. Keyed on the stored value so any change that
    // lands in settings (another writer, or the view model's clamping) re-syncs the thumb.
    var fontScale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }
    var clockScale by remember(settings.clockScale) { mutableFloatStateOf(settings.clockScale) }
    var backgroundOpacity by remember(settings.backgroundOpacity) {
        mutableFloatStateOf(settings.backgroundOpacity)
    }
    var dayStart by remember(settings.dayStartMinute) {
        mutableFloatStateOf(settings.dayStartMinute.toFloat())
    }
    var dayEnd by remember(settings.dayEndMinute) {
        mutableFloatStateOf(settings.dayEndMinute.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Opaque, unlike the panels: this is a settings sheet, not a home surface, and
            // sliders/labels need stable contrast rather than wallpaper showing through.
            .background(palette.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "← Back",
            color = palette.accent,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                // Read as "Back", not "left arrow Back".
                .semantics { contentDescription = "Back" }
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 8.dp),
        )

        SectionTitle("Theme")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            bundledPalettes.forEach { candidate ->
                val selected = candidate.id == settings.paletteId
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(candidate.background)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) candidate.accent else palette.divider,
                            shape = CircleShape,
                        )
                        // A colour swatch has no text, so TalkBack needs the name spelled out.
                        .semantics { contentDescription = "${candidate.displayName} theme" }
                        .clickable(role = Role.Button) { onSelectPalette(candidate.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(candidate.accent),
                        )
                    }
                }
            }
        }
        Text(
            // paletteById, not first{}: an unknown stored id (a palette removed in an update)
            // would otherwise throw and crash settings; the theme already falls back the same way.
            text = paletteById(settings.paletteId).displayName,
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        SectionTitle("Font")
        Column(modifier = Modifier.padding(top = 8.dp)) {
            bundledFonts.forEach { font ->
                // Each row renders in its own family — a font picker whose rows all use one
                // shared face isn't actually a preview, it's just a list of names.
                OptionRow(
                    label = font.displayName,
                    fontFamily = font.family,
                    selected = font.id == settings.fontId,
                    onClick = { onSelectFont(font.id) },
                )
            }
        }

        SectionTitle("Weight")
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FontWeightOption.entries.forEach { weight ->
                Text(
                    text = weight.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (weight == settings.fontWeight) palette.accent else palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onFontWeightChange(weight) }
                        .padding(vertical = 6.dp),
                )
            }
        }

        SectionTitle("Text size")
        ValueLabel("${(fontScale * 100).toInt()}%")
        Slider(
            value = fontScale,
            onValueChange = { fontScale = it },
            onValueChangeFinished = { onFontScaleChange(fontScale) },
            valueRange = 0.85f..1.3f,
        )

        SectionTitle("Clock style")
        Column(modifier = Modifier.padding(top = 8.dp)) {
            ClockStyle.entries.forEach { style ->
                OptionRow(
                    label = style.displayName,
                    selected = style == settings.clockStyle,
                    onClick = { onClockStyleChange(style) },
                )
            }
        }

        SectionTitle("Date style")
        Column(modifier = Modifier.padding(top = 8.dp)) {
            DateStyle.entries.forEach { style ->
                OptionRow(
                    label = style.displayName,
                    selected = style == settings.dateStyle,
                    onClick = { onDateStyleChange(style) },
                )
            }
        }

        SectionTitle("Calendar")
        Column(modifier = Modifier.padding(top = 8.dp)) {
            CalendarSystem.entries.forEach { system ->
                OptionRow(
                    label = system.displayName,
                    selected = system == settings.calendarSystem,
                    onClick = { onCalendarSystemChange(system) },
                )
            }
        }

        SectionTitle("Clock size")
        ValueLabel("${(clockScale * 100).toInt()}%")
        Slider(
            value = clockScale,
            onValueChange = { clockScale = it },
            onValueChangeFinished = { onClockScaleChange(clockScale) },
            valueRange = 0.6f..1.6f,
        )

        SectionTitle("Background")
        // Drives the scrim the launcher paints over the wallpaper: 100% is a solid palette
        // background with no wallpaper showing through at all.
        ValueLabel(
            if (backgroundOpacity >= 1f) {
                "Opaque — wallpaper hidden"
            } else {
                "${(backgroundOpacity * 100).toInt()}%"
            },
        )
        Slider(
            value = backgroundOpacity,
            onValueChange = { backgroundOpacity = it },
            onValueChangeFinished = { onBackgroundOpacityChange(backgroundOpacity) },
            valueRange = 0f..1f,
        )

        SectionTitle("Day window")
        ValueLabel("${formatMinutes(dayStart.toInt())} – ${formatMinutes(dayEnd.toInt())}")
        // On release the thumbs snap to the clamped window before persisting. Keying on the
        // stored value alone is not enough here: a drag the clamp maps back to the value already
        // stored writes nothing, nothing re-emits, and the thumb would stay where it was dropped.
        val commitDayWindow: (Int, Int) -> Unit = { start, end ->
            val (clampedStart, clampedEnd) = clampDayWindow(start, end)
            dayStart = clampedStart.toFloat()
            dayEnd = clampedEnd.toFloat()
            onDayWindowChange(clampedStart, clampedEnd)
        }
        Slider(
            value = dayStart,
            onValueChange = { dayStart = it },
            onValueChangeFinished = { commitDayWindow(dayStart.toInt(), settings.dayEndMinute) },
            valueRange = 0f..(23 * 60).toFloat(),
        )
        Slider(
            value = dayEnd,
            onValueChange = { dayEnd = it },
            onValueChangeFinished = { commitDayWindow(settings.dayStartMinute, dayEnd.toInt()) },
            valueRange = 0f..(24 * 60).toFloat(),
        )

        SectionTitle("Hidden apps")
        Text(
            text = "Tap to show or hide in the app drawer.",
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            // A plain Column, not a LazyColumn: a lazy list cannot be nested inside a
            // verticalScroll parent — it would be measured with infinite height and throw.
            allApps.forEach { entry ->
                val hidden = entry.key in settings.hiddenAppKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHidden(entry) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entry.displayLabel(settings.appRenames),
                        color = if (hidden) palette.textSecondary else palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hidden) {
                        Text(
                            text = "hidden",
                            color = palette.accentMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    fontFamily: FontFamily? = null,
) {
    val palette = LocalLauncherPalette.current
    Text(
        text = label,
        // Selected must read as the prominent one: accent alone isn't reliably brighter than
        // textPrimary across every bundled palette, so unselected rows recede to textSecondary
        // and selected additionally gets weight, not just a color swap.
        color = if (selected) palette.accent else palette.textSecondary,
        style = MaterialTheme.typography.bodyLarge.let {
            val withFont = if (fontFamily != null) it.copy(fontFamily = fontFamily) else it
            withFont.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    val palette = LocalLauncherPalette.current
    Text(
        text = text,
        color = palette.textSecondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 28.dp, bottom = 2.dp),
    )
}

@Composable
private fun ValueLabel(text: String) {
    val palette = LocalLauncherPalette.current
    Text(
        text = text,
        color = palette.textPrimary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "%02d:%02d".format(hours, minutes)
}
