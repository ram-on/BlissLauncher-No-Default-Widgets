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
package foundation.e.blisslauncher.uioverrides.touchcontrollers;

import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_VERTICAL_PROGRESS;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.LINEAR;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.Interpolators;
import foundation.e.quickstep.OverviewInteractionState;
import foundation.e.quickstep.RecentsModel;

import foundation.e.blisslauncher.uioverrides.states.OverviewState;

import foundation.e.quickstep.util.LayoutUtils;

import foundation.e.blisslauncher.core.touch.AbstractStateChangeTouchController;
import foundation.e.blisslauncher.core.touch.SwipeDetector;
import foundation.e.blisslauncher.features.test.TestActivity;

/**
 * Touch controller for handling various state transitions in portrait UI.
 */
public class PortraitStatesTouchController extends AbstractStateChangeTouchController {

    private static final String TAG = "PortraitStatesTouchCtrl";

    /**
     * The progress at which all apps content will be fully visible when swiping up from overview.
     */
    private static final float ALL_APPS_CONTENT_FADE_THRESHOLD = 0.08f;

    /**
     * The progress at which recents will begin fading out when swiping up from overview.
     */
    private static final float RECENTS_FADE_THRESHOLD = 0.88f;

    private final PortraitOverviewStateTouchHelper mOverviewPortraitStateTouchHelper;

    private final InterpolatorWrapper mAllAppsInterpolatorWrapper = new InterpolatorWrapper();

    private final boolean mAllowDragToOverview;

    // If true, we will finish the current animation instantly on second touch.
    private boolean mFinishFastOnSecondTouch;

    public PortraitStatesTouchController(TestActivity l, boolean allowDragToOverview) {
        super(l, SwipeDetector.VERTICAL);
        mOverviewPortraitStateTouchHelper = new PortraitOverviewStateTouchHelper(l);
        mAllowDragToOverview = allowDragToOverview;
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (mCurrentAnimation != null) {
            if (mFinishFastOnSecondTouch) {
                // TODO: Animate to finish instead.
                mCurrentAnimation.skipToEnd();
            }
            // Otherwise, make sure everything is settled and don't intercept so they can scroll
            // recents, dismiss a task, etc.
            if (mAtomicAnim != null) {
                mAtomicAnim.end();
            }
            return false;
        }
       if (mLauncher.isInState(OVERVIEW)) {
            if (!mOverviewPortraitStateTouchHelper.canInterceptTouch(ev)) {
                return false;
            }
        } else {
            // If we are swiping to all apps instead of overview, allow it from anywhere.
            boolean interceptAnywhere = mLauncher.isInState(NORMAL) && !mAllowDragToOverview;
            // For all other states, only listen if the event originated below the hotseat height
            if (!interceptAnywhere && !isTouchOverHotseat(mLauncher, ev)) {
                return false;
            }
        }
        if (AbstractFloatingView.getTopOpenViewWithType(mLauncher, TYPE_ACCESSIBLE) != null) {
            return false;
        }
        return true;
    }

    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        if (fromState == OVERVIEW) {
            return  NORMAL;
        } else if (fromState == NORMAL && isDragTowardPositive) {
            int stateFlags = OverviewInteractionState.INSTANCE.get(mLauncher)
                    .getSystemUiStateFlags();
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
    protected AnimatorSetBuilder getAnimatorSetBuilderForStates(LauncherState fromState,
            LauncherState toState) {
        AnimatorSetBuilder builder = new AnimatorSetBuilder();
        if (fromState == NORMAL && toState == OVERVIEW) {
            builder = getNormalToOverviewAnimation();
        }
        return builder;
    }

    @Override
    protected float initCurrentAnimation(@LauncherStateManager.AnimationComponents int animComponents) {
        float range = getShiftRange();
        long maxAccuracy = (long) (2 * range);

        float startVerticalShift = mFromState.getVerticalProgress(mLauncher) * range;
        float endVerticalShift = mToState.getVerticalProgress(mLauncher) * range;

        float totalShift = endVerticalShift - startVerticalShift;

        final AnimatorSetBuilder builder = totalShift == 0 ? new AnimatorSetBuilder()
                : getAnimatorSetBuilderForStates(mFromState, mToState);
        updateAnimatorBuilderOnReinit(builder);

        cancelPendingAnim();

        if (mFromState == OVERVIEW && mToState == NORMAL
                && mOverviewPortraitStateTouchHelper.shouldSwipeDownReturnToApp()) {
            // Reset the state manager, when changing the interaction mode
            mLauncher.getStateManager().goToState(OVERVIEW, false /* animate */);
            mPendingAnimation = mOverviewPortraitStateTouchHelper
                    .createSwipeDownToTaskAppAnimation(maxAccuracy);
            mPendingAnimation.anim.setInterpolator(Interpolators.LINEAR);

            Runnable onCancelRunnable = () -> {
                cancelPendingAnim();
                clearState();
            };
            mCurrentAnimation = AnimatorPlaybackController.wrap(mPendingAnimation.anim, maxAccuracy,
                    onCancelRunnable);
            mLauncher.getStateManager().setCurrentUserControlledAnimation(mCurrentAnimation);
            totalShift = LayoutUtils.getShelfTrackingDistance(mLauncher,
                    mLauncher.getDeviceProfile());
        } else {
            mCurrentAnimation = mLauncher.getStateManager()
                    .createAnimationToNewWorkspace(mToState, builder, maxAccuracy, this::clearState,
                            animComponents);
        }

        if (totalShift == 0) {
            totalShift = Math.signum(mFromState.ordinal - mToState.ordinal)
                    * OverviewState.getDefaultSwipeHeight(mLauncher);
        }
        return 1 / totalShift;
    }

    /**
     * Give subclasses the chance to update the animation when we re-initialize towards a new state.
     */
    protected void updateAnimatorBuilderOnReinit(AnimatorSetBuilder builder) {
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
                        Interpolators.overshootInterpolatorForVelocity(velocity), currFraction, 1);
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
            RecentsModel.INSTANCE.get(mLauncher).onOverviewShown(true, TAG);
        }
    }

    /**
     * Whether the motion event is over the hotseat.
     *
     * @param launcher the launcher activity
     * @param ev the event to check
     * @return true if the event is over the hotseat
     */
    static boolean isTouchOverHotseat(TestActivity launcher, MotionEvent ev) {
        VariantDeviceProfile dp = launcher.getDeviceProfile();
        int hotseatHeight = dp.getHotseatBarSizePx() + dp.getInsets().bottom;
        return (ev.getY() >= (launcher.getDragLayer().getHeight() - hotseatHeight));
    }

    private static class InterpolatorWrapper implements Interpolator {

        public TimeInterpolator baseInterpolator = LINEAR;

        @Override
        public float getInterpolation(float v) {
            return baseInterpolator.getInterpolation(v);
        }
    }
}
