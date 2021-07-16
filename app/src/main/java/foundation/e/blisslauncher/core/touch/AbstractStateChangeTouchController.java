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
package foundation.e.blisslauncher.core.touch;

import static foundation.e.blisslauncher.core.Utilities.SINGLE_FRAME_MS;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.LauncherStateManager.ANIM_ALL;
import static foundation.e.blisslauncher.features.test.LauncherStateManager.ATOMIC_OVERVIEW_SCALE_COMPONENT;
import static foundation.e.blisslauncher.features.test.LauncherStateManager.NON_ATOMIC_COMPONENT;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.scrollInterpolatorForVelocity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.utils.FlingBlockCheck;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.TouchController;
import foundation.e.blisslauncher.features.test.anim.AnimationSuccessListener;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils;
import foundation.e.blisslauncher.features.test.anim.PendingAnimation;

/**
 * TouchController for handling state changes
 */
public abstract class AbstractStateChangeTouchController
        implements TouchController, SwipeDetector.Listener {

    private static final String TAG = "ASCTouchController";

    // Progress after which the transition is assumed to be a success in case user does not fling
    public static final float SUCCESS_TRANSITION_PROGRESS = 0.5f;

    /**
     * Play an atomic recents animation when the progress from NORMAL to OVERVIEW reaches this.
     */
    public static final float ATOMIC_OVERVIEW_ANIM_THRESHOLD = 0.5f;
    protected final long ATOMIC_DURATION = getAtomicDuration();

    protected final TestActivity mLauncher;
    protected final SwipeDetector mDetector;

    private boolean mNoIntercept;
    protected int mStartContainerType;

    protected LauncherState mStartState;
    protected LauncherState mFromState;
    protected LauncherState mToState;
    protected AnimatorPlaybackController mCurrentAnimation;
    protected PendingAnimation mPendingAnimation;

    private float mStartProgress;
    // Ratio of transition process [0, 1] to drag displacement (px)
    private float mProgressMultiplier;
    private float mDisplacementShift;
    private boolean mCanBlockFling;
    private FlingBlockCheck mFlingBlockCheck = new FlingBlockCheck();

    public AnimatorSet mAtomicAnim;
    private boolean mPassedOverviewAtomicThreshold;
    // mAtomicAnim plays the atomic components of the state animations when we pass the threshold.
    // However, if we reinit to transition to a new state (e.g. OVERVIEW -> ALL_APPS) before the
    // atomic animation finishes, we only control the non-atomic components so that we don't
    // interfere with the atomic animation. When the atomic animation ends, we start controlling
    // the atomic components as well, using this controller.
    private AnimatorPlaybackController mAtomicComponentsController;
    private float mAtomicComponentsStartProgress;

    public AbstractStateChangeTouchController(TestActivity l, SwipeDetector.Direction dir) {
        mLauncher = l;
        mDetector = new SwipeDetector(l, this, dir);
    }

    protected long getAtomicDuration() {
        return 200;
    }

    protected abstract boolean canInterceptTouch(MotionEvent ev);

    @Override
    public final boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mNoIntercept = !canInterceptTouch(ev);
            if (mNoIntercept) {
                return false;
            }

            // Now figure out which direction scroll events the controller will start
            // calling the callbacks.
            final int directionsToDetectScroll;
            boolean ignoreSlopWhenSettling = false;

            if (mCurrentAnimation != null) {
                directionsToDetectScroll = SwipeDetector.DIRECTION_BOTH;
                ignoreSlopWhenSettling = true;
            } else {
                directionsToDetectScroll = getSwipeDirection();
                if (directionsToDetectScroll == 0) {
                    mNoIntercept = true;
                    return false;
                }
            }
            mDetector.setDetectableScrollConditions(
                    directionsToDetectScroll, ignoreSlopWhenSettling);
        }

        if (mNoIntercept) {
            return false;
        }

        onControllerTouchEvent(ev);
        return mDetector.isDraggingOrSettling();
    }

    private int getSwipeDirection() {
        LauncherState fromState = mLauncher.getStateManager().getState();
        int swipeDirection = 0;
        if (getTargetState(fromState, true /* isDragTowardPositive */) != fromState) {
            swipeDirection |= SwipeDetector.DIRECTION_POSITIVE;
        }
        if (getTargetState(fromState, false /* isDragTowardPositive */) != fromState) {
            swipeDirection |= SwipeDetector.DIRECTION_NEGATIVE;
        }
        return swipeDirection;
    }

    @Override
    public final boolean onControllerTouchEvent(MotionEvent ev) {
        return mDetector.onTouchEvent(ev);
    }

    /**
     * Returns the state to go to from fromState given the drag direction. If there is no state in
     * that direction, returns fromState.
     */
    protected abstract LauncherState getTargetState(LauncherState fromState,
            boolean isDragTowardPositive);

    protected abstract float initCurrentAnimation(@LauncherStateManager.AnimationComponents int animComponents);

    private boolean reinitCurrentAnimation(boolean reachedToState, boolean isDragTowardPositive) {
        LauncherState newFromState = mFromState == null ? mLauncher.getStateManager().getState()
                : reachedToState ? mToState : mFromState;
        LauncherState newToState = getTargetState(newFromState, isDragTowardPositive);

        if (newFromState == mFromState && newToState == mToState || (newFromState == newToState)) {
            return false;
        }

        mFromState = newFromState;
        mToState = newToState;

        mStartProgress = 0;
        mPassedOverviewAtomicThreshold = false;
        if (mCurrentAnimation != null) {
            mCurrentAnimation.setOnCancelRunnable(null);
        }
        int animComponents = goingBetweenNormalAndOverview(mFromState, mToState)
                ? NON_ATOMIC_COMPONENT : ANIM_ALL;
        if (mAtomicAnim != null) {
            // Control the non-atomic components until the atomic animation finishes, then control
            // the atomic components as well.
            animComponents = NON_ATOMIC_COMPONENT;
            mAtomicAnim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animation) {
                    cancelAtomicComponentsController();
                    if (mCurrentAnimation != null) {
                        mAtomicComponentsStartProgress = mCurrentAnimation.getProgressFraction();
                        long duration = 200l;
                        mAtomicComponentsController = AnimatorPlaybackController.wrap(
                                createAtomicAnimForState(mFromState, mToState, duration), duration);
                        mAtomicComponentsController.dispatchOnStart();
                    }
                }
            });
        }
        if (goingBetweenNormalAndOverview(mFromState, mToState)) {
            cancelAtomicComponentsController();
        }
        mProgressMultiplier = initCurrentAnimation(animComponents);
        mCurrentAnimation.dispatchOnStart();
        return true;
    }

    private boolean goingBetweenNormalAndOverview(LauncherState fromState, LauncherState toState) {
        return (fromState == NORMAL || fromState == OVERVIEW)
                && (toState == NORMAL || toState == OVERVIEW)
                && mPendingAnimation == null;
    }

    @Override
    public void onDragStart(boolean start) {
        mStartState = mLauncher.getStateManager().getState();
        if (mCurrentAnimation == null) {
            mFromState = mStartState;
            mToState = null;
            mAtomicComponentsController = null;
            reinitCurrentAnimation(false, mDetector.wasInitialTouchPositive());
            mDisplacementShift = 0;
        } else {
            mCurrentAnimation.pause();
            mStartProgress = mCurrentAnimation.getProgressFraction();
        }
        mCanBlockFling = mFromState == NORMAL;
        mFlingBlockCheck.unblockFling();
    }

    @Override
    public boolean onDrag(float displacement) {
        float deltaProgress = mProgressMultiplier * (displacement - mDisplacementShift);
        float progress = deltaProgress + mStartProgress;
        updateProgress(progress);
        boolean isDragTowardPositive = (displacement - mDisplacementShift) < 0;
        if (progress <= 0) {
            if (reinitCurrentAnimation(false, isDragTowardPositive)) {
                mDisplacementShift = displacement;
                if (mCanBlockFling) {
                    mFlingBlockCheck.blockFling();
                }
            }
        } else if (progress >= 1) {
            if (reinitCurrentAnimation(true, isDragTowardPositive)) {
                mDisplacementShift = displacement;
                if (mCanBlockFling) {
                    mFlingBlockCheck.blockFling();
                }
            }
        } else {
            mFlingBlockCheck.onEvent();
        }

        return true;
    }

    protected void updateProgress(float fraction) {
        mCurrentAnimation.setPlayFraction(fraction);
        if (mAtomicComponentsController != null) {
            mAtomicComponentsController.setPlayFraction(fraction - mAtomicComponentsStartProgress);
        }
        maybeUpdateAtomicAnim(mFromState, mToState, fraction);
    }

    /**
     * When going between normal and overview states, see if we passed the overview threshold and
     * play the appropriate atomic animation if so.
     */
    private void maybeUpdateAtomicAnim(LauncherState fromState, LauncherState toState,
            float progress) {
        if (!goingBetweenNormalAndOverview(fromState, toState)) {
            return;
        }
        float threshold = toState == OVERVIEW ? ATOMIC_OVERVIEW_ANIM_THRESHOLD
                : 1f - ATOMIC_OVERVIEW_ANIM_THRESHOLD;
        boolean passedThreshold = progress >= threshold;
        if (passedThreshold != mPassedOverviewAtomicThreshold) {
            LauncherState atomicFromState = passedThreshold ? fromState: toState;
            LauncherState atomicToState = passedThreshold ? toState : fromState;
            mPassedOverviewAtomicThreshold = passedThreshold;
            if (mAtomicAnim != null) {
                mAtomicAnim.cancel();
            }
            mAtomicAnim = createAtomicAnimForState(atomicFromState, atomicToState, ATOMIC_DURATION);
            mAtomicAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAtomicAnim = null;
                }
            });
            mAtomicAnim.start();
            mLauncher.getDragLayer().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        }
    }

    private AnimatorSet createAtomicAnimForState(LauncherState fromState, LauncherState targetState,
            long duration) {
        AnimatorSetBuilder builder = getAnimatorSetBuilderForStates(fromState, targetState);
        return mLauncher.getStateManager().createAtomicAnimation(fromState, targetState, builder,
            ATOMIC_OVERVIEW_SCALE_COMPONENT, duration);
    }

    protected AnimatorSetBuilder getAnimatorSetBuilderForStates(LauncherState fromState,
        LauncherState toState) {
        return new AnimatorSetBuilder();
    }

    @Override
    public void onDragEnd(float velocity, boolean fling) {
        boolean blockedFling = fling && mFlingBlockCheck.isBlocked();
        if (blockedFling) {
            fling = false;
        }

        final LauncherState targetState;
        final float progress = mCurrentAnimation.getProgressFraction();
        if (fling) {
            targetState =
                    Float.compare(Math.signum(velocity), Math.signum(mProgressMultiplier)) == 0
                            ? mToState : mFromState;
            // snap to top or bottom using the release velocity
        } else {
            float successProgress = SUCCESS_TRANSITION_PROGRESS;
            targetState = (progress > successProgress) ? mToState : mFromState;
        }

        final float endProgress;
        final float startProgress;
        final long duration;
        // Increase the duration if we prevented the fling, as we are going against a high velocity.
        final int durationMultiplier = blockedFling && targetState == mFromState
                ? LauncherAnimUtils.blockedFlingDurationFactor(velocity) : 1;

        if (targetState == mToState) {
            endProgress = 1;
            if (progress >= 1) {
                duration = 0;
                startProgress = 1;
            } else {
                startProgress = Utilities.boundToRange(
                        progress + velocity * SINGLE_FRAME_MS * mProgressMultiplier, 0f, 1f);
                duration = SwipeDetector.calculateDuration(velocity,
                        endProgress - Math.max(progress, 0)) * durationMultiplier;
            }
        } else {
            // Let the state manager know that the animation didn't go to the target state,
            // but don't cancel ourselves (we already clean up when the animation completes).
            Runnable onCancel = mCurrentAnimation.getOnCancelRunnable();
            mCurrentAnimation.setOnCancelRunnable(null);
            mCurrentAnimation.dispatchOnCancel();
            mCurrentAnimation.setOnCancelRunnable(onCancel);

            endProgress = 0;
            if (progress <= 0) {
                duration = 0;
                startProgress = 0;
            } else {
                startProgress = Utilities.boundToRange(
                        progress + velocity * SINGLE_FRAME_MS * mProgressMultiplier, 0f, 1f);
                duration = SwipeDetector.calculateDuration(velocity,
                        Math.min(progress, 1) - endProgress) * durationMultiplier;
            }
        }

        mCurrentAnimation.setEndAction(() -> onSwipeInteractionCompleted(targetState));
        ValueAnimator anim = mCurrentAnimation.getAnimationPlayer();
        anim.setFloatValues(startProgress, endProgress);
        maybeUpdateAtomicAnim(mFromState, targetState, targetState == mToState ? 1f : 0f);
        updateSwipeCompleteAnimation(anim, Math.max(duration, getRemainingAtomicDuration()),
                targetState, velocity, fling);
        mCurrentAnimation.dispatchOnStart();
        anim.start();
        if (mAtomicAnim == null) {
            startAtomicComponentsAnim(endProgress, anim.getDuration());
        } else {
            mAtomicAnim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    startAtomicComponentsAnim(endProgress, anim.getDuration());
                }
            });
        }
    }

    /**
     * Animates the atomic components from the current progress to the final progress.
     *
     * Note that this only applies when we are controlling the atomic components separately from
     * the non-atomic components, which only happens if we reinit before the atomic animation
     * finishes.
     */
    private void startAtomicComponentsAnim(float toProgress, long duration) {
        if (mAtomicComponentsController != null) {
            ValueAnimator atomicAnim = mAtomicComponentsController.getAnimationPlayer();
            atomicAnim.setFloatValues(mAtomicComponentsController.getProgressFraction(), toProgress);
            atomicAnim.setDuration(duration);
            atomicAnim.start();
            atomicAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAtomicComponentsController = null;
                }
            });
        }
    }

    private long getRemainingAtomicDuration() {
        if (mAtomicAnim == null) {
            return 0;
        }
        if (Utilities.ATLEAST_OREO) {
            return mAtomicAnim.getTotalDuration() - mAtomicAnim.getCurrentPlayTime();
        } else {
            long remainingDuration = 0;
            for (Animator anim : mAtomicAnim.getChildAnimations()) {
                remainingDuration = Math.max(remainingDuration, anim.getDuration());
            }
            return remainingDuration;
        }
    }

    protected void updateSwipeCompleteAnimation(ValueAnimator animator, long expectedDuration,
            LauncherState targetState, float velocity, boolean isFling) {
        animator.setDuration(expectedDuration)
                .setInterpolator(scrollInterpolatorForVelocity(velocity));
    }

    protected void onSwipeInteractionCompleted(LauncherState targetState) {
        clearState();
        boolean shouldGoToTargetState = true;
        if (mPendingAnimation != null) {
            boolean reachedTarget = mToState == targetState;
            mPendingAnimation.finish(reachedTarget);
            mPendingAnimation = null;
            shouldGoToTargetState = !reachedTarget;
        }
        if (shouldGoToTargetState) {
            mLauncher.getStateManager().goToState(targetState, false /* animated */);
        }
    }

    protected void clearState() {
        mCurrentAnimation = null;
        cancelAtomicComponentsController();
        mDetector.finishedScrolling();
        mDetector.setDetectableScrollConditions(0, false);
    }

    private void cancelAtomicComponentsController() {
        if (mAtomicComponentsController != null) {
            mAtomicComponentsController.getAnimationPlayer().cancel();
            mAtomicComponentsController = null;
        }
    }

    protected float getShiftRange() {
        return 0;
    }
}
