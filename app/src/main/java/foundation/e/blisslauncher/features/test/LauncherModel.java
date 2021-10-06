package foundation.e.blisslauncher.features.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.telecom.Call;
import android.util.Log;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.executors.MainThreadExecutor;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.Preconditions;
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager;

public class LauncherModel extends BroadcastReceiver {

    private static final boolean DEBUG_RECEIVER = false;

    static final String TAG = "Launcher.Model";

    private final MainThreadExecutor mUiExecutor = new MainThreadExecutor();
   final LauncherAppState mApp;
    final Object mLock = new Object();

    WeakReference<Callbacks> mCallbacks;

    static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    private static final Looper mWorkerLooper;
    static {
        sWorkerThread.start();
        mWorkerLooper = sWorkerThread.getLooper();
    }
    static final Handler sWorker = new Handler(mWorkerLooper);

    // Runnable to check if the shortcuts permission has changed.
    /*private final Runnable mShortcutPermissionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (mModelLoaded) {
                boolean hasShortcutHostPermission =
                    DeepShortcutManager.getInstance(mApp.getContext()).hasHostPermission();
                if (hasShortcutHostPermission != sBgDataModel.hasShortcutHostPermission) {
                    forceReload();
                }
            }
        }
    };*/

    public interface Callbacks {
        void bindAppsAdded(List<LauncherItem> items);
    }

    LauncherModel(LauncherAppState app) {
        mApp = app;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive intent=" + intent);
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            Preconditions.assertUIThread();
            mCallbacks = new WeakReference<>(callbacks);
        }
    }

    /**
     * Adds the provided items to the workspace.
     */
    public void addAndBindAddedWorkspaceItems(List<Pair<LauncherItem, Object>> itemList) {
        Callbacks callbacks = getCallback();
        if (callbacks != null) {
            //callbacks.preAddApps();
            List<LauncherItem> items =  new ArrayList<>();
            for (Pair<LauncherItem, Object> entry : itemList) {
                items.add(entry.first);
            }
            mUiExecutor.execute(() -> callbacks.bindAppsAdded(items));
        }

    }

    public Callbacks getCallback() {
        return mCallbacks != null ? mCallbacks.get() : null;
    }

    /**
     * @return the looper for the worker thread which can be used to start background tasks.
     */
    public static Looper getWorkerLooper() {
        return mWorkerLooper;
    }

    public static void setWorkerPriority(final int priority) {
        Process.setThreadPriority(sWorkerThread.getThreadId(), priority);
    }
}
