package foundation.e.blisslauncher.features.notification;


import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import foundation.e.blisslauncher.core.utils.ListUtil;

/**
 * Created by falcon on 14/3/18.
 */
@TargetApi(Build.VERSION_CODES.O)
public class NotificationService extends NotificationListenerService {

    NotificationRepository mNotificationRepository;

    private static final int MSG_NOTIFICATION_POSTED = 1;
    private static final int MSG_NOTIFICATION_REMOVED = 2;
    private static final int MSG_NOTIFICATION_FULL_REFRESH = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationRepository = NotificationRepository.getNotificationRepository();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onListenerConnected() {
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
