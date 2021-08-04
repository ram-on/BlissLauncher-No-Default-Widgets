package foundation.e.blisslauncher.features.notification

import android.service.notification.StatusBarNotification
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.PackageUserKey
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager
import foundation.e.blisslauncher.features.test.TestActivity
import java.util.HashMap
import java.util.function.Predicate

class NotificationDataProvider(private val mLauncher: TestActivity) : NotificationListener.NotificationsChangedListener {

    /** Maps packages to their DotInfo's .  */
    private val mPackageUserToDotInfos = mutableMapOf<PackageUserKey, DotInfo>()

    private fun updateNotificationDots(updatedDots: Predicate<PackageUserKey>) {
        mLauncher.updateNotificationDots(updatedDots)
    }

    override fun onNotificationPosted(
        postedPackageUserKey: PackageUserKey,
        notificationKey: NotificationKeyData?,
        shouldBeFilteredOut: Boolean
    ) {
        val dotInfo = mPackageUserToDotInfos[postedPackageUserKey]
        val dotShouldBeRefreshed: Boolean
        if (dotInfo == null) {
            dotShouldBeRefreshed = if (!shouldBeFilteredOut) {
                val newDotInfo = DotInfo()
                newDotInfo.addOrUpdateNotificationKey(notificationKey)
                mPackageUserToDotInfos.put(postedPackageUserKey, newDotInfo)
                true
            } else {
                false
            }
        } else {
            dotShouldBeRefreshed =
                if (shouldBeFilteredOut) dotInfo.removeNotificationKey(notificationKey) else dotInfo.addOrUpdateNotificationKey(
                    notificationKey
                )
            if (dotInfo.notificationKeys.size == 0) {
                mPackageUserToDotInfos.remove(postedPackageUserKey)
            }
        }
        if (dotShouldBeRefreshed) {
            updateNotificationDots { t: PackageUserKey? ->
                postedPackageUserKey == t
            }
        }
    }

    override fun onNotificationRemoved(
        removedPackageUserKey: PackageUserKey,
        notificationKey: NotificationKeyData?
    ) {
        val oldDotInfo = mPackageUserToDotInfos[removedPackageUserKey]
        if (oldDotInfo != null && oldDotInfo.removeNotificationKey(notificationKey)) {
            if (oldDotInfo.notificationKeys.size == 0) {
                mPackageUserToDotInfos.remove(removedPackageUserKey)
            }
            updateNotificationDots { t: PackageUserKey? ->
                removedPackageUserKey == t
            }
        }
    }

    override fun onNotificationFullRefresh(activeNotifications: List<StatusBarNotification>?) {
        if (activeNotifications == null) return
        // This will contain the PackageUserKeys which have updated dots.
        val updatedDots = HashMap(mPackageUserToDotInfos)
        mPackageUserToDotInfos.clear()
        for (notification in activeNotifications) {
            val packageUserKey = PackageUserKey.fromNotification(notification)
            var dotInfo = mPackageUserToDotInfos[packageUserKey]
            if (dotInfo == null) {
                dotInfo = DotInfo()
                mPackageUserToDotInfos[packageUserKey] = dotInfo
            }
            dotInfo.addOrUpdateNotificationKey(NotificationKeyData.fromNotification(notification))
        }

        // Add and remove from updatedDots so it contains the PackageUserKeys of updated dots.

        // Add and remove from updatedDots so it contains the PackageUserKeys of updated dots.
        for (packageUserKey in mPackageUserToDotInfos.keys) {
            val prevDot = updatedDots[packageUserKey]
            val newDot = mPackageUserToDotInfos[packageUserKey]
            if (prevDot == null) {
                updatedDots[packageUserKey] = newDot
            } else {
                // No need to update the dot if it already existed (no visual change).
                // Note that if the dot was removed entirely, we wouldn't reach this point because
                // this loop only includes active notifications added above.
                updatedDots.remove(packageUserKey)
            }
        }

        if (updatedDots.isNotEmpty()) {
            updateNotificationDots { key: PackageUserKey? ->
                updatedDots.containsKey(
                    key
                )
            }
        }
    }
    fun getDotInfoForItem(info: LauncherItem?): DotInfo? {
        return if (!DeepShortcutManager.supportsShortcuts(info)) {
            null
        } else mPackageUserToDotInfos[PackageUserKey.fromItemInfo(info)]
    }
}
