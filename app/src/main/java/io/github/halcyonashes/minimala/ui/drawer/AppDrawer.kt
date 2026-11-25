package io.github.halcyonashes.minimala.ui.drawer

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.ui.common.PanelSpacing
import io.github.halcyonashes.minimala.ui.common.SectionCard
import io.github.halcyonashes.minimala.ui.displayLabel
import io.github.halcyonashes.minimala.ui.startActivitySafely
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    isOpen: Boolean,
    apps: List<AppEntry>,
    renames: Map<String, String>,
    favoriteKeys: Set<String>,
    frequentKeys: Set<String>,
    onLaunchApp: (AppEntry) -> Unit,
    onToggleFavorite: (AppEntry) -> Unit,
    onToggleFrequent: (AppEntry) -> Unit,
    onToggleHidden: (AppEntry) -> Unit,
    onRename: (AppEntry, String) -> Unit,
    onOpenAppInfo: (AppEntry) -> Unit,
    onUninstallApp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var menuTargetKey by remember { mutableStateOf<String?>(null) }
    var renameTargetKey by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // The drawer stays composed while closed (see LauncherShell), so nothing here is torn
    // down on its own: without this the keyboard would stay up over home and keep typing into
    // the hidden field, the old query would still filter the next open, and a HOME press would
    // leave the menu or rename dialog floating over the home screen.
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            focusManager.clearFocus()
            query = ""
            menuTargetKey = null
            renameTargetKey = null
        }
    }

    // Search matches the name the user actually sees, so a renamed app is findable by its
    // new name rather than only by the label the system reports. Re-sorted by that same name:
    // the list arrives sorted by system label, so a renamed app would otherwise sit under its
    // old letter and break the letter index below. Same key as AppRepository's sort.
    val labelled = remember(apps, renames) {
        apps.map { it to it.displayLabel(renames) }.sortedBy { (_, label) -> label.lowercase() }
    }
    val filtered = remember(labelled, query) {
        if (query.isBlank()) labelled else labelled.filter { (_, label) -> label.contains(query, ignoreCase = true) }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Maps each letter to the index of the first app under it. `labelled` is sorted by the
    // displayed label, so the first match per letter is the group start.
    val letterIndex = remember(filtered) {
        val map = LinkedHashMap<Char, Int>()
        filtered.forEachIndexed { index, (_, label) ->
            val letter = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
            map.putIfAbsent(letter, index)
        }
        map
    }

    // The search field is deliberately NOT auto-focused: the drawer is opened to browse far
    // more often than to type, and a keyboard that covers two thirds of the list on every
    // open costs more than it saves. Tapping the field still focuses it normally.

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surface)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SectionCard(
            contentPadding = PaddingValues(horizontal = PanelSpacing.card, vertical = 14.dp),
            modifier = Modifier.padding(bottom = PanelSpacing.section),
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        filtered.firstOrNull()?.let { (entry, _) -> onLaunchApp(entry) }
                            ?: context.webSearch(query)
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (filtered.isEmpty() && query.isNotBlank()) {
            Text(
                text = "No apps match \"$query\"",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "Search the web instead",
                color = palette.accent,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .combinedClickable(onClick = { context.webSearch(query) })
                    .padding(vertical = 8.dp),
            )
        }

        // imePadding shrinks the list (and the scroller beside it) to the space above the
        // keyboard, so the last results can scroll into view instead of sitting under it. It
        // only adds what systemBarsPadding above has not already consumed — the navigation-bar
        // inset is subtracted from the IME inset, never applied twice.
        Row(modifier = Modifier.fillMaxSize().imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered, key = { (entry, _) -> entry.key }) { (entry, label) ->
                    Box {
                        Text(
                            text = label,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onLaunchApp(entry) },
                                    onLongClick = { menuTargetKey = entry.key },
                                )
                                .padding(vertical = 14.dp),
                        )

                        DropdownMenu(
                            expanded = menuTargetKey == entry.key,
                            onDismissRequest = { menuTargetKey = null },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = palette.surface,
                            border = BorderStroke(1.dp, palette.accentMuted),
                        ) {
                            DropdownMenuItem(text = { Text("App info") }, onClick = {
                                menuTargetKey = null
                                onOpenAppInfo(entry)
                            })
                            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                menuTargetKey = null
                                renameTargetKey = entry.key
                                renameText = label
                            })
                            DropdownMenuItem(
                                text = { Text(if (entry.key in favoriteKeys) "Unfavorite" else "Favorite") },
                                onClick = {
                                    menuTargetKey = null
                                    onToggleFavorite(entry)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (entry.key in frequentKeys) "Remove from frequent" else "Add to frequent") },
                                onClick = {
                                    menuTargetKey = null
                                    onToggleFrequent(entry)
                                },
                            )
                            DropdownMenuItem(text = { Text("Hide") }, onClick = {
                                menuTargetKey = null
                                onToggleHidden(entry)
                            })
                            DropdownMenuItem(text = { Text("Uninstall") }, onClick = {
                                menuTargetKey = null
                                onUninstallApp(entry)
                            })
                        }
                    }
                }
            }

            if (query.isBlank() && letterIndex.isNotEmpty()) {
                AlphabetScroller(
                    letters = ALPHABET_SCROLLER_LETTERS,
                    availableLetters = letterIndex.keys,
                    onLetterSelected = { letter ->
                        letterIndex[letter]?.let { index ->
                            scope.launch { listState.scrollToItem(index) }
                        }
                    },
                )
            }
        }
    }

    val renaming = apps.find { it.key == renameTargetKey }
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameTargetKey = null },
            title = { Text("Rename ${renaming.label}") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text(renaming.label) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(renaming, renameText)
                    renameTargetKey = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetKey = null }) { Text("Cancel") }
            },
        )
    }
}

