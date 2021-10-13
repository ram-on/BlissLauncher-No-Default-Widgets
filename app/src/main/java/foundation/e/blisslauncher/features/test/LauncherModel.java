/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.e.blisslauncher.features.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.executors.MainThreadExecutor;
import foundation.e.blisslauncher.core.utils.Preconditions;
import foundation.e.blisslauncher.features.shortcuts.InstallShortcutReceiver;

public class LauncherModel extends BroadcastReceiver implements
    OnAppsChangedCallback {

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

    @Override
    public void onPackageRemoved(String packageName, UserHandle user) {
        // TODO: Handle package removed here.
        onPackagesRemoved(user, packageName);
    }

    public void onPackagesRemoved(UserHandle user, String... packages) {
        final HashSet<String> removedPackages = new HashSet<>();
        Collections.addAll(removedPackages, packages);
        if (!removedPackages.isEmpty()) {
            LauncherItemMatcher removeMatch = LauncherItemMatcher.ofPackages(removedPackages, user);
            deleteAndBindComponentsRemoved(removeMatch);

            // Remove any queued items from the install queue
            if (sWorkerThread.getThreadId() == Process.myTid()) {
            } else {
                // If we are not on the worker thread, then post to the worker handler
                sWorker.post(() -> InstallShortcutReceiver
                    .removeFromInstallQueue(mApp.getContext(), removedPackages, user));
            }
        }
    }

    private void deleteAndBindComponentsRemoved(LauncherItemMatcher removeMatch) {
        mCallbacks.get().bindWorkspaceComponentsRemoved(removeMatch);
    }

    @Override
    public void onPackageAdded(String packageName, UserHandle user) {

    }

    @Override
    public void onPackageChanged(String packageName, UserHandle user) {

    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {

    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {

    }

    @Override
    public void onPackagesSuspended(String[] packageNames, UserHandle user) {

    }

    @Override
    public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {

    }

    @Override
    public void onShortcutsChanged(
        String packageName, List<ShortcutInfo> shortcuts, UserHandle user
    ) {

    }

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

        void bindWorkspaceComponentsRemoved(LauncherItemMatcher matcher);
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
            List<LauncherItem> items = new ArrayList<>();
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
