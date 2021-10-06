package foundation.e.blisslauncher.features.test;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.Toast;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager;

public abstract class BaseDraggingActivity extends BaseActivity {

    private static final String TAG = "BaseDraggingActivity";

    // The Intent extra that defines whether to ignore the launch animation
    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
        "com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // When starting an action mode, setting this tag will cause the action mode to be cancelled
    // automatically when user interacts with the launcher.
    public static final Object AUTO_CANCEL_ACTION_MODE = new Object();

    private ActionMode mCurrentActionMode;
    protected boolean mIsSafeModeEnabled;

    private OnStartCallback mOnStartCallback;

    private int mThemeRes = R.style.HomeScreenTheme;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        setTheme(mThemeRes);
    }

    protected int getThemeRes() {
        return mThemeRes;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mCurrentActionMode = mode;
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mCurrentActionMode = null;
    }

    public boolean finishAutoCancelActionMode() {
        if (mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == mCurrentActionMode.getTag()) {
            mCurrentActionMode.finish();
            return true;
        }
        return false;
    }

    public abstract BaseDragLayer getDragLayer();

    public abstract <T extends View> T getOverviewPanel();

    public abstract View getRootView();

/*    public abstract BadgeInfo getBadgeInfoForItem(ItemInfo info);

    public abstract void invalidateParent(ItemInfo info);*/

    public static BaseDraggingActivity fromContext(Context context) {
        if (context instanceof BaseDraggingActivity) {
            return (BaseDraggingActivity) context;
        }
        return ((BaseDraggingActivity) ((ContextWrapper) context).getBaseContext());
    }

    public Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    public final Bundle getActivityLaunchOptionsAsBundle(View v) {
        ActivityOptions activityOptions = getActivityLaunchOptions(v);
        return activityOptions == null ? null : activityOptions.toBundle();
    }

    public abstract ActivityOptions getActivityLaunchOptions(View v);

    public boolean startActivitySafely(View v, Intent intent, LauncherItem item) {
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Only launch using the new animation if the shortcut has not opted out (this is a
        // private contract between launcher and may be ignored in the future).
        boolean useLaunchAnimation = (v != null) &&
            !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
        Bundle optsBundle = useLaunchAnimation
            ? getActivityLaunchOptionsAsBundle(v)
            : null;

        UserHandle user = item == null ? null : item.user.getRealHandle();

        // Prepare intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }
        try {
            boolean isShortcut = Utilities.ATLEAST_MARSHMALLOW
                && (item instanceof ShortcutItem)
                && (item.itemType == Constants.ITEM_TYPE_SHORTCUT);
            if (isShortcut) {
                // Shortcuts need some special checks due to legacy reasons.
                startShortcutIntentSafely(intent, optsBundle, item);
            } else if (user == null || user.equals(Process.myUserHandle())) {
                // Could be launching some bookkeeping activity
                startActivity(intent, optsBundle);
            } else {
                LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                launcherApps.startAppDetailsActivity(
                    intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
        }
        return false;
    }

    private void startShortcutIntentSafely(Intent intent, Bundle optsBundle, LauncherItem info) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                    .penaltyLog().build());

                if (info.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                    String id = ((ShortcutItem) info).id;
                    String packageName = intent.getPackage();
                    DeepShortcutManager.getInstance(this).startShortcut(
                        packageName, id, intent.getSourceBounds(), optsBundle, info.user.getRealHandle());
                } else {
                    // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            if (!onErrorStartingShortcut(intent, info)) {
                throw e;
            }
        }
    }

    protected boolean onErrorStartingShortcut(Intent intent, LauncherItem info) {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mOnStartCallback != null) {
            mOnStartCallback.onActivityStart(this);
            mOnStartCallback = null;
        }
    }

    public <T extends BaseDraggingActivity> void setOnStartCallback(OnStartCallback<T> callback) {
        mOnStartCallback = callback;
    }

    protected void onDeviceProfileInitiated() {
        //TODO: Enable or disable rotation listener here if supporting landscape launcher.
    }

    protected abstract void reapplyUi();

    /**
     * Callback for listening for onStart
     */
    public interface OnStartCallback<T extends BaseDraggingActivity> {

        void onActivityStart(T activity);
    }
}
