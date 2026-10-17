package io.github.halcyonashes.minimala.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import io.github.halcyonashes.minimala.core.apps.AppEntry
import io.github.halcyonashes.minimala.core.system.expandStatusBar
import io.github.halcyonashes.minimala.theme.LocalLauncherPalette
import io.github.halcyonashes.minimala.theme.wallpaperScrim
import io.github.halcyonashes.minimala.ui.LauncherUiState
import io.github.halcyonashes.minimala.ui.drawer.AppDrawer
import io.github.halcyonashes.minimala.ui.home.HomePanel
import io.github.halcyonashes.minimala.ui.left.LeftPanel
import io.github.halcyonashes.minimala.ui.right.RightPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val HomePage = 1
private const val PageCount = 3

private const val DrawerAnimationMillis = 220

/**
 * Fling speed (dp/s) above which the drawer commits in the direction of the flick regardless of
 * how far it travelled. Without this, only a slow drag past the halfway mark could open it — a
 * quick upward flick, which is how the drawer is actually opened in practice, would spring back.
 */
private const val DrawerFlingVelocityDp = 400f

/**
 * Three-panel pager (left/home/right) with the app drawer as a drag-driven overlay,
 * not a fourth page — a diagonal swipe into a fourth page would break "HOME always
 * returns to center", so the drawer lives above the pager instead.
 */
@Composable
fun LauncherShell(
    uiState: LauncherUiState,
    resetSignal: Int,
    onLaunchApp: (AppEntry) -> Unit,
    onToggleFavorite: (AppEntry) -> Unit,
    onToggleFrequent: (AppEntry) -> Unit,
    onToggleHidden: (AppEntry) -> Unit,
    onRenameApp: (AppEntry, String) -> Unit,
    onOpenAppInfo: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateEvent: (String) -> Unit,
) {
    val palette = LocalLauncherPalette.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = HomePage) { PageCount }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        // Scrim rather than a fixed opaque fill: the window is transparent with showWallpaper
        // set, so how much wallpaper survives is the user's Background setting — at 1f this is
        // opaque and the wallpaper is hidden entirely, the "fully dark" end of the range.
        modifier = Modifier
            .fillMaxSize()
            .background(palette.wallpaperScrim(uiState.settings.backgroundOpacity)),
    ) {
        val density = LocalDensity.current.density
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Single source of truth for drawer position: heightPx = closed, 0 = fully open.
        val drawerOffset = remember { Animatable(Float.MAX_VALUE) }
        LaunchedEffect(heightPx) {
            if (drawerOffset.value > heightPx) drawerOffset.snapTo(heightPx)
        }

        val offset = drawerOffset.value.coerceAtMost(heightPx)
        val drawerVisible = offset < heightPx

        val settleDrawer: (Boolean) -> Unit = { open ->
            scope.launch {
                drawerOffset.animateTo(if (open) 0f else heightPx, tween(DrawerAnimationMillis))
            }
        }

        LaunchedEffect(resetSignal) {
            if (resetSignal == 0) return@LaunchedEffect
            // A HOME press must return to a known state: centre panel, drawer shut.
            drawerOffset.animateTo(heightPx, tween(DrawerAnimationMillis))
            pagerState.animateScrollToPage(HomePage)
        }

        HorizontalPager(
            state = pagerState,
            key = { it },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            // The drag detector lives INSIDE the page, deliberately. Compose dispatches the
            // main pointer pass child-first, so a detector here sees a vertical drag before the
            // pager's horizontal scrollable does and can consume it. Attached to the pager's own
            // modifier it would instead be the scrollable's parent, receive the change already
            // consumed, and cancel its own slop detection — the drawer would never open.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawerDrag(
                        scope = scope,
                        drawerOffset = drawerOffset,
                        heightPx = heightPx,
                        onSettle = settleDrawer,
                        // Ignores touches once the drawer is up. Compose hit-tests every node
                        // whose bounds contain the touch, with no occlusion, so this detector
                        // still receives events through the drawer covering it — and on the
                        // Initial pass it would win them, stealing the drawer's own gestures.
                        //
                        // Checked here, per gesture, rather than by dropping the modifier when
                        // the drawer opens: removing a pointer node mid-drag cancels the very
                        // gesture that is opening the drawer, so the drawer never opens at all.
                        enabled = { drawerOffset.value >= heightPx },
                        // Only the home page forwards a swipe-down to the status bar — the
                        // drawer only ever opens from home, so left/right keep swiping down
                        // as a dead gesture rather than surprising the user elsewhere.
                        onSwipeDownConfirmed = if (page == HomePage) {
                            { context.expandStatusBar() }
                        } else {
                            null
                        },
                    ),
            ) {
                when (page) {
                    0 -> LeftPanel(uiState = uiState, onOpenSettings = onOpenSettings)
                    1 -> HomePanel(uiState = uiState, onLaunchApp = onLaunchApp)
                    2 -> RightPanel(uiState = uiState, onLaunchApp = onLaunchApp, onCreateEvent = onCreateEvent)
                }
            }
        }

        // Always composed, even fully closed — translationY simply parks it one full screen
        // below the viewport, so it never overlaps and never hit-tests while closed. Composing
        // it only once drawerVisible flips true would mean its first composition and layout
        // pass — the search field, the LazyColumn — lands mid-drag, on the very first pixel of
        // an opening swipe, which is the frame hitch that made opening feel worse than closing
        // (closing only ever translates an already-live subtree, never mounts one).
        BackHandler(enabled = drawerVisible) { settleDrawer(false) }

        val dismissConnection = remember(heightPx, density) {
            DrawerDismissConnection(
                scope = scope,
                drawerOffset = drawerOffset,
                heightPx = heightPx,
                flingThresholdPxPerSecond = DrawerFlingVelocityDp * density,
                onSettle = settleDrawer,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = offset }
                // Nested scroll is what makes "pull down anywhere to close" work: the app
                // list gets the drag first, and only the part it cannot use — because it is
                // already at the top — comes back here and moves the drawer.
                .nestedScroll(dismissConnection)
                // Main pass, unlike the home-panel detector: children get first refusal, so
                // this catches drags on the header and search field while leaving scrolls
                // that start inside the list to the list.
                .drawerDrag(
                    scope = scope,
                    drawerOffset = drawerOffset,
                    heightPx = heightPx,
                    onSettle = settleDrawer,
                    pass = PointerEventPass.Main,
                ),
        ) {
            AppDrawer(
                apps = uiState.visibleApps,
                renames = uiState.settings.appRenames,
                favoriteKeys = uiState.settings.favoritePackageKeys.toSet(),
                frequentKeys = uiState.settings.frequentAppKeys.toSet(),
                onLaunchApp = { entry ->
                    onLaunchApp(entry)
                    settleDrawer(false)
                },
                onToggleFavorite = onToggleFavorite,
                onToggleFrequent = onToggleFrequent,
                onToggleHidden = onToggleHidden,
                onRename = onRenameApp,
                onOpenAppInfo = onOpenAppInfo,
            )
        }
    }
}

/**
 * Vertical drag that moves [drawerOffset] between 0 (open) and [heightPx] (closed).
 *
 * Settles on fling velocity first and nearest edge otherwise, which is what makes a short flick
 * work; only vertical movement is consumed, so horizontal swipes still reach the pager beneath.
 * The same rule serves both directions, so opening and closing stay symmetric.
 *
 * Direction is decided on [PointerEventPass.Initial] rather than the default Main pass. Compose
 * delivers Main pass child-first, so a plain detectVerticalDragGestures here still loses the race
 * to any clickable label underneath it (a favorite app, a quick-settings row) whenever the swipe
 * happens to start on top of one — the open gesture would silently miss. Initial pass is
 * dispatched parent-first, so this detector gets first refusal on every touch and can consume a
 * confirmed vertical drag before any descendant clickable ever sees the move.
 */
private fun Modifier.drawerDrag(
    scope: CoroutineScope,
    drawerOffset: Animatable<Float, AnimationVector1D>,
    heightPx: Float,
    onSettle: (Boolean) -> Unit,
    pass: PointerEventPass = PointerEventPass.Initial,
    enabled: () -> Boolean = { true },
    onSwipeDownConfirmed: (() -> Unit)? = null,
) = pointerInput(heightPx, pass) {
    val flingThresholdPxPerSecond = DrawerFlingVelocityDp * density
    val touchSlop = viewConfiguration.touchSlop

    awaitEachGesture {
        val down = awaitFirstDown(pass = pass)
        if (!enabled()) return@awaitEachGesture
        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        var dragged = drawerOffset.value.coerceAtMost(heightPx)
        var confirmedVertical = false

        // Slop is measured against travel accumulated since the finger went down, NOT against
        // one frame's delta. A single frame of a deliberate, unhurried drag is only a few px,
        // so a per-frame test only ever fired on a fast flick — which is why the drawer used to
        // need several swipes before one happened to be violent enough to register.
        var travel = Offset.Zero

        while (true) {
            val event = awaitPointerEvent(pass = pass)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            val delta = change.positionChange()
            velocityTracker.addPosition(change.uptimeMillis, change.position)

            if (!confirmedVertical) {
                travel += delta
                val verticalEnough = kotlin.math.abs(travel.y) > touchSlop &&
                    kotlin.math.abs(travel.y) > kotlin.math.abs(travel.x)
                if (verticalEnough) {
                    // A confirmed downward drag never opens the drawer — enabled() only lets
                    // this run while the drawer is already closed, so downward has nothing to
                    // drag towards. Hand it to the status bar instead of dragging in place.
                    if (travel.y > 0f && onSwipeDownConfirmed != null) {
                        change.consume()
                        onSwipeDownConfirmed()
                        return@awaitEachGesture
                    }
                    confirmedVertical = true
                    change.consume()
                } else if (kotlin.math.abs(travel.x) > touchSlop) {
                    // Clearly horizontal — leave it untouched for the pager and everything else.
                    return@awaitEachGesture
                }
            } else {
                change.consume()
            }

            if (confirmedVertical) {
                dragged = (dragged + delta.y).coerceIn(0f, heightPx)
                scope.launch { drawerOffset.snapTo(dragged) }
            }
        }

        if (confirmedVertical) {
            val velocityY = velocityTracker.calculateVelocity().y
            onSettle(
                when {
                    velocityY <= -flingThresholdPxPerSecond -> true
                    velocityY >= flingThresholdPxPerSecond -> false
                    else -> dragged < heightPx / 2f
                },
            )
        }
    }
}

