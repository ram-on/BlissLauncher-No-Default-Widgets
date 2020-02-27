package foundation.e.blisslauncher.data.compat

import android.annotation.TargetApi
import android.content.Context
import android.os.Build

@TargetApi(Build.VERSION_CODES.N_MR1)
open class UserManagerCompatVNMr1(context: Context) : UserManagerCompatVN(context) {

    override val isDemoUser: Boolean
        get() = userManager.isDemoUser
}
