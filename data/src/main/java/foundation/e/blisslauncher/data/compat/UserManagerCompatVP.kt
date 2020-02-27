package foundation.e.blisslauncher.data.compat

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.UserHandle

@TargetApi(Build.VERSION_CODES.P)
open class UserManagerCompatVP(context: Context) : UserManagerCompatVNMr1(context) {

    override fun requestQuietModeEnabled(enableQuietMode: Boolean, user: UserHandle?): Boolean =
        userManager.requestQuietModeEnabled(enableQuietMode, user)
}
