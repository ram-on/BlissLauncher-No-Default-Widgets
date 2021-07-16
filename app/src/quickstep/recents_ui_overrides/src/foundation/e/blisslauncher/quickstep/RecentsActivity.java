/*
 * Copyright (C) 2019 The Android Open Source Project
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
package foundation.e.blisslauncher.quickstep;

import static com.android.systemui.shared.system.RemoteAnimationTargetCompat.MODE_CLOSING;
import static foundation.e.blisslauncher.QuickstepAppTransitionManagerImpl.RECENTS_LAUNCH_DURATION;
import static foundation.e.blisslauncher.QuickstepAppTransitionManagerImpl.STATUS_BAR_TRANSITION_DURATION;
import static foundation.e.blisslauncher.QuickstepAppTransitionManagerImpl.STATUS_BAR_TRANSITION_PRE_DELAY;
import static foundation.e.blisslauncher.quickstep.TaskUtils.taskIsATargetWithMode;
import static foundation.e.blisslauncher.quickstep.TaskViewUtils.getRecentsWindowAnimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationRunnerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.LauncherAnimationRunner;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.features.test.BaseDragLayer;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.Interpolators;
import foundation.e.blisslauncher.quickstep.fallback.FallbackRecentsView;
import foundation.e.blisslauncher.quickstep.fallback.RecentsRootView;
import foundation.e.blisslauncher.quickstep.util.ClipAnimationHelper;
import foundation.e.blisslauncher.quickstep.views.RecentsView;
import foundation.e.blisslauncher.quickstep.views.TaskView;

/**
 * A recents activity that shows the recently launched tasks as swipable task cards.
 * See {@link RecentsView}.
 */
public final class RecentsActivity extends BaseRecentsActivity {

    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private RecentsRootView mRecentsRootView;
    private FallbackRecentsView mFallbackRecentsView;

    @Override
    protected void initViews() {
        setContentView(R.layout.fallback_recents_activity);
        mRecentsRootView = findViewById(R.id.drag_layer);
        mFallbackRecentsView = findViewById(R.id.overview_panel);
        mRecentsRootView.setup();
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

    @Override
    protected void onHandleConfigChanged() {
        super.onHandleConfigChanged();
        mRecentsRootView.setup();
    }

    @Override
    protected void reapplyUi() {
        mRecentsRootView.dispatchInsets();
    }

    @Override
    protected VariantDeviceProfile createDeviceProfile() {
        VariantDeviceProfile dp = BlissLauncher.getApplication(this).getInvariantDeviceProfile().getDeviceProfile(this);
        return (mRecentsRootView != null) && isInMultiWindowMode()
                ? dp.getMultiWindowProfile(this, mRecentsRootView.getLastKnownSize())
                : super.createDeviceProfile();
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
                AnimatorSet anim = composeRecentsLaunchAnimator(taskView, targetCompats);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFallbackRecentsView.resetViewUI();
                    }
                });
                result.setAnimation(anim);
            }
        };
        return ActivityOptionsCompat.makeRemoteAnimation(new RemoteAnimationAdapterCompat(
                runner, RECENTS_LAUNCH_DURATION,
                RECENTS_LAUNCH_DURATION - STATUS_BAR_TRANSITION_DURATION
                        - STATUS_BAR_TRANSITION_PRE_DELAY));
    }

    /**
     * Composes the animations for a launch from the recents list if possible.
     */
    private AnimatorSet composeRecentsLaunchAnimator(TaskView taskView,
            RemoteAnimationTargetCompat[] targets) {
        AnimatorSet target = new AnimatorSet();
        boolean activityClosing = taskIsATargetWithMode(targets, getTaskId(), MODE_CLOSING);
        ClipAnimationHelper helper = new ClipAnimationHelper(this);
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
        mFallbackRecentsView.resetTaskVisuals();
    }

    public void onTaskLaunched() {
        mFallbackRecentsView.resetTaskVisuals();
    }
}
