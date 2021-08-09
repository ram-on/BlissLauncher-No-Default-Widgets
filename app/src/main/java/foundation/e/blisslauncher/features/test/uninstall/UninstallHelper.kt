package foundation.e.blisslauncher.features.test.uninstall

import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import android.util.ArrayMap
import foundation.e.blisslauncher.features.test.Alarm
import foundation.e.blisslauncher.features.test.OnAlarmListener

object UninstallHelper : OnAlarmListener {
    private const val CACHE_EXPIRE_TIMEOUT: Long = 5000
    private val mUninstallDisabledCache = ArrayMap<UserHandle, Boolean>(1)

    private val mCacheExpireAlarm: Alarm = Alarm()

    init {
        mCacheExpireAlarm.setOnAlarmListener(this)
    }

    fun isUninstallDisabled(user: UserHandle, context: Context): Boolean {
        var uninstallDisabled = mUninstallDisabledCache[user]
        if (uninstallDisabled == null) {
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val restrictions = userManager.getUserRestrictions(user)
            uninstallDisabled =
                (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false) ||
                    restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false))
            mUninstallDisabledCache[user] = uninstallDisabled
        }

        // Cancel any pending alarm and set cache expiry after some time
        mCacheExpireAlarm.setAlarm(CACHE_EXPIRE_TIMEOUT)
        return uninstallDisabled
    }

    override fun onAlarm(alarm: Alarm?) {
        mUninstallDisabledCache.clear()
    }
}
