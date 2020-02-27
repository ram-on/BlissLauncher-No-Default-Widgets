package foundation.e.blisslauncher.common.compat

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.UserHandle

/**
 * Interface repository of [android.content.pm.LauncherApps]
 */
interface LauncherAppsCompat {

    /**
     * Wrapper callback for [android.content.pm.LauncherApps.Callback]
     */
    interface OnAppsChangedCallbackCompat {
        fun onPackageRemoved(
            packageName: String,
            user: UserHandle
        )

        fun onPackageAdded(
            packageName: String,
            user: UserHandle
        )

        fun onPackageChanged(
            packageName: String,
            user: UserHandle
        )

        fun onPackagesAvailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        )

        fun onPackagesUnavailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        )

        fun onPackagesSuspended(
            packageNames: Array<String>,
            user: UserHandle
        )

        fun onPackagesUnsuspended(
            packageNames: Array<String>,
            user: UserHandle
        )

        fun onShortcutsChanged(
            packageName: String,
            shortcuts: List<ShortcutInfoCompat>,
            user: UserHandle
        )
    }

    /**
     * Returns a list of Activities for a given package and user handle.
     *
     * If packageName is null, then it returns all the activities
     * installed on device for the given user.
     */
    fun getActivityList(
        packageName: String?,
        user: UserHandle?
    ): List<LauncherActivityInfo>

    /**
     * @see android.content.pm.LauncherApps.resolveActivity
     */
    fun resolveActivity(
        intent: Intent?,
        user: UserHandle?
    ): LauncherActivityInfo?

    fun startActivityForProfile(
        component: ComponentName?,
        user: UserHandle?,
        sourceBounds: Rect?,
        opts: Bundle?
    )

    /**
     * @see android.content.pm.LauncherApps.getApplicationInfo
     */
    fun getApplicationInfo(
        packageName: String,
        flags: Int,
        user: UserHandle
    ): ApplicationInfo?

    fun showAppDetailsForProfile(
        component: ComponentName?,
        user: UserHandle?,
        sourceBounds: Rect?,
        opts: Bundle?
    )

    fun addOnAppsChangedCallback(listener: OnAppsChangedCallbackCompat)

    fun removeOnAppsChangedCallback(listener: OnAppsChangedCallbackCompat)

    fun isPackageEnabledForProfile(
        packageName: String?,
        user: UserHandle?
    ): Boolean

    fun isActivityEnabledForProfile(
        component: ComponentName?,
        user: UserHandle?
    ): Boolean

    //TODO
    /*abstract fun getCustomShortcutActivityList(
        packageUser: PackageUserKey?
    ): List<ShortcutConfigActivityInfo?>?*/

    fun showAppDetailsForProfile(
        component: ComponentName?,
        user: UserHandle?
    ) {
        showAppDetailsForProfile(component, user, null, null)
    }
}