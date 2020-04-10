package foundation.e.blisslauncher.domain.keys

import android.content.ComponentName
import android.content.Intent
import android.os.UserHandle
import foundation.e.blisslauncher.common.compat.ShortcutInfoCompat
import foundation.e.blisslauncher.domain.entity.LauncherItem

class ShortcutKey(componentName: ComponentName, user: UserHandle) :
    ComponentKey(componentName, user) {

    constructor(packageName: String, user: UserHandle, id: String) : this(
        ComponentName(
            packageName,
            id
        ), user
    )

    fun getId() = componentName.className

    companion object {
        fun fromShortcutInfoCompat(shortcutInfo: ShortcutInfoCompat): ShortcutKey = ShortcutKey(
            shortcutInfo.getPackage(), shortcutInfo.getUserHandle(), shortcutInfo.getId()
        )

        fun fromIntent(intent: Intent, user: UserHandle) = ShortcutKey(
            intent.`package`!!,
            user,
            intent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID)
        )

        fun fromLauncherItem(item: LauncherItem) = fromIntent(item.getIntent()!!, item.user)
    }
}