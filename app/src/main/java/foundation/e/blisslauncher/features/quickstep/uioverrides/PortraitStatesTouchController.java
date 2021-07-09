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
package foundation.e.blisslauncher.features.quickstep.uioverrides;

import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_VERTICAL_PROGRESS;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.LINEAR;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import foundation.e.blisslauncher.core.touch.AbstractStateChangeTouchController;
import foundation.e.blisslauncher.core.touch.SwipeDetector;
import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.quickstep.RecentsModel;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.quickstep.views.TaskView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.Interpolators;

/**
 * Touch controller for handling various state transitions in portrait UI.
 */
public class PortraitStatesTouchController extends AbstractStateChangeTouchController {

    private static final String TAG = "PortraitStatesTouchCtrl";

    private InterpolatorWrapper mAllAppsInterpolatorWrapper = new InterpolatorWrapper();

    // If true, we will finish the current animation instantly on second touch.
    private boolean mFinishFastOnSecondTouch;


    public PortraitStatesTouchController(TestActivity l) {
        super(l, SwipeDetector.VERTICAL);
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (mCurrentAnimation != null) {
            if (mFinishFastOnSecondTouch) {
                // TODO: Animate to finish instead.
                mCurrentAnimation.getAnimationPlayer().end();
            }

            // If we are already animating from a previous state, we can intercept.
            return true;
        }
        // For all other states, only listen if the event originated below the hotseat height
        VariantDeviceProfile dp = mLauncher.getDeviceProfile();
        int hotseatHeight = dp.getHotseatBarSizePx() + dp.getInsets().bottom;
        if (ev.getY() < (mLauncher.getDragLayer().getHeight() - hotseatHeight)) {
            return false;
        }

        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false;
        }
        return true;
    }

    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        if (fromState == OVERVIEW) {
            return NORMAL;
        } else if (fromState == NORMAL && isDragTowardPositive) {
            return OVERVIEW;
        }
        return fromState;
    }

    private AnimatorSetBuilder getNormalToOverviewAnimation() {
        mAllAppsInterpolatorWrapper.baseInterpolator = LINEAR;

        AnimatorSetBuilder builder = new AnimatorSetBuilder();
        builder.setInterpolator(ANIM_VERTICAL_PROGRESS, mAllAppsInterpolatorWrapper);

        return builder;
    }

    @Override
    protected float initCurrentAnimation(@LauncherStateManager.AnimationComponents int animComponents) {
        float range = getShiftRange();
        long maxAccuracy = (long) (2 * range);

        float startVerticalShift = 1f * range;
        float endVerticalShift = 1f * range;

        float totalShift = endVerticalShift - startVerticalShift;

        final AnimatorSetBuilder builder;

        if (mFromState == NORMAL && mToState == OVERVIEW && totalShift != 0) {
            builder = getNormalToOverviewAnimation();
        } else {
            builder = new AnimatorSetBuilder();
        }

        cancelPendingAnim();

        RecentsView recentsView = mLauncher.getOverviewPanel();
        TaskView taskView = (TaskView) recentsView.getChildAt(recentsView.getNextPage());
        if (recentsView.shouldSwipeDownLaunchApp() && mFromState == OVERVIEW && mToState == NORMAL
                && taskView != null) {
            mPendingAnimation = recentsView.createTaskLauncherAnimation(taskView, maxAccuracy);
            mPendingAnimation.anim.setInterpolator(Interpolators.ZOOM_IN);

            Runnable onCancelRunnable = () -> {
                cancelPendingAnim();
                clearState();
            };
            mCurrentAnimation = AnimatorPlaybackController.wrap(mPendingAnimation.anim, maxAccuracy,
                    onCancelRunnable);
            mLauncher.getStateManager().setCurrentUserControlledAnimation(mCurrentAnimation);
        } else {
            mCurrentAnimation = mLauncher.getStateManager()
                    .createAnimationToNewWorkspace(mToState, builder, maxAccuracy, this::clearState,
                            animComponents);
        }

        if (totalShift == 0) {
            totalShift = Math.signum(mFromState.ordinal - mToState.ordinal)
                    * 200;
        }
        return 1 / totalShift;
    }

    private float getShiftRange() {
        return mLauncher.getDeviceProfile().getHeightPx();
    }

    private void cancelPendingAnim() {
        if (mPendingAnimation != null) {
            mPendingAnimation.finish(false);
            mPendingAnimation = null;
        }
    }

    @Override
    protected void updateSwipeCompleteAnimation(ValueAnimator animator, long expectedDuration,
            LauncherState targetState, float velocity, boolean isFling) {
        super.updateSwipeCompleteAnimation(animator, expectedDuration, targetState,
                velocity, isFling);
        handleFirstSwipeToOverview(animator, expectedDuration, targetState, velocity, isFling);
    }

    private void handleFirstSwipeToOverview(final ValueAnimator animator,
            final long expectedDuration, final LauncherState targetState, final float velocity,
            final boolean isFling) {
        if (mFromState == NORMAL && mToState == OVERVIEW && targetState == OVERVIEW) {
            mFinishFastOnSecondTouch = true;
            if (isFling && expectedDuration != 0) {
                // Update all apps interpolator to add a bit of overshoot starting from currFraction
                final float currFraction = mCurrentAnimation.getProgressFraction();
                mAllAppsInterpolatorWrapper.baseInterpolator = Interpolators.clampToProgress(
                        new OvershootInterpolator(Math.min(Math.abs(velocity), 3f)), currFraction, 1);
                animator.setDuration(Math.min(expectedDuration, ATOMIC_DURATION))
                        .setInterpolator(LINEAR);
            }
        } else {
            mFinishFastOnSecondTouch = false;
        }
    }

    @Override
    protected void onSwipeInteractionCompleted(LauncherState targetState) {
        super.onSwipeInteractionCompleted(targetState);
        if (mStartState == NORMAL && targetState == OVERVIEW) {
            RecentsModel.getInstance(mLauncher).onOverviewShown(true, TAG);
        }
    }

    private static class InterpolatorWrapper implements Interpolator {

        public TimeInterpolator baseInterpolator = LINEAR;

        @Override
        public float getInterpolation(float v) {
            return baseInterpolator.getInterpolation(v);
        }
    }
}
