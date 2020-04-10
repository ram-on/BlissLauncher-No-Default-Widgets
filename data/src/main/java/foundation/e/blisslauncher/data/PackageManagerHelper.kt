package foundation.e.blisslauncher.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.UserHandle
import android.text.TextUtils
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.domain.entity.ApplicationItem

class PackageManagerHelper(
    context: Context,
    private val launcherApps: LauncherAppsCompat
) {
    private val pm = context.packageManager

    val isSafeMode
        get() = pm.isSafeMode

    /**
     * Returns true if the app can possibly be on the SDCard. This is just a workaround and doesn't
     * guarantee that the app is on SD card.
     */
    fun isAppOnSdcard(
        packageName: String,
        user: UserHandle
    ): Boolean {
        val info = launcherApps.getApplicationInfo(
            packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, user
        )
        return info != null && info.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
    }

    /**
     * Returns whether the target app is suspended for a given user as per
     * [android.app.admin.DevicePolicyManager.isPackageSuspended].
     */
    fun isAppSuspended(
        packageName: String,
        user: UserHandle
    ): Boolean {
        val info = launcherApps.getApplicationInfo(packageName, 0, user)
        return info != null && info.flags and ApplicationInfo.FLAG_SUSPENDED != 0
    }

    fun isAppSuspended(info: ApplicationInfo): Boolean =
        info.flags and ApplicationInfo.FLAG_SUSPENDED != 0

    fun getAppLaunchIntent(pkg: String, user: UserHandle): Intent? {
        val activities = launcherApps.getActivityList(pkg, user)
        return if (activities.isEmpty()) null else
            ApplicationItem.makeLaunchIntent(activities[0])
    }

    /**
     * Returns true if {@param srcPackage} has the permission required to start the activity from
     * {@param intent}. If {@param srcPackage} is null, then the activity should not need
     * any permissions
     */
    fun hasPermissionForActivity(
        intent: Intent?,
        srcPackage: String?
    ): Boolean {
        val target: ResolveInfo = pm.resolveActivity(intent, 0)
            ?: // Not a valid target
            return false
        if (TextUtils.isEmpty(target.activityInfo.permission)) {
            // No permission is needed
            return true
        }
        if (TextUtils.isEmpty(srcPackage)) {
            // The activity requires some permission but there is no source.
            return false
        }

        // Source does not have sufficient permissions.
        if (pm.checkPermission(target.activityInfo.permission, srcPackage) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        // On M and above also check AppOpsManager for compatibility mode permissions.
        if (TextUtils.isEmpty(AppOpsManager.permissionToOp(target.activityInfo.permission))) {
            // There is no app-op for this permission, which could have been disabled.
            return true
        }

        // There is no direct way to check if the app-op is allowed for a particular app. Since
        // app-op is only enabled for apps running in compatibility mode, simply block such apps.
        try {
            return pm.getApplicationInfo(
                srcPackage,
                0
            ).targetSdkVersion >= Build.VERSION_CODES.M
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }
}