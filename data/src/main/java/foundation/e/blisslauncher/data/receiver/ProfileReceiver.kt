package foundation.e.blisslauncher.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserHandle
import foundation.e.blisslauncher.domain.interactor.ChangeUserAvailability
import foundation.e.blisslauncher.domain.interactor.ChangeUserLockState
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileReceiver @Inject constructor(
    private val context: Context,
    private val changeUserAvailability: ChangeUserAvailability,
    private val changeUserLockState: ChangeUserLockState,
    private val userManager: UserManagerRepository
) :
    BroadcastReceiver() {

    fun register() {
        // Register intent receivers
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        // For handling managed profiles
        // For handling managed profiles
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)

        context.registerReceiver(this, filter)
    }

    fun unregister() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (DEBUG_RECEIVER) Timber.d("onReceive intent=$intent")

        when (val action = intent.action) {
            Intent.ACTION_LOCALE_CHANGED -> TODO("Force reload here") // If locale has been changed, clear out all the labels in workspace
            Intent.ACTION_MANAGED_PROFILE_ADDED, Intent.ACTION_MANAGED_PROFILE_REMOVED -> {
                userManager.enableAndResetCache()
                TODO("Force reload here")
            }
            Intent.ACTION_MANAGED_PROFILE_AVAILABLE, Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE, Intent.ACTION_MANAGED_PROFILE_UNLOCKED -> {
                val user = intent.getParcelableExtra<UserHandle>(Intent.EXTRA_USER)
                if (user != null) {
                    if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE == action || Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE == action) {
                        changeUserAvailability(user)
                        // TODO
                    }

                    // ACTION_MANAGED_PROFILE_UNAVAILABLE sends the profile back to locked mode, so
                    // we need to run the state change task again.
                    // ACTION_MANAGED_PROFILE_UNAVAILABLE sends the profile back to locked mode, so
                    // we need to run the state change task again.
                    if (Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE == action || Intent.ACTION_MANAGED_PROFILE_UNLOCKED == action) {
                        changeUserLockState(user)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileReceiver"
        private const val DEBUG_RECEIVER = true
    }
}