package foundation.e.blisslauncher.domain.entity

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ShortcutIconResource

/**
 * Represents an item in workspace and inside folder.
 * It can be an Application, Shortcut or Deep Shortcut.
 * Also used for pinned and dynamic shortcuts of the apps.
 */
open class ShortcutItem : LauncherItemWithIcon {

    /**
     * The intent used to start the application.
     */
    @get:JvmName("_getIntent")
    var intent: Intent? = null

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    var iconResource: ShortcutIconResource? = null

    /**
     * A message to display when the user tries to start a disabled shortcut.
     * This is currently only used for deep shortcuts.
     */
    var disabledMessage: CharSequence? = null

    var status = 0

    /**
     * The installation progress [0-100] of the package that this shortcut represents.
     */
    private var installProgress = 0
        set(value) {
            status = status or FLAG_INSTALL_SESSION_ACTIVE
            field = value
        }

    constructor() {
        itemType = LauncherConstants.ItemType.SHORTCUT
    }

    constructor(item: ShortcutItem) : super(item) {
        title = item.title
        intent = item.getIntent()
        iconResource = item.iconResource
        status = item.status
        installProgress = item.installProgress
    }

    override fun getIntent(): Intent? {
        return intent
    }

    fun hasStatusFlag(flag: Int) = status and flag != 0

    fun isPromise() = hasStatusFlag(FLAG_RESTORED_ICON or FLAG_AUTOINSTALL_ICON)

    fun hasPromiseIconUi(): Boolean = isPromise() && !hasStatusFlag(FLAG_SUPPORTS_WEB_UI)

    override fun getTargetComponent(): ComponentName? {
        val cn = super.getTargetComponent()
        if (cn == null && (itemType == LauncherConstants.ItemType.SHORTCUT ||
                hasStatusFlag(FLAG_SUPPORTS_WEB_UI))
        ) {
            // Legacy shortcuts and promise icons with web UI may not have a componentName but just
            // a packageName. In that case create a dummy componentName instead of adding additional
            // check everywhere.
            val pkg: String? = intent?.getPackage()
            return if (pkg == null) null else ComponentName(pkg, ".")
        }
        return cn
    }

    companion object {
        val DEFAULT = 0

        /**
         * The shortcut was restored from a backup and it not ready to be used. This is automatically
         * set during backup/restore
         */
        const val FLAG_RESTORED_ICON = 1

        /**
         * The icon was added as an auto-install app, and is not ready to be used. This flag can't
         * be present along with [.FLAG_RESTORED_ICON], and is set during default layout
         * parsing.
         */
        const val FLAG_AUTOINSTALL_ICON = 2 //0B10;

        /**
         * The icon is being installed. If [.FLAG_RESTORED_ICON] or [.FLAG_AUTOINSTALL_ICON]
         * is set, then the icon is either being installed or is in a broken state.
         */
        const val FLAG_INSTALL_SESSION_ACTIVE = 4 // 0B100;

        /**
         * Indicates that the widget restore has started.
         */
        const val FLAG_RESTORE_STARTED = 8 //0B1000;

        /**
         * Web UI supported.
         */
        const val FLAG_SUPPORTS_WEB_UI = 16 //0B10000;
    }
}