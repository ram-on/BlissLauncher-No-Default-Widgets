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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;

import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.touch.SwipeDetector;
import foundation.e.blisslauncher.core.utils.FlingBlockCheck;
import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.test.BaseDragLayer;
import foundation.e.blisslauncher.features.test.BaseDraggingActivity;
import foundation.e.blisslauncher.features.test.TouchController;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.features.test.anim.Interpolators;
import foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils;
import foundation.e.blisslauncher.features.test.anim.PendingAnimation;
import foundation.e.quickstep.SysUINavigationMode;
import foundation.e.quickstep.views.RecentsView;
import foundation.e.quickstep.views.TaskView;

import static foundation.e.blisslauncher.core.Flags.ENABLE_QUICKSTEP_LIVE_TILE;
import static foundation.e.blisslauncher.core.Flags.QUICKSTEP_SPRINGS;
import static foundation.e.blisslauncher.core.Utilities.SINGLE_FRAME_MS;
import static foundation.e.blisslauncher.features.quickstep.AbstractFloatingView.TYPE_ACCESSIBLE;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.scrollInterpolatorForVelocity;

/**
 * Touch controller for handling task view card swipes
 */
public abstract class TaskViewTouchController<T extends BaseDraggingActivity>
        extends AnimatorListenerAdapter implements TouchController, SwipeDetector.Listener {

    private static final String TAG = "OverviewSwipeController";

    // Progress after which the transition is assumed to be a success in case user does not fling
    public static final float SUCCESS_TRANSITION_PROGRESS = 0.5f;

    protected final T mActivity;
    private final SwipeDetector mDetector;
    private final RecentsView mRecentsView;
    private final int[] mTempCords = new int[2];

    private PendingAnimation mPendingAnimation;
    private AnimatorPlaybackController mCurrentAnimation;
    private boolean mCurrentAnimationIsGoingUp;

    private boolean mNoIntercept;

    private float mDisplacementShift;
    private float mProgressMultiplier;
    private float mEndDisplacement;
    private FlingBlockCheck mFlingBlockCheck = new FlingBlockCheck();

    private TaskView mTaskBeingDragged;

    public TaskViewTouchController(T activity) {
        mActivity = activity;
        mRecentsView = activity.getOverviewPanel();
        mDetector = new SwipeDetector(activity, this, SwipeDetector.VERTICAL);
    }

    private boolean canInterceptTouch() {
        if (mCurrentAnimation != null) {
            // If we are already animating from a previous state, we can intercept.
            return true;
        }
        if (AbstractFloatingView.getTopOpenViewWithType(mActivity, TYPE_ACCESSIBLE) != null) {
            return false;
        }
        return isRecentsInteractive();
    }

    protected abstract boolean isRecentsInteractive();

    protected void onUserControlledAnimationCreated(AnimatorPlaybackController animController) {
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        if (mCurrentAnimation != null && animation == mCurrentAnimation.getTarget()) {
            clearState();
        }
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mNoIntercept = !canInterceptTouch();
            if (mNoIntercept) {
                return false;
            }

            // Now figure out which direction scroll events the controller will start
            // calling the callbacks.
            int directionsToDetectScroll = 0;
            boolean ignoreSlopWhenSettling = false;
            if (mCurrentAnimation != null) {
                directionsToDetectScroll = SwipeDetector.DIRECTION_BOTH;
                ignoreSlopWhenSettling = true;
            } else {
                mTaskBeingDragged = null;

                for (int i = 0; i < mRecentsView.getTaskViewCount(); i++) {
                    TaskView view = mRecentsView.getTaskViewAt(i);
                    if (mRecentsView.isTaskViewVisible(view) && mActivity.getDragLayer()
                            .isEventOverView(view, ev)) {
                        mTaskBeingDragged = view;
                        if (!SysUINavigationMode.getMode(mActivity).hasGestures) {
                            // Don't allow swipe down to open if we don't support swipe up
                            // to enter overview.
                            directionsToDetectScroll = SwipeDetector.DIRECTION_POSITIVE;
                        } else {
                            // The task can be dragged up to dismiss it,
                            // and down to open if it's the current page.
                            directionsToDetectScroll = i == mRecentsView.getCurrentPage()
                                    ? SwipeDetector.DIRECTION_BOTH : SwipeDetector.DIRECTION_POSITIVE;
                        }
                        break;
                    }
                }
                if (mTaskBeingDragged == null) {
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

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return mDetector.onTouchEvent(ev);
    }

    private void reInitAnimationController(boolean goingUp) {
        if (mCurrentAnimation != null && mCurrentAnimationIsGoingUp == goingUp) {
            // No need to init
            return;
        }
        int scrollDirections = mDetector.getScrollDirections();
        if (goingUp && ((scrollDirections & SwipeDetector.DIRECTION_POSITIVE) == 0)
                || !goingUp && ((scrollDirections & SwipeDetector.DIRECTION_NEGATIVE) == 0)) {
            // Trying to re-init in an unsupported direction.
            return;
        }
        if (mCurrentAnimation != null) {
            mCurrentAnimation.setPlayFraction(0);
        }
        if (mPendingAnimation != null) {
            mPendingAnimation.finish(false);
            mPendingAnimation = null;
        }

        mCurrentAnimationIsGoingUp = goingUp;
        BaseDragLayer dl = mActivity.getDragLayer();
        long maxDuration = (long) (2 * dl.getHeight());

        if (goingUp) {
            mPendingAnimation = mRecentsView.createTaskDismissAnimation(mTaskBeingDragged,
                    true /* animateTaskView */, true /* removeTask */, maxDuration);

            mEndDisplacement = -mTaskBeingDragged.getHeight();
        } else {
            mPendingAnimation = mRecentsView.createTaskLauncherAnimation(
                    mTaskBeingDragged, maxDuration);
            mPendingAnimation.anim.setInterpolator(Interpolators.ZOOM_IN);

            mTempCords[1] = mTaskBeingDragged.getHeight();
            dl.getDescendantCoordRelativeToSelf(mTaskBeingDragged, mTempCords);
            mEndDisplacement = dl.getHeight() - mTempCords[1];
        }

        if (mCurrentAnimation != null) {
            mCurrentAnimation.setOnCancelRunnable(null);
        }
        mCurrentAnimation = AnimatorPlaybackController
                .wrap(mPendingAnimation.anim, maxDuration, this::clearState);
        onUserControlledAnimationCreated(mCurrentAnimation);
        mCurrentAnimation.getTarget().addListener(this);
        mCurrentAnimation.dispatchOnStart();
        mProgressMultiplier = 1 / mEndDisplacement;
    }

    @Override
    public void onDragStart(boolean start) {
        if (mCurrentAnimation == null) {
            reInitAnimationController(mDetector.wasInitialTouchPositive());
            mDisplacementShift = 0;
        } else {
            mDisplacementShift = mCurrentAnimation.getProgressFraction() / mProgressMultiplier;
            mCurrentAnimation.pause();
        }
        mFlingBlockCheck.unblockFling();
    }

    @Override
    public boolean onDrag(float displacement) {
        float totalDisplacement = displacement + mDisplacementShift;
        boolean isGoingUp =
                totalDisplacement == 0 ? mCurrentAnimationIsGoingUp : totalDisplacement < 0;
        if (isGoingUp != mCurrentAnimationIsGoingUp) {
            reInitAnimationController(isGoingUp);
            mFlingBlockCheck.blockFling();
        } else {
            mFlingBlockCheck.onEvent();
        }
        mCurrentAnimation.setPlayFraction(Utilities.boundToRange(
                totalDisplacement * mProgressMultiplier, 0, 1));

        if (ENABLE_QUICKSTEP_LIVE_TILE) {
            if (mRecentsView.getCurrentPage() != 0 || isGoingUp) {
                mRecentsView.redrawLiveTile(true);
            }
        }
        return true;
    }

    @Override
    public void onDragEnd(float velocity, boolean fling) {
        final boolean goingToEnd;
        final int logAction;
        boolean blockedFling = fling && mFlingBlockCheck.isBlocked();
        if (blockedFling) {
            fling = false;
        }
        float progress = mCurrentAnimation.getProgressFraction();
        float interpolatedProgress = mCurrentAnimation.getInterpolatedProgress();
        if (fling) {
            boolean goingUp = velocity < 0;
            goingToEnd = goingUp == mCurrentAnimationIsGoingUp;
        } else {
            goingToEnd = interpolatedProgress > SUCCESS_TRANSITION_PROGRESS;
        }
        long animationDuration = SwipeDetector.calculateDuration(
                velocity, goingToEnd ? (1 - progress) : progress);
        if (blockedFling && !goingToEnd) {
            animationDuration *= LauncherAnimUtils.blockedFlingDurationFactor(velocity);
        }

        float nextFrameProgress = Utilities.boundToRange(
                progress + velocity * SINGLE_FRAME_MS / Math.abs(mEndDisplacement), 0f, 1f);

        mCurrentAnimation.setEndAction(() -> onCurrentAnimationEnd(goingToEnd));

        ValueAnimator anim = mCurrentAnimation.getAnimationPlayer();
        anim.setFloatValues(nextFrameProgress, goingToEnd ? 1f : 0f);
        anim.setDuration(animationDuration);
        anim.setInterpolator(scrollInterpolatorForVelocity(velocity));
        if (ENABLE_QUICKSTEP_LIVE_TILE) {
            anim.addUpdateListener(valueAnimator -> {
                if (mRecentsView.getCurrentPage() != 0 || mCurrentAnimationIsGoingUp) {
                    mRecentsView.redrawLiveTile(true);
                }
            });
        }
        if (QUICKSTEP_SPRINGS) {
            mCurrentAnimation.dispatchOnStartWithVelocity(goingToEnd ? 1f : 0f, velocity);
        }
        anim.start();
    }

    private void onCurrentAnimationEnd(boolean wasSuccess) {
        if (mPendingAnimation != null) {
            mPendingAnimation.finish(wasSuccess);
            mPendingAnimation = null;
        }
        clearState();
    }

    private void clearState() {
        mDetector.finishedScrolling();
        mDetector.setDetectableScrollConditions(0, false);
        mTaskBeingDragged = null;
        mCurrentAnimation = null;
        if (mPendingAnimation != null) {
            mPendingAnimation.finish(false);
            mPendingAnimation = null;
        }
    }
}
