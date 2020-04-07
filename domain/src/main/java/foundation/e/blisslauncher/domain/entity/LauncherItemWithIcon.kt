package foundation.e.blisslauncher.domain.entity

import android.content.Intent
import android.graphics.Bitmap

/**
 * Represents a LauncherItem which also have an icon.
 */
abstract class LauncherItemWithIcon : LauncherItem {

    /**
     * A bitmap version of the application icon.
     */
    var iconBitmap: Bitmap? = null

    /**
     * Dominant color in the [.iconBitmap].
     */
    var iconColor = 0

    /**
     * Indicates whether we're using a low res icon
     */
    var usingLowResIcon = false

    /**
     * Status associated with the system state of the underlying item. This is calculated every
     * time a new info is created and not persisted on the disk.
     */
    var runtimeStatusFlags = 0

    constructor()

    constructor(item: LauncherItemWithIcon) : super(item) {
        iconBitmap = item.iconBitmap
        iconColor = item.iconColor
        usingLowResIcon = item.usingLowResIcon
        runtimeStatusFlags = item.runtimeStatusFlags
    }

    override fun isDisabled(): Boolean = (runtimeStatusFlags and FLAG_DISABLED_MASK) != 0

    companion object {
        /**
         * Indicates that the icon is disabled due to safe mode restrictions.
         */
        const val FLAG_DISABLED_SAFEMODE = 1 shl 0

        /**
         * Indicates that the icon is disabled as the app is not available.
         */
        const val FLAG_DISABLED_NOT_AVAILABLE = 1 shl 1

        /**
         * Indicates that the icon is disabled as the app is suspended
         */
        const val FLAG_DISABLED_SUSPENDED = 1 shl 2

        /**
         * Indicates that the icon is disabled as the user is in quiet mode.
         */
        const val FLAG_DISABLED_QUIET_USER = 1 shl 3

        /**
         * Indicates that the icon is disabled as the publisher has disabled the actual shortcut.
         */
        const val FLAG_DISABLED_BY_PUBLISHER = 1 shl 4

        /**
         * Indicates that the icon is disabled as the user partition is currently locked.
         */
        const val FLAG_DISABLED_LOCKED_USER = 1 shl 5

        const val FLAG_DISABLED_MASK = FLAG_DISABLED_SAFEMODE or
            FLAG_DISABLED_NOT_AVAILABLE or FLAG_DISABLED_SUSPENDED or
            FLAG_DISABLED_QUIET_USER or FLAG_DISABLED_BY_PUBLISHER or FLAG_DISABLED_LOCKED_USER

        /**
         * The item points to a system app.
         */
        const val FLAG_SYSTEM_YES = 1 shl 6

        /**
         * The item points to a non system app.
         */
        const val FLAG_SYSTEM_NO = 1 shl 7

        const val FLAG_SYSTEM_MASK = FLAG_SYSTEM_YES or FLAG_SYSTEM_NO

        /**
         * Flag indicating that the icon is an [android.graphics.drawable.AdaptiveIconDrawable]
         * that can be optimized in various way.
         */
        const val FLAG_ADAPTIVE_ICON = 1 shl 8

        /**
         * Flag indicating that the icon is badged.
         */
        const val FLAG_ICON_BADGED = 1 shl 9
    }
}