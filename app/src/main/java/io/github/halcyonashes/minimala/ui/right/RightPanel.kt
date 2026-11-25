package io.github.halcyonashes.minimala.ui.right

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.calendar.nepali.nepaliFullLabel
import io.github.halcyonashes.minimala.core.calendar.nepali.toLocalDate
import io.github.halcyonashes.minimala.core.settings.CalendarSystem
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.ui.LauncherUiState
import io.github.halcyonashes.minimala.ui.common.CompactTouchTarget
import io.github.halcyonashes.minimala.ui.common.PanelSpacing
import io.github.halcyonashes.minimala.ui.common.SectionLabel
import io.github.halcyonashes.minimala.ui.common.rememberToday
import io.github.halcyonashes.minimala.ui.displayLabel
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NoteActionSize = 32.dp

/** Tallest the Frequent list grows before it scrolls: about five rows. */
private val FrequentMaxHeight = 220.dp

/** Least the Notes field keeps under a long Frequent list or the keyboard: about three lines. */
private val NotesMinHeight = 120.dp

@Composable
fun RightPanel(
    uiState: LauncherUiState,
    onLaunchApp: (AppEntry) -> Unit,
    onCreateEvent: (title: String) -> Unit = {},
    onScratchPadChange: (String) -> Unit = {},
    onReorderFrequent: (List<String>) -> Unit = {},
) {
    val palette = LocalLauncherPalette.current
    // Saveable: the pager drops this page's plain remember state whenever it scrolls away.
    var composingTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val calendarSystem = uiState.settings.calendarSystem
    // Keyed on today so the heading rolls over at midnight while this page stays on screen.
    val today = rememberToday()
    val todayLabel = remember(calendarSystem, today) { dayLabel(System.currentTimeMillis(), calendarSystem) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            // After systemBarsPadding, which has already consumed the navigation bar, so this
            // adds only the part of the keyboard above it: the Notes field stays in view.
            .imePadding()
            .padding(
                start = PanelSpacing.edge,
                end = PanelSpacing.edge,
                top = PanelSpacing.edgeTop,
                bottom = PanelSpacing.edge,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = todayLabel,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Circular affordance instead of a bare glyph: a 40dp disc reads as a button rather
            // than as stray text next to the heading, and its touch target is the full 48dp.
            CompactTouchTarget(
                onClick = { composingTitle = if (composingTitle == null) "" else null },
                description = if (composingTitle == null) "New event" else "Cancel new event",
            ) { visual ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.16f))
                        .then(visual),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (composingTitle == null) "+" else "×",
                        color = palette.accent,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (composingTitle != null) {
            OutlinedTextField(
                value = composingTitle.orEmpty(),
                onValueChange = { composingTitle = it },
                placeholder = { Text("New event title") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val title = composingTitle.orEmpty()
                    if (title.isNotBlank()) onCreateEvent(title)
                    composingTitle = null
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PanelSpacing.label),
            )
        }

        Spacer(modifier = Modifier.height(PanelSpacing.section))

        // null while not editing: the field mirrors the stored note, so it still picks it up when
        // settings arrive after first composition (uiState starts from defaults). Edits stay
        // local until ✓ saves them (or the page is left, below); × throws them away. Saveable so
        // a swipe away, HOME or a configuration change keeps the unsaved text.
        var draft by rememberSaveable { mutableStateOf<String?>(null) }
        var isNoteFocused by remember { mutableStateOf(false) }
        // Leaving the page still commits a pending draft rather than dropping it: saveable state
        // only lives as long as the pager, so opening settings or a process restart would lose
        // it silently. × stays the explicit way to throw edits away; leaving is not a discard.
        val pendingDraft by rememberUpdatedState(draft)
        val saveNote by rememberUpdatedState(onScratchPadChange)
        DisposableEffect(Unit) {
            onDispose { pendingDraft?.let(saveNote) }
        }
        val focusManager = LocalFocusManager.current
        val noteText = draft ?: uiState.settings.scratchPad
        val closeNote = { save: Boolean ->
            if (save) draft?.let(onScratchPadChange)
            draft = null
            focusManager.clearFocus()
        }

        // Min height matches the action buttons so the field doesn't jump when they appear.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NoteActionSize + PanelSpacing.label),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Notes", modifier = Modifier.weight(1f))
            if (isNoteFocused || draft != null) {
                if (noteText.isNotEmpty()) NoteAction("Clear", "Clear note") { draft = "" }
                NoteAction("×", "Discard changes") { closeNote(false) }
                NoteAction("✓", "Save note") { closeNote(true) }
            }
        }
        OutlinedTextField(
            value = noteText,
            onValueChange = { draft = it },
            placeholder = { Text("Jot something down") },
            shape = RoundedCornerShape(16.dp),
            // weight, not fillMaxSize: an unweighted child would take the whole panel height
            // and push the frequent-apps section off the bottom. Its minimum height is held by
            // the Frequent list's cap below, since a weighted child's own heightIn is overridden.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onFocusChanged { isNoteFocused = it.isFocused },
        )

        // A plain scrolling Column, not a LazyColumn: this list is a handful of user-picked
        // apps, not a feed. Nothing caps how many get added, though, so it is bounded and scrolls.
        if (uiState.frequentApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PanelSpacing.section))
            SectionLabel("Frequent")
            FrequentList(
                apps = uiState.frequentApps,
                renames = uiState.settings.appRenames,
                onLaunchApp = onLaunchApp,
                onReorder = onReorderFrequent,
                // The Column measures this before the weighted Notes field, so the height on
                // offer here is what the two share. Taking all but NotesMinHeight of it keeps the
                // note usable under a long list or with the keyboard up.
                modifier = Modifier.layout { measurable, constraints ->
                    val cap = minOf(
                        FrequentMaxHeight.roundToPx(),
                        (constraints.maxHeight - NotesMinHeight.roundToPx()).coerceAtLeast(0),
                    )
                    val placeable = measurable.measure(constraints.copy(maxHeight = cap))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
            )
        }
    }
}

