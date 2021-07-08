/*
 * Copyright (C) 2018 The Android Open Source Project
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

package foundation.e.blisslauncher.features.quickstep;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.utils.PackageManagerHelper;
import foundation.e.blisslauncher.core.utils.UserHandle;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.quickstep.views.TaskThumbnailView;
import foundation.e.blisslauncher.features.quickstep.views.TaskView;
import foundation.e.blisslauncher.features.test.BaseDraggingActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

/**
 * Represents a system shortcut that can be shown for a recent task.
 */
public class TaskSystemShortcut {

    private static final String TAG = "TaskSystemShortcut";

    public final int iconResId;
    public final int labelResId;

    public TaskSystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public View.OnClickListener getOnClickListener(
            BaseDraggingActivity activity, LauncherItem itemInfo) {
        return null;
    }

    public View.OnClickListener getOnClickListener(BaseDraggingActivity activity, TaskView view) {
        Task task = view.getTask();

        ShortcutItem dummyInfo = new ShortcutItem();
        dummyInfo.launchIntent = new Intent();
        ComponentName component = task.getTopComponent();
        dummyInfo.launchIntent.setComponent(component);
        dummyInfo.user = new UserHandle(task.key.userId, android.os.UserHandle.of(task.key.userId));
        dummyInfo.title = TaskUtils.getTitle(activity, task);

        return getOnClickListenerForTask(activity, dummyInfo);
    }

    protected View.OnClickListener getOnClickListenerForTask(
            BaseDraggingActivity activity, LauncherItem dummyInfo) {
        return getOnClickListener(activity, dummyInfo);
    }

    public static class AppInfo extends TaskSystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
            BaseDraggingActivity activity, LauncherItem itemInfo) {
            return (view) -> {
                Rect sourceBounds = activity.getViewBounds(view);
                Bundle opts = activity.getActivityLaunchOptionsAsBundle(view);
                new PackageManagerHelper(activity).startDetailsActivityForInfo(
                    itemInfo, sourceBounds, opts);
            };
        }
    }

    public static class SplitScreen extends TaskSystemShortcut {

        private Handler mHandler;

        public SplitScreen() {
            super(R.drawable.ic_split_screen, R.string.recent_task_option_split_screen);
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, TaskView taskView) {
            if (activity.getDeviceProfile().isMultiWindowMode) {
                return null;
            }
            final Task task  = taskView.getTask();
            final int taskId = task.key.id;
            if (!task.isDockable) {
                return null;
            }
            final RecentsView recentsView = activity.getOverviewPanel();

            final TaskThumbnailView thumbnailView = taskView.getThumbnail();
            return (v -> {
                final View.OnLayoutChangeListener onLayoutChangeListener =
                        new View.OnLayoutChangeListener() {
                            @Override
                            public void onLayoutChange(View v, int l, int t, int r, int b,
                                    int oldL, int oldT, int oldR, int oldB) {
                                taskView.getRootView().removeOnLayoutChangeListener(this);
                                recentsView.removeIgnoreResetTask(taskView);

                                // Start animating in the side pages once launcher has been resized
                                recentsView.dismissTask(taskView, false, false);
                            }
                        };

                final VariantDeviceProfile.OnDeviceProfileChangeListener onDeviceProfileChangeListener =
                        new VariantDeviceProfile.OnDeviceProfileChangeListener() {
                            @Override
                            public void onDeviceProfileChanged(VariantDeviceProfile dp) {
                                activity.removeOnDeviceProfileChangeListener(this);
                                if (dp.isMultiWindowMode) {
                                    taskView.getRootView().addOnLayoutChangeListener(
                                            onLayoutChangeListener);
                                }
                            }
                        };

                AbstractFloatingView.closeOpenViews(activity, true,
                        AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);

                final int navBarPosition = WindowManagerWrapper.getInstance().getNavBarPosition();
                if (navBarPosition == WindowManagerWrapper.NAV_BAR_POS_INVALID) {
                    return;
                }
                boolean dockTopOrLeft = navBarPosition != WindowManagerWrapper.NAV_BAR_POS_LEFT;
                if (ActivityManagerWrapper.getInstance().startActivityFromRecents(taskId,
                        ActivityOptionsCompat.makeSplitScreenOptions(dockTopOrLeft))) {
                    ISystemUiProxy sysUiProxy = RecentsModel.getInstance(activity).getSystemUiProxy();
                    try {
                        sysUiProxy.onSplitScreenInvoked();
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to notify SysUI of split screen: ", e);
                        return;
                    }
                    // Add a device profile change listener to kick off animating the side tasks
                    // once we enter multiwindow mode and relayout
                    activity.addOnDeviceProfileChangeListener(onDeviceProfileChangeListener);

                    final Runnable animStartedListener = () -> {
                        // Hide the task view and wait for the window to be resized
                        // TODO: Consider animating in launcher and do an in-place start activity
                        //       afterwards
                        recentsView.addIgnoreResetTask(taskView);
                        taskView.setAlpha(0f);
                    };

                    final int[] position = new int[2];
                    thumbnailView.getLocationOnScreen(position);
                    final int width = (int) (thumbnailView.getWidth() * taskView.getScaleX());
                    final int height = (int) (thumbnailView.getHeight() * taskView.getScaleY());
                    final Rect taskBounds = new Rect(position[0], position[1],
                            position[0] + width, position[1] + height);

                    Bitmap thumbnail = RecentsTransition.drawViewIntoHardwareBitmap(
                            taskBounds.width(), taskBounds.height(), thumbnailView, 1f,
                            Color.BLACK);
                    AppTransitionAnimationSpecsFuture future =
                            new AppTransitionAnimationSpecsFuture(mHandler) {
                        @Override
                        public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                            return Collections.singletonList(new AppTransitionAnimationSpecCompat(
                                    taskId, thumbnail, taskBounds));
                        }
                    };
                    WindowManagerWrapper.getInstance().overridePendingAppTransitionMultiThumbFuture(
                            future, animStartedListener, mHandler, true /* scaleUp */);
                }
            });
        }
    }

    public static class Pin extends TaskSystemShortcut {

        private static final String TAG = Pin.class.getSimpleName();

        private Handler mHandler;

        public Pin() {
            super(R.drawable.ic_pin, R.string.recent_task_option_pin);
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, TaskView taskView) {
            ISystemUiProxy sysUiProxy = RecentsModel.getInstance(activity).getSystemUiProxy();
            if (sysUiProxy == null) {
                return null;
            }
            if (!ActivityManagerWrapper.getInstance().isScreenPinningEnabled()) {
                return null;
            }
            if (ActivityManagerWrapper.getInstance().isLockToAppActive()) {
                // We shouldn't be able to pin while an app is locked.
                return null;
            }
            return view -> {
                Consumer<Boolean> resultCallback = success -> {
                    if (success) {
                        try {
                            sysUiProxy.startScreenPinning(taskView.getTask().key.id);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Failed to start screen pinning: ", e);
                        }
                    } else {
                        taskView.notifyTaskLaunchFailed(TAG);
                    }
                };
                taskView.launchTask(true, resultCallback, mHandler);
            };
        }
    }

}
