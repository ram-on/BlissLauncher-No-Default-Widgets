package foundation.e.blisslauncher.core.utils;

import android.os.UserHandle;
import android.service.notification.StatusBarNotification;

import java.util.Arrays;

import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager;

/** Creates a hash key based on package name and user. */
public class PackageUserKey {

    public String mPackageName;
    public UserHandle mUser;
    private int mHashCode;

    public static PackageUserKey fromItemInfo(LauncherItem info) {
        return new PackageUserKey(info.getTargetComponent().getPackageName(), info.user.getRealHandle());
    }

    public static PackageUserKey fromNotification(StatusBarNotification notification) {
        return new PackageUserKey(notification.getPackageName(), notification.getUser());
    }

    public PackageUserKey(String packageName, UserHandle user) {
        update(packageName, user);
    }

    private void update(String packageName, UserHandle user) {
        mPackageName = packageName;
        mUser = user;
        mHashCode = Arrays.hashCode(new Object[] {packageName, user});
    }

    /**
     * This should only be called to avoid new object creations in a loop.
     * @return Whether this PackageUserKey was successfully updated - it shouldn't be used if not.
     */
    public boolean updateFromItemInfo(LauncherItem info) {
        if (DeepShortcutManager.supportsShortcuts(info)) {
            update(info.getTargetComponent().getPackageName(), info.user.getRealHandle());
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageUserKey)) return false;
        PackageUserKey otherKey = (PackageUserKey) obj;
        return mPackageName.equals(otherKey.mPackageName) && mUser.equals(otherKey.mUser);
    }
}
