package foundation.e.blisslauncher.data.icon

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconProvider @Inject constructor(context: Context) {
    private var mSystemState: String = ""

    init {
        updateSystemStateString(context)
    }

    fun updateSystemStateString(context: Context) {
        val locale: String = context.resources.configuration.locales.toLanguageTags()
        mSystemState = locale + "," + Build.VERSION.SDK_INT
    }

    fun getIconSystemState(packageName: String): String {
        return mSystemState
    }

    /**
     * @param flattenDrawable true if the caller does not care about the specification of the
     * original icon as long as the flattened version looks the same.
     */
    fun getIcon(
        info: LauncherActivityInfo,
        iconDpi: Int,
        flattenDrawable: Boolean
    ): Drawable = info.getIcon(iconDpi)
}
