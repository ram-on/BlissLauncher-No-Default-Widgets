package foundation.e.blisslauncher.domain.keys

import android.os.UserHandle
import android.service.notification.StatusBarNotification
import foundation.e.blisslauncher.domain.entity.LauncherItem

class PackageUserKey(var packageName: String?, var user: UserHandle?) {
    private val hashCode: Int = arrayOf(packageName, user).contentHashCode()

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (other !is PackageUserKey) return false
        return other.packageName == packageName && other.user == user
    }

    /**
     * This should only be called to avoid new object creations in a loop.
     * @return Whether this PackageUserKey was successfully updated - it shouldn't be used if not.
     */
    fun updateFromItemInfo(info: LauncherItem): Boolean {
        /*if (DeepShortcutManager.supportsShortcuts(info)) {
            update(info.getTargetComponent().getPackageName(), info.user)
            return true
        }
        return false*/
        TODO()
    }

    companion object {
        fun fromLauncherItem(item: LauncherItem): PackageUserKey =
            PackageUserKey(
                item.getTargetComponent()?.packageName,
                item.user
            )

        fun fromNotification(sbn: StatusBarNotification): PackageUserKey =
            PackageUserKey(
                sbn.packageName,
                sbn.user
            )
    }
}