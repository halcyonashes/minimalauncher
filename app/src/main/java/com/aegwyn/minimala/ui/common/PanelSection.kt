package com.aegwyn.minimala.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegwyn.minimala.theme.LocalLauncherPalette

/**
 * Shared spacing scale for the side panels. One scale in one place keeps the two panels in
 * rhythm — the alternative was each panel re-picking its own 20.dp and drifting apart.
 */
object PanelSpacing {
    /** Outer panel inset. */
    val edge = 22.dp

    /** Gap below the status bar. Content sitting flush against the clock reads as a bug. */
    val edgeTop = 20.dp

    /** Gap between two sections (card to card). */
    val section = 22.dp

    /** Gap between a section label and its content. */
    val label = 10.dp

    /** Inner padding of a card surface. */
    val card = 16.dp

    /** Minimum height of a tappable row — below this it stops being comfortably hittable. */
    val touchRow = 52.dp
}

/** Small all-caps eyebrow above a section. Carries hierarchy without spending a big type size. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, align: TextAlign? = null) {
    val palette = LocalLauncherPalette.current
    Text(
        text = text.uppercase(),
        color = palette.textSecondary,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        textAlign = align,
        modifier = modifier.padding(bottom = PanelSpacing.label),
    )
}

/**
 * Rounded translucent surface used for every grouped block in the side panels. Layering the
 * panels this way is what separates content from the wallpaper showing through the scrim.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(PanelSpacing.card),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalLauncherPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.72f))
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
