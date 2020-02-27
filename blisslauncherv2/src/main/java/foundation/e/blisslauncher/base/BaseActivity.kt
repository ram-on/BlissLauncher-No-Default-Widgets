package foundation.e.blisslauncher.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.IntDef
import foundation.e.blisslauncher.common.util.SystemUiController
import javax.inject.Inject

open class BaseActivity : Activity() {

    /*val dpChangeListeners = ArrayList<DeviceProfile.OnDeviceProfileChangeListener>()

    @Inject
    lateinit var deviceProfile: DeviceProfile*/

    @Inject
    lateinit var systemUiController: SystemUiController

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true,
        value = [ACTIVITY_STATE_STARTED, ACTIVITY_STATE_RESUMED, ACTIVITY_STATE_USER_ACTIVE]
    )
    annotation class ActivityFlags

    @ActivityFlags
    private var activityFlags: Int = 0

    val isStarted: Boolean
        get() = activityFlags and ACTIVITY_STATE_STARTED != 0

    val hasBeenResumed: Boolean
        get() = activityFlags and ACTIVITY_STATE_RESUMED != 0

    override fun onStart() {
        activityFlags = activityFlags or ACTIVITY_STATE_STARTED
        super.onStart()
    }

    override fun onResume() {
        activityFlags = activityFlags or ACTIVITY_STATE_RESUMED or ACTIVITY_STATE_USER_ACTIVE
        super.onResume()
    }

    override fun onUserLeaveHint() {
        activityFlags = activityFlags and ACTIVITY_STATE_USER_ACTIVE.inv()
        super.onUserLeaveHint()
    }

    override fun onPause() {
        activityFlags = activityFlags and ACTIVITY_STATE_RESUMED.inv()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        activityFlags =
            activityFlags and ACTIVITY_STATE_STARTED.inv() and ACTIVITY_STATE_USER_ACTIVE.inv()
    }

    protected fun dispatchDeviceProfileChanged() {
        //dpChangeListeners.forEach { it.onDeviceProfileChanged(deviceProfile) }
    }

    companion object {
        private const val ACTIVITY_STATE_STARTED = 1 shl 0
        private const val ACTIVITY_STATE_RESUMED = 1 shl 1
        private const val ACTIVITY_STATE_USER_ACTIVE = 1 shl 2

        fun fromContext(context: Context): BaseActivity =
            if (context is BaseActivity) context
            else ((context as ContextWrapper).baseContext) as BaseActivity
    }
}