private val ALPHABET_SCROLLER_LETTERS = listOf('#') + ('A'..'Z')

// Single-pointer gesture: a tap selects a letter, a drag tracks it continuously, both driven
// off the same y-to-index math so touch-down and touch-move behave identically.
@Composable
private fun AlphabetScroller(
    letters: List<Char>,
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLauncherPalette.current
    var itemHeightPx by remember { mutableStateOf(0f) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    // pointerInput is keyed on `letters`, which never changes, so its gesture coroutine lives
    // for the whole composition. Read through updated state, or it keeps the letter set and
    // scroll positions from the first composition and jumps to stale rows after any app change.
    val currentAvailableLetters by rememberUpdatedState(availableLetters)
    val currentOnLetterSelected by rememberUpdatedState(onLetterSelected)

    fun letterAt(y: Float): Char? {
        if (itemHeightPx <= 0f || letters.isEmpty()) return null
        val index = (y / itemHeightPx).toInt().coerceIn(0, letters.lastIndex)
        return letters[index]
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .onSizeChanged { size ->
                if (letters.isNotEmpty()) itemHeightPx = size.height / letters.size.toFloat()
            }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    letterAt(down.position.y)?.let { letter ->
                        activeLetter = letter
                        if (letter in currentAvailableLetters) currentOnLetterSelected(letter)
                    }
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pressed = change.pressed
                        if (pressed) {
                            letterAt(change.position.y)?.let { letter ->
                                if (letter != activeLetter) {
                                    activeLetter = letter
                                    if (letter in currentAvailableLetters) currentOnLetterSelected(letter)
                                }
                            }
                            change.consume()
                        }
                    }
                    activeLetter = null
                }
            }
            // The drag gesture is unusable under TalkBack, and 27 one-character text nodes are
            // just noise, so the column becomes one node whose custom actions jump to each
            // letter that actually has apps.
            .clearAndSetSemantics {
                contentDescription = "Alphabet index"
                customActions = letters.filter { it in availableLetters }.map { letter ->
                    CustomAccessibilityAction("Jump to $letter") {
                        onLetterSelected(letter)
                        true
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { letter ->
            val isAvailable = letter in availableLetters
            val color = when {
                letter == activeLetter && isAvailable -> palette.accent
                isAvailable -> palette.textSecondary
                else -> palette.textSecondary.copy(alpha = 0.3f)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun android.content.Context.webSearch(query: String) {
    if (query.isBlank()) return
    startActivitySafely(
        Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query),
        "No search app available",
    )
}