/**
 * Long-press a row, then drag vertically to move it. The order is kept locally while dragging
 * and persisted once on drop, so settings aren't rewritten on every swap. TalkBack users get the
 * same through "Move up" / "Move down" actions on each row.
 *
 * The list scrolls when it overflows. A drag that moves before the long press fires is a scroll;
 * once the long press fires the row consumes every move, which cancels the scroll's own drag
 * detection, so the two never fight. There is no auto-scroll while dragging.
 */
@Composable
private fun FrequentList(
    apps: List<AppEntry>,
    renames: Map<String, String>,
    onLaunchApp: (AppEntry) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    var order by remember(apps) { mutableStateOf(apps) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }

    fun endDrag(commit: Boolean) {
        if (commit && order != apps) onReorder(order.map { it.key }) else order = apps
        draggingKey = null
        dragOffset = 0f
    }

    // Swap with a neighbour once the row is dragged past half of it; the offset is shifted by
    // one row so the dragged row stays under the finger after it moves in the list.
    fun onDrag(deltaY: Float) {
        dragOffset += deltaY
        val index = order.indexOfFirst { it.key == draggingKey }
        val half = rowHeight / 2f
        val target = when {
            rowHeight == 0 || index < 0 -> return
            dragOffset > half && index < order.lastIndex -> index + 1
            dragOffset < -half && index > 0 -> index - 1
            else -> return
        }
        order = order.toMutableList().apply { add(target, removeAt(index)) }
        dragOffset += if (target > index) -rowHeight else rowHeight
    }

    // The current order with one row moved, as the keys onReorder persists.
    fun moved(from: Int, to: Int): List<String> =
        order.map { it.key }.toMutableList().apply { add(to, removeAt(from)) }

    val scrollState = rememberScrollState()
    Column(
        // Scrolling only switches on when the list overflows, so a short list leaves vertical
        // swipes to the panel's drawer gesture exactly as before.
        modifier = modifier.verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
    ) {
        order.forEachIndexed { index, entry ->
            key(entry.key) {
                val isDragging = entry.key == draggingKey
                Text(
                    text = entry.displayLabel(renames),
                    color = if (isDragging) palette.accent else palette.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowHeight = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .clickable { onLaunchApp(entry) }
                        .semantics {
                            customActions = listOfNotNull(
                                if (index > 0) {
                                    CustomAccessibilityAction("Move up") { onReorder(moved(index, index - 1)); true }
                                } else {
                                    null
                                },
                                if (index < order.lastIndex) {
                                    CustomAccessibilityAction("Move down") { onReorder(moved(index, index + 1)); true }
                                } else {
                                    null
                                },
                            )
                        }
                        // Inside clickable so it sees events first and consumes the drag/up,
                        // which stops the long-press release from also launching the app.
                        // Keyed on apps too: `order` is re-remembered when apps changes, and a
                        // stale gesture coroutine would keep writing to the old state.
                        .pointerInput(entry.key, apps) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingKey = entry.key; dragOffset = 0f },
                                onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                                onDragEnd = { endDrag(commit = true) },
                                onDragCancel = { endDrag(commit = false) },
                            )
                        }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

/** Pill rather than circle so "Clear" fits; a single glyph at min width still reads as a circle. */
@Composable
private fun NoteAction(label: String, description: String, onClick: () -> Unit) {
    val palette = LocalLauncherPalette.current
    CompactTouchTarget(
        onClick = onClick,
        description = description,
        modifier = Modifier.padding(start = 8.dp, bottom = PanelSpacing.label),
    ) { visual ->
        Box(
            modifier = Modifier
                .height(NoteActionSize)
                .defaultMinSize(minWidth = NoteActionSize)
                .clip(RoundedCornerShape(NoteActionSize / 2))
                .background(palette.accent.copy(alpha = 0.16f))
                .then(visual)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, color = palette.accent, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * Bikram Sambat label for BS, falling back to the Gregorian one outside the BS table's
 * range (BS 1975..2099). A wrong Nepali date is worse than an honest English one.
 */
private fun dayLabel(millis: Long, calendarSystem: CalendarSystem): String =
    (if (calendarSystem == CalendarSystem.BS) nepaliFullLabel(millis.toLocalDate()) else null)
        ?: SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(millis))
