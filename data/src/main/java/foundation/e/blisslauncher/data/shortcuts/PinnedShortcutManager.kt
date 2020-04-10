package foundation.e.blisslauncher.data.shortcuts

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.domain.keys.ShortcutKey
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages shortcuts, such as querying for them, pinning them, etc.
 */
class PinnedShortcutManager @Inject constructor(
    private val context: Context
) {
    private var wasLastCallSuccess = false
    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun wasLastCallSuccess() = wasLastCallSuccess

    /**
     * Removes the given shortcut from the current list of pinned shortcuts.
     * (Runs on background thread)
     */
    @TargetApi(25)
    fun unpinShortcut(key: ShortcutKey) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            val packageName = key.componentName.getPackageName()
            val id = key.getId()
            val user = key.user
            val pinnedIds =
                extractIds(queryForPinnedShortcuts(packageName, user))
            pinnedIds.remove(id)
            try {
                launcherApps.pinShortcuts(packageName, pinnedIds, user)
                wasLastCallSuccess = true
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to unpin shortcut")
                wasLastCallSuccess = false
            } catch (e: IllegalStateException) {
                Timber.e(e, "Failed to unpin shortcut")
                wasLastCallSuccess = false
            }
        }
    }

    /**
     * Adds the given shortcut to the current list of pinned shortcuts.
     * (Runs on background thread)
     */
    @TargetApi(25)
    fun pinShortcut(key: ShortcutKey) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            val packageName = key.componentName.getPackageName()
            val id = key.getId()
            val user = key.user
            val pinnedIds =
                extractIds(queryForPinnedShortcuts(packageName, user))
            pinnedIds.add(id)
            try {
                launcherApps.pinShortcuts(packageName, pinnedIds, user)
                wasLastCallSuccess = true
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to pin shortcut")
                wasLastCallSuccess = false
            } catch (e: IllegalStateException) {
                Timber.e(e, "Failed to pin shortcut")
                wasLastCallSuccess = false
            }
        }
    }

    @TargetApi(25)
    fun getShortcutIconDrawable(shortcutInfo: ShortcutInfoCompat, density: Int): Drawable? {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            try {
                val icon: Drawable = launcherApps.getShortcutIconDrawable(
                    shortcutInfo.getShortcutInfo(), density
                )
                wasLastCallSuccess = true
                return icon
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to get shortcut icon")
                wasLastCallSuccess = false
            } catch (e: java.lang.IllegalStateException) {
                Timber.e(e, "Failed to get shortcut icon")
                wasLastCallSuccess = false
            }
        }
        return null
    }

    fun queryForPinnedShortcuts(
        packageName: String?,
        user: UserHandle?
    ): List<ShortcutInfoCompat> {
        return query(ShortcutQuery.FLAG_MATCH_PINNED, packageName, null, null, user)
    }

    @TargetApi(25)
    private fun query(
        flags: Int,
        packageName: String?,
        activity: ComponentName?,
        shortcutIds: List<String>?,
        user: UserHandle?
    ): List<ShortcutInfoCompat> {
        return if (Utilities.ATLEAST_NOUGAT_MR1) {
            val q = ShortcutQuery()
            q.setQueryFlags(flags)
            if (packageName != null) {
                q.setPackage(packageName)
                q.setActivity(activity)
                q.setShortcutIds(shortcutIds)
            }
            var shortcutInfos: List<ShortcutInfo>? = null
            try {
                shortcutInfos = launcherApps.getShortcuts(q, user)
                wasLastCallSuccess = true
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to query for shortcuts")
                wasLastCallSuccess = false
            } catch (e: java.lang.IllegalStateException) {
                Timber.e(e, "Failed to query for shortcuts")
                wasLastCallSuccess = false
            }
            shortcutInfos?.map { ShortcutInfoCompat(it) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    @TargetApi(25)
    fun hasHostPermission(): Boolean {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            try {
                return launcherApps.hasShortcutHostPermission()
            } catch (e: SecurityException) {
                Timber.e(e, "Failed to make shortcut manager call")
            } catch (e: java.lang.IllegalStateException) {
                Timber.e(e, "Failed to make shortcut manager call")
            }
        }
        return false
    }

    private fun extractIds(shortcuts: List<ShortcutInfoCompat>): MutableList<String> {
        return shortcuts.map { it.getId() }.toMutableList()
    }
}