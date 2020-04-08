package foundation.e.blisslauncher.domain.entity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.os.Build
import android.os.Process
import android.os.UserHandle
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.domain.keys.ComponentKey

/**
 * Represents an app in Launcher Workspace
 */
open class ApplicationItem : AppShortcutItem {

    /**
     * The intent used to start the application.
     */
    lateinit var componentName: ComponentName

    constructor() : super() {
        itemType = LauncherConstants.ItemType.APPLICATION
    }

    constructor(info: LauncherActivityInfo, user: UserHandle, quietModeEnabled: Boolean) {
        this.componentName = info.componentName
        this.container = NO_ID.toLong()
        this.user = user
        this.title = info.label
        intent = makeLaunchIntent(componentName)

        if (quietModeEnabled) {
            runtimeStatusFlags = runtimeStatusFlags or FLAG_DISABLED_QUIET_USER
        }
        updateRuntimeFlagsForActivityTarget(this, info)
    }

    override fun getIntent(): Intent? = intent

    fun toComponentKey(): ComponentKey =
        ComponentKey(
            componentName,
            user
        )

    companion object {
        fun makeLaunchIntent(info: LauncherActivityInfo) = makeLaunchIntent(info.componentName)

        fun makeLaunchIntent(cn: ComponentName): Intent {
            return Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        fun updateRuntimeFlagsForActivityTarget(
            info: LauncherItemWithIcon,
            lai: LauncherActivityInfo
        ) {
            val appInfo = lai.applicationInfo
            /*if (PackageManagerHelper.isAppSuspended(appInfo)) {
                info.runtimeStatusFlags = info.runtimeStatusFlags or FLAG_DISABLED_SUSPENDED
            }*/
            info.runtimeStatusFlags =
                info.runtimeStatusFlags or if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) FLAG_SYSTEM_NO else FLAG_SYSTEM_YES
            if (Utilities.ATLEAST_OREO &&
                appInfo.targetSdkVersion >= Build.VERSION_CODES.O && Process.myUserHandle() == lai.user
            ) { // The icon for a non-primary user is badged, hence it's not exactly an adaptive icon.
                info.runtimeStatusFlags = info.runtimeStatusFlags or FLAG_ADAPTIVE_ICON
            }
        }
    }
}