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
package foundation.e.blisslauncher.uioverrides.touchcontrollers;

import static android.view.View.TRANSLATION_X;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.DEACCEL_3;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView;
import foundation.e.blisslauncher.core.touch.AbstractStateChangeTouchController;
import foundation.e.blisslauncher.core.touch.SwipeDetector;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.TouchController;
import foundation.e.blisslauncher.features.test.anim.AnimationSuccessListener;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.quickstep.views.RecentsView;

/**
 * Handles swiping up on the nav bar to go home from launcher, e.g. overview or all apps.
 */
public class NavBarToHomeTouchController implements TouchController, SwipeDetector.Listener {

    private static final Interpolator PULLBACK_INTERPOLATOR = DEACCEL_3;

    private final TestActivity mLauncher;
    private final SwipeDetector mSwipeDetector;
    private final float mPullbackDistance;

    private boolean mNoIntercept;
    private LauncherState mStartState;
    private LauncherState mEndState = NORMAL;
    private AnimatorPlaybackController mCurrentAnimation;

    public NavBarToHomeTouchController(TestActivity launcher) {
        mLauncher = launcher;
        mSwipeDetector = new SwipeDetector(mLauncher, this, SwipeDetector.VERTICAL);
        mPullbackDistance = mLauncher.getResources().getDimension(R.dimen.home_pullback_distance);
    }

    @Override
    public final boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mStartState = mLauncher.getStateManager().getState();
            mNoIntercept = !canInterceptTouch(ev);
            if (mNoIntercept) {
                return false;
            }
            mSwipeDetector.setDetectableScrollConditions(SwipeDetector.DIRECTION_POSITIVE, false);
        }

        if (mNoIntercept) {
            return false;
        }

        onControllerTouchEvent(ev);
        return mSwipeDetector.isDraggingOrSettling();
    }

    private boolean canInterceptTouch(MotionEvent ev) {
        boolean cameFromNavBar = (ev.getEdgeFlags() & Utilities.EDGE_NAV_BAR) != 0;
        if (!cameFromNavBar) {
            return false;
        }
        if (mStartState == OVERVIEW) {
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return true;
        }
        return false;
    }

    @Override
    public final boolean onControllerTouchEvent(MotionEvent ev) {
        return mSwipeDetector.onTouchEvent(ev);
    }

    private float getShiftRange() {
        return mLauncher.getDeviceProfile().getHeightPx();
    }

    @Override
    public void onDragStart(boolean start) {
        initCurrentAnimation();
    }

    private void initCurrentAnimation() {
        long accuracy = (long) (getShiftRange() * 2);
        final AnimatorSet anim = new AnimatorSet();
        if (mStartState == OVERVIEW) {
            RecentsView recentsView = mLauncher.getOverviewPanel();
            float pullbackDist = mPullbackDistance;
            if (!recentsView.isRtl()) {
                pullbackDist = -pullbackDist;
            }
            Animator pullback = ObjectAnimator.ofFloat(recentsView, TRANSLATION_X, pullbackDist);
            pullback.setInterpolator(PULLBACK_INTERPOLATOR);
            anim.play(pullback);
        }
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(mLauncher);
        anim.setDuration(accuracy);
        mCurrentAnimation = AnimatorPlaybackController.wrap(anim, accuracy, this::clearState);
    }

    private void clearState() {
        mCurrentAnimation = null;
        mSwipeDetector.finishedScrolling();
        mSwipeDetector.setDetectableScrollConditions(0, false);
    }

    @Override
    public boolean onDrag(float displacement) {
        // Only allow swipe up.
        displacement = Math.min(0, displacement);
        float progress = Utilities.getProgress(displacement, 0, getShiftRange());
        mCurrentAnimation.setPlayFraction(progress);
        return true;
    }

    @Override
    public void onDragEnd(float velocity, boolean fling) {
        float progress = mCurrentAnimation.getProgressFraction();
        float interpolatedProgress = PULLBACK_INTERPOLATOR.getInterpolation(progress);
        boolean success = interpolatedProgress >= AbstractStateChangeTouchController.SUCCESS_TRANSITION_PROGRESS
                || (velocity < 0 && fling);
        if (success) {
            mLauncher.getStateManager().goToState(mEndState, true,
                    () -> onSwipeInteractionCompleted(mEndState));
            AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(mLauncher);
            if (topOpenView != null) {
                AbstractFloatingView.closeAllOpenViews(mLauncher);
            }
        } else {
            // Quickly return to the state we came from (we didn't move far).
            ValueAnimator anim = mCurrentAnimation.getAnimationPlayer();
            anim.setFloatValues(progress, 0);
            anim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    onSwipeInteractionCompleted(mStartState);
                }
            });
            anim.setDuration(80).start();
        }
    }

    private void onSwipeInteractionCompleted(LauncherState targetState) {
        clearState();
        mLauncher.getStateManager().goToState(targetState, false /* animated */);
    }
}
