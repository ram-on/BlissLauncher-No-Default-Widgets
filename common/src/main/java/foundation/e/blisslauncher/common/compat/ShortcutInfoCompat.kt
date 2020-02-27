package foundation.e.blisslauncher.common.compat

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.UserHandle

/**
 * Wrapper class for [android.content.pm.ShortcutInfo] representing deep shortcuts into apps.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class ShortcutInfoCompat(private val shortcutInfo: ShortcutInfo) {

    @TargetApi(Build.VERSION_CODES.N)
    fun makeIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(INTENT_CATEGORY)
        .setComponent(getActivity())
        .setPackage(getPackage())
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        .putExtra(EXTRA_SHORTCUT_ID, getId())

    fun getShortcutInfo(): ShortcutInfo = shortcutInfo

    fun getPackage(): String = shortcutInfo.`package`

    //@RequiresApi(Build.VERSION_CODES.N_MR1)
    fun getBadgePackage(context: Context): String? {
        val whitelistedPkg = ""
        return if (whitelistedPkg == getPackage() && shortcutInfo.getExtras().containsKey(
                EXTRA_BADGEPKG
            )
        ) {
            shortcutInfo.getExtras()
                .getString(EXTRA_BADGEPKG)
        } else getPackage()
    }

    fun getId(): String = shortcutInfo.id

    fun getShortLabel(): CharSequence? = shortcutInfo.shortLabel

    fun getLongLabel(): CharSequence? = shortcutInfo.longLabel

    fun getLastChangedTimestamp(): Long = shortcutInfo.getLastChangedTimestamp()

    fun getActivity(): ComponentName? = shortcutInfo.getActivity()

    fun getUserHandle(): UserHandle? = shortcutInfo.getUserHandle()

    fun hasKeyFieldsOnly(): Boolean = shortcutInfo.hasKeyFieldsOnly()

    fun isPinned(): Boolean = shortcutInfo.isPinned()

    fun isDeclaredInManifest(): Boolean = shortcutInfo.isDeclaredInManifest()

    fun isEnabled(): Boolean = shortcutInfo.isEnabled()

    fun isDynamic(): Boolean = shortcutInfo.isDynamic()

    fun getRank(): Int = shortcutInfo.getRank()

    fun getDisabledMessage(): CharSequence? = shortcutInfo.getDisabledMessage()

    override fun toString(): String = shortcutInfo.toString()

    companion object {
        private const val INTENT_CATEGORY = "com.android.launcher3.DEEP_SHORTCUT"
        private const val EXTRA_BADGEPKG = "badge_package"
        const val EXTRA_SHORTCUT_ID = "shortcut_id"
    }
}