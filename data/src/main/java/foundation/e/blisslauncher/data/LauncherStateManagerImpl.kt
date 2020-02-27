package foundation.e.blisslauncher.data

import android.content.Context
import android.os.Process
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.data.notification.NotificationListener
import foundation.e.blisslauncher.data.receiver.ConfigChangedReceiver
import foundation.e.blisslauncher.data.receiver.ProfileReceiver
import foundation.e.blisslauncher.domain.manager.LauncherStateManager
import foundation.e.blisslauncher.domain.repository.UserManagerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherStateManagerImpl @Inject constructor(
    private val context: Context,
    private val profileReceiver: ProfileReceiver,
    private val userManagerRepository: UserManagerRepository,
    private val configReceiver: ConfigChangedReceiver,
    private val onAppsChangedCallbackCompat: LauncherAppsCompat.OnAppsChangedCallbackCompat,
    private val appExecutors: AppExecutors,
    private val launcherAppsCompat: LauncherAppsCompat
) : LauncherStateManager {
    private lateinit var notificationBadgingObserver: SettingsObserver

    override fun init() {
        Timber.d("Initialising Launcher components")

        launcherAppsCompat.addOnAppsChangedCallback(onAppsChangedCallbackCompat)

        profileReceiver.register()
        userManagerRepository.enableAndResetCache()
        configReceiver.register()
        notificationBadgingObserver = object : SettingsObserver.Secure(context.contentResolver) {
            override fun onSettingChanged(isNotificationBadgingEnabled: Boolean) {
                if (isNotificationBadgingEnabled) {
                    NotificationListener.requestRebind(context)
                }
            }
        }
        notificationBadgingObserver.register(NotificationListener.NOTIFICATION_BADGING)
        Timber.d("Initialisation completed")
    }

    override fun terminate() {
        launcherAppsCompat.removeOnAppsChangedCallback(onAppsChangedCallbackCompat)
        profileReceiver.unregister()
        configReceiver.unregister()
        notificationBadgingObserver.unregister()
    }

    fun changeThreadPriority(priority: Int) {
        appExecutors.io.execute {
            Process.setThreadPriority(priority)
        }
    }
}