/**
 * Lets the app list hand its leftover pull back to the drawer.
 *
 * A drag inside the list belongs to the list, so the dismiss detector deliberately does not
 * take it. Once the list is scrolled to the top it can no longer use a downward drag, and that
 * unconsumed remainder arrives here as [onPostScroll] — which is what turns "pull down over the
 * apps" into "close the drawer" without ever fighting the list for the gesture.
 *
 * [onPreScroll] handles the reverse: while the drawer is part-way dismissed, an upward drag has
 * to put it back before the list may scroll, otherwise one continuous gesture would change
 * meaning half way through.
 */
private class DrawerDismissConnection(
    private val scope: CoroutineScope,
    private val drawerOffset: Animatable<Float, AnimationVector1D>,
    private val heightPx: Float,
    private val flingThresholdPxPerSecond: Float,
    private val onSettle: (Boolean) -> Unit,
) : NestedScrollConnection {

    /**
     * Offset as this connection last requested it. [Animatable.snapTo] is applied from a
     * coroutine, so [Animatable.value] can still be a frame behind when the next scroll event
     * arrives; driving from that lagging value would drop part of a fast drag.
     */
    private var requested = heightPx

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0f) return Offset.Zero
        return if (position() > 0f) drag(available.y) else Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        return drag(available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val current = position()
        if (current <= 0f) return Velocity.Zero
        onSettle(
            when {
                available.y <= -flingThresholdPxPerSecond -> true
                available.y >= flingThresholdPxPerSecond -> false
                else -> current < heightPx / 2f
            },
        )
        requested = heightPx
        // Consume the whole fling: it belongs to the drawer settling, not to the list.
        return available
    }

    private fun position(): Float {
        val settled = drawerOffset.value <= 0f || drawerOffset.value >= heightPx
        return (if (settled) drawerOffset.value else requested).coerceIn(0f, heightPx)
    }

    private fun drag(delta: Float): Offset {
        val start = position()
        val target = (start + delta).coerceIn(0f, heightPx)
        requested = target
        scope.launch { drawerOffset.snapTo(target) }
        return Offset(0f, target - start)
    }
}
