package foundation.e.blisslauncher.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import foundation.e.blisslauncher.data.SettingsObserver
import io.reactivex.subjects.Subject

/**
 * Created by falcon on 14/3/18.
 */
class NotificationListener : NotificationListenerService() {

    private lateinit var notificationBadgingObserver: SettingsObserver

    val tempRanking = Ranking()

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        isCreated = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isCreated = false
    }

    override fun onListenerConnected() {
        isConnected = true

        notificationBadgingObserver = object : SettingsObserver.Secure(contentResolver) {
            override fun onSettingChanged(isNotificationBadgingEnabled: Boolean) {
                if (!isNotificationBadgingEnabled) {
                    requestUnbind()
                }
            }
        }
        notificationBadgingObserver.register(NOTIFICATION_BADGING)
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        notificationBadgingObserver.unregister()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateNotifications()
    }

    fun getListenerIfConnected(): NotificationListener? {
        return if (isConnected) instance else null
    }

    fun observeNotifications(behaviourSubject: Subject<List<String>>) {
        /*subject = behaviourSubject
        val notificationListener = getListenerIfConnected()
        if (notificationListener != null) {
            updateNotifications()
        } else if (!isCreated) {
            subject.onNext(emptyList())
        }*/
    }

    private fun updateNotifications() {
        if (isSubjectInitialised()) {
            if (isConnected) {
                try {
                    subject.onNext(filterNotifications(activeNotifications).map { it.packageName })
                } catch (e: SecurityException) {
                    Log.e(
                        TAG,
                        "SecurityException: failed to fetch notifications"
                    )
                    subject.onNext(emptyList())
                }
            }
            subject.onNext(emptyList())
        }
    }

    private fun filterNotifications(notifications: Array<StatusBarNotification>?): List<StatusBarNotification> {
        if (notifications == null) return emptyList()
        return notifications.filter { shouldBeFilteredOut(it) }
    }

    private fun shouldBeFilteredOut(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        currentRanking.getRanking(sbn.key, tempRanking)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!tempRanking.canShowBadge())
                return true

            if (tempRanking.channel.id == NotificationChannel.DEFAULT_CHANNEL_ID) {
                if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
                    return true
                }
            }
        }

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        val missingTitleAndText = title.isNullOrEmpty() and text.isNullOrEmpty()
        val isGroupHeader = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return isGroupHeader or missingTitleAndText
    }

    companion object {
        private var isCreated: Boolean = false
        private var isConnected: Boolean = false
        private var instance: NotificationListener? = null

        private lateinit var subject: Subject<List<String>>

        const val NOTIFICATION_BADGING = "notification_badging"

        private const val TAG = "NotificationListener"

        private fun isSubjectInitialised() = ::subject.isInitialized

        fun requestRebind(context: Context) {
            requestRebind(ComponentName(context, NotificationListener::class.java))
        }
    }
}