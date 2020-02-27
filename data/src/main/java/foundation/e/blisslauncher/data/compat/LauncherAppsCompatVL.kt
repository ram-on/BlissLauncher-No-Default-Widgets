package foundation.e.blisslauncher.data.compat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.util.ArrayMap
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import java.util.ArrayList

open class LauncherAppsCompatVL internal constructor(protected val context: Context) :
    LauncherAppsCompat {

    protected val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val callbacks =
        ArrayMap<LauncherAppsCompat.OnAppsChangedCallbackCompat, WrappedCallback>()

    override fun getActivityList(
        packageName: String?,
        user: UserHandle?
    ): List<LauncherActivityInfo> {
        return launcherApps.getActivityList(packageName, user)
    }

    override fun resolveActivity(
        intent: Intent?,
        user: UserHandle?
    ): LauncherActivityInfo? {
        return launcherApps.resolveActivity(intent, user)
    }

    override fun startActivityForProfile(
        component: ComponentName?,
        user: UserHandle?,
        sourceBounds: Rect?,
        opts: Bundle?
    ) {
        launcherApps.startMainActivity(component, user, sourceBounds, opts)
    }

    override fun getApplicationInfo(
        packageName: String,
        flags: Int,
        user: UserHandle
    ): ApplicationInfo? {
        val isPrimaryUser = Process.myUserHandle() == user
        if (!isPrimaryUser && flags == 0) {
            // We are looking for an installed app on a secondary profile. Prior to O, the only
            // entry point for work profiles is through the LauncherActivity.
            val activityList =
                launcherApps.getActivityList(packageName, user)
            return if (activityList.size > 0) activityList[0].applicationInfo else null
        }
        return try {
            val info =
                context.packageManager.getApplicationInfo(packageName, flags)
            // There is no way to check if the app is installed for managed profile. But for
            // primary profile, we can still have this check.
            if (isPrimaryUser && info.flags and ApplicationInfo.FLAG_INSTALLED == 0 ||
                !info.enabled
            ) {
                null
            } else info
        } catch (e: PackageManager.NameNotFoundException) { // Package not found
            null
        }
    }

    override fun showAppDetailsForProfile(
        component: ComponentName?,
        user: UserHandle?,
        sourceBounds: Rect?,
        opts: Bundle?
    ) {
        launcherApps.startAppDetailsActivity(component, user, sourceBounds, opts)
    }

    override fun addOnAppsChangedCallback(listener: LauncherAppsCompat.OnAppsChangedCallbackCompat) {
        val wrappedCallback = WrappedCallback(listener)
        synchronized(callbacks) { callbacks.put(listener, wrappedCallback) }
        launcherApps.registerCallback(wrappedCallback)
    }

    override fun removeOnAppsChangedCallback(listener: LauncherAppsCompat.OnAppsChangedCallbackCompat) {
        var wrappedCallback: WrappedCallback?
        synchronized(callbacks) { wrappedCallback = callbacks.remove(listener) }
        if (wrappedCallback != null) {
            launcherApps.unregisterCallback(wrappedCallback)
        }
    }

    override fun isPackageEnabledForProfile(
        packageName: String?,
        user: UserHandle?
    ): Boolean {
        return launcherApps.isPackageEnabled(packageName, user)
    }

    override fun isActivityEnabledForProfile(
        component: ComponentName?,
        user: UserHandle?
    ): Boolean {
        return launcherApps.isActivityEnabled(component, user)
    }

    private class WrappedCallback(private val mCallback: LauncherAppsCompat.OnAppsChangedCallbackCompat) :
        LauncherApps.Callback() {
        override fun onPackageRemoved(
            packageName: String,
            user: UserHandle
        ) {
            mCallback.onPackageRemoved(packageName, user)
        }

        override fun onPackageAdded(
            packageName: String,
            user: UserHandle
        ) {
            mCallback.onPackageAdded(packageName, user)
        }

        override fun onPackageChanged(
            packageName: String,
            user: UserHandle
        ) {
            mCallback.onPackageChanged(packageName, user)
        }

        override fun onPackagesAvailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            mCallback.onPackagesAvailable(packageNames, user, replacing)
        }

        override fun onPackagesUnavailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            mCallback.onPackagesUnavailable(packageNames, user, replacing)
        }

        override fun onPackagesSuspended(
            packageNames: Array<String>,
            user: UserHandle
        ) {
            mCallback.onPackagesSuspended(packageNames, user)
        }

        override fun onPackagesUnsuspended(
            packageNames: Array<String>,
            user: UserHandle
        ) {
            mCallback.onPackagesUnsuspended(packageNames, user)
        }

        override fun onShortcutsChanged(
            packageName: String,
            shortcuts: List<ShortcutInfo>,
            user: UserHandle
        ) {
            val shortcutInfoCompats: MutableList<ShortcutInfoCompat> =
                ArrayList(
                    shortcuts.size
                )
            for (shortcutInfo in shortcuts) {
                shortcutInfoCompats.add(
                    ShortcutInfoCompat(
                        shortcutInfo
                    )
                )
            }
            mCallback.onShortcutsChanged(packageName, shortcutInfoCompats, user)
        }
    }

    /*@Override
    public List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(
            @Nullable PackageUserKey packageUser) {
        List<ShortcutConfigActivityInfo> result = new ArrayList<>();
        if (packageUser != null && !packageUser.mUser.equals(Process.myUserHandle())) {
            return result;
        }
        PackageManager pm = mContext.getPackageManager();
        for (ResolveInfo info :
                pm.queryIntentActivities(new Intent(Intent.ACTION_CREATE_SHORTCUT), 0)) {
            if (packageUser == null || packageUser.mPackageName
                    .equals(info.activityInfo.packageName)) {
                result.add(new ShortcutConfigActivityInfoVL(info.activityInfo, pm));
            }
        }
        return result;
    }*/
}