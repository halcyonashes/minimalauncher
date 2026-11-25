package io.github.halcyonashes.minimala.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette

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

    /** Smallest touch target for any control, however small it is drawn. */
    val touchTarget = 48.dp
}

/**
 * A button drawn smaller than [PanelSpacing.touchTarget] that still takes taps, and TalkBack
 * focus, across the full target. The extra area overhangs the surrounding padding instead of
 * growing the layout: this reports only the visual's size to its parent, so panel spacing is
 * unchanged. That relies on Compose hit-testing a child outside its parent's bounds unless a clip
 * stops it, so keep the overhang (up to 8dp a side) clear of clipping containers.
 *
 * [content] must emit exactly one layout node and apply the modifier it is given after its clip:
 * that keeps the press ripple on the visible shape and hides the glyph from accessibility, where
 * [description] speaks for it. A null [onClick] shows the control as disabled.
 */
@Composable
fun CompactTouchTarget(
    onClick: (() -> Unit)?,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable (visualModifier: Modifier) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Layout(
        modifier = modifier,
        content = {
            content(Modifier.clearAndSetSemantics {}.indication(interactionSource, indication))
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = onClick != null,
                        role = Role.Button,
                        onClick = { onClick?.invoke() },
                    )
                    .semantics { contentDescription = description },
            )
        },
    ) { measurables, constraints ->
        val visual = measurables.first().measure(constraints)
        val minSize = PanelSpacing.touchTarget.roundToPx()
        val target = measurables.last().measure(
            Constraints.fixed(maxOf(visual.width, minSize), maxOf(visual.height, minSize)),
        )
        layout(visual.width, visual.height) {
            visual.place(0, 0)
            // Placed last so it is hit-tested first, over the visual it surrounds.
            target.place((visual.width - target.width) / 2, (visual.height - target.height) / 2)
        }
    }
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
