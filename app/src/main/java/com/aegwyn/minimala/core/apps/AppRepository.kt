package com.aegwyn.minimala.core.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wraps [LauncherApps] to expose the live launchable-app list across all user profiles
 * (including the work profile). Registers a callback instead of polling PackageManager,
 * which is the standard launcher-performance mistake.
 */
class AppRepository(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback)
        refresh()
    }

    /**
     * Always off the main thread: [LauncherApps.getActivityList] is a binder round-trip per
     * profile and returns hundreds of entries on a real device. Doing it inline would stall
     * Application.onCreate, and the callbacks below fire on the main looper.
     */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            _apps.value = loadAll()
        }
    }

    private fun loadAll(): List<AppEntry> =
        userManager.userProfiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                AppEntry(
                    componentName = info.componentName,
                    user = user,
                    label = info.label.toString(),
                )
            }
        }.sortedBy { it.label.lowercase() }

    /**
     * The entry may be stale — the app can be uninstalled between the list snapshot and the
     * tap, which throws. A launcher crashing on that leaves the user with no home screen, so
     * failures resync the list instead of propagating.
     */
    fun launch(entry: AppEntry, sourceBounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(entry.componentName, entry.user, sourceBounds, null) }
            .onFailure { refresh() }
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.componentName, entry.user, null, null) }
            .onFailure { refresh() }
    }
}
