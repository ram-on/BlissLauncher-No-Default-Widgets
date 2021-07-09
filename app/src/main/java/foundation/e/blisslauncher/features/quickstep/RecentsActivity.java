/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.systemui.shared.system.RemoteAnimationTargetCompat.MODE_CLOSING;
import static foundation.e.blisslauncher.features.quickstep.LauncherAppTransitionManagerImpl.RECENTS_LAUNCH_DURATION;
import static foundation.e.blisslauncher.features.quickstep.LauncherAppTransitionManagerImpl.STATUS_BAR_TRANSITION_DURATION;
import static foundation.e.blisslauncher.features.quickstep.TaskUtils.getRecentsWindowAnimator;
import static foundation.e.blisslauncher.features.quickstep.TaskUtils.taskIsATargetWithMode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationRunnerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.features.quickstep.fallback.FallbackRecentsView;
import foundation.e.blisslauncher.features.quickstep.fallback.RecentsRootView;
import foundation.e.blisslauncher.features.quickstep.uioverrides.UiFactory;
import foundation.e.blisslauncher.features.quickstep.util.ClipAnimationHelper;
import foundation.e.blisslauncher.features.quickstep.views.RecentsViewContainer;
import foundation.e.blisslauncher.features.quickstep.views.TaskView;
import foundation.e.blisslauncher.features.test.BaseDragLayer;
import foundation.e.blisslauncher.features.test.BaseDraggingActivity;
import foundation.e.blisslauncher.features.test.InvariantDeviceProfile;
import foundation.e.blisslauncher.features.test.LauncherAnimationRunner;
import foundation.e.blisslauncher.features.test.LauncherAppState;
import foundation.e.blisslauncher.features.test.SystemUiController;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.Interpolators;

/**
 * A simple activity to show the recently launched tasks
 */
public class RecentsActivity extends BaseDraggingActivity {

    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private RecentsRootView mRecentsRootView;
    private FallbackRecentsView mFallbackRecentsView;
    private RecentsViewContainer mOverviewPanelContainer;

    private Configuration mOldConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOldConfig = new Configuration(getResources().getConfiguration());
        initDeviceProfile();

        setContentView(R.layout.fallback_recents_activity);
        mRecentsRootView = findViewById(R.id.drag_layer);
        mFallbackRecentsView = findViewById(R.id.overview_panel);
        mOverviewPanelContainer = findViewById(R.id.overview_panel_container);

        mRecentsRootView.setup();

        getSystemUiController().updateUiState(
            SystemUiController.UI_STATE_BASE_WINDOW,
                false);
        RecentsActivityTracker.onRecentsActivityCreate(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            onHandleConfigChanged();
        }
        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        onHandleConfigChanged();
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
    }

    public void onRootViewSizeChanged() {
        if (isInMultiWindowMode()) {
            onHandleConfigChanged();
        }
    }

    private void onHandleConfigChanged() {
        initDeviceProfile();

        AbstractFloatingView.closeOpenViews(this, true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);
        dispatchDeviceProfileChanged();

        mRecentsRootView.setup();
        reapplyUi();
    }

    @Override
    protected void reapplyUi() {
        mRecentsRootView.dispatchInsets();
    }

    private void initDeviceProfile() {
        // In case we are reusing IDP, create a copy so that we dont conflict with Launcher
        // activity.
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (isInMultiWindowMode()) {
            InvariantDeviceProfile idp = appState == null
                    ? new InvariantDeviceProfile(this) : appState.getInvariantDeviceProfile();
            VariantDeviceProfile dp = idp.getDeviceProfile(this);
            mDeviceProfile = mRecentsRootView == null ? dp.copy(this)
                    : dp.getMultiWindowProfile(this, mRecentsRootView.getLastKnownSize());
        } else {
            // If we are reusing the Invariant device profile, make a copy.
            mDeviceProfile = appState == null
                    ? new InvariantDeviceProfile(this).getDeviceProfile(this)
                    : appState.getInvariantDeviceProfile().getDeviceProfile(this).copy(this);
        }
        onDeviceProfileInitiated();
    }

    @Override
    public BaseDragLayer getDragLayer() {
        return mRecentsRootView;
    }

    @Override
    public View getRootView() {
        return mRecentsRootView;
    }

    @Override
    public <T extends View> T getOverviewPanel() {
        return (T) mFallbackRecentsView;
    }

    public RecentsViewContainer getOverviewPanelContainer() {
        return mOverviewPanelContainer;
    }

    @Override
    public ActivityOptions getActivityLaunchOptions(final View v) {
        if (!(v instanceof TaskView)) {
            return null;
        }

        final TaskView taskView = (TaskView) v;
        RemoteAnimationRunnerCompat runner = new LauncherAnimationRunner(mUiHandler,
                true /* startAtFrontOfQueue */) {

            @Override
            public void onCreateAnimation(RemoteAnimationTargetCompat[] targetCompats,
                    AnimationResult result) {
                result.setAnimation(composeRecentsLaunchAnimator(taskView, targetCompats));
            }
        };
        return ActivityOptionsCompat.makeRemoteAnimation(new RemoteAnimationAdapterCompat(
                runner, RECENTS_LAUNCH_DURATION,
                RECENTS_LAUNCH_DURATION - STATUS_BAR_TRANSITION_DURATION));
    }

    /**
     * Composes the animations for a launch from the recents list if possible.
     */
    private AnimatorSet composeRecentsLaunchAnimator(TaskView taskView,
            RemoteAnimationTargetCompat[] targets) {
        AnimatorSet target = new AnimatorSet();
        boolean activityClosing = taskIsATargetWithMode(targets, getTaskId(), MODE_CLOSING);
        ClipAnimationHelper helper = new ClipAnimationHelper();
        target.play(getRecentsWindowAnimator(taskView, !activityClosing, targets, helper)
                .setDuration(RECENTS_LAUNCH_DURATION));

        // Found a visible recents task that matches the opening app, lets launch the app from there
        if (activityClosing) {
            Animator adjacentAnimation = mFallbackRecentsView
                    .createAdjacentPageAnimForTaskLaunch(taskView, helper);
            adjacentAnimation.setInterpolator(Interpolators.TOUCH_RESPONSE_INTERPOLATOR);
            adjacentAnimation.setDuration(RECENTS_LAUNCH_DURATION);
            adjacentAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFallbackRecentsView.resetTaskVisuals();
                }
            });
            target.play(adjacentAnimation);
        }
        return target;
    }

    @Override
    protected void onStart() {
        // Set the alpha to 1 before calling super, as it may get set back to 0 due to
        // onActivityStart callback.
        mFallbackRecentsView.setContentAlpha(1);
        super.onStart();
        UiFactory.onStart(this);
        mFallbackRecentsView.resetTaskVisuals();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        UiFactory.onTrimMemory(this, level);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RecentsActivityTracker.onRecentsActivityNewIntent(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecentsActivityTracker.onRecentsActivityDestroy(this);
    }

    @Override
    public void onBackPressed() {
        // TODO: Launch the task we came from
        startHome();
    }

    public void startHome() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
