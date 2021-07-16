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

import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.utils.Preconditions;
import foundation.e.blisslauncher.quickstep.util.RecentsAnimationListenerSet;
import foundation.e.blisslauncher.quickstep.util.SwipeAnimationTargetSet;

import java.io.PrintWriter;

/**
 * Utility class used to store state information shared across multiple transitions.
 */
public class SwipeSharedState implements SwipeAnimationTargetSet.SwipeAnimationListener {

    private OverviewComponentObserver mOverviewComponentObserver;

    private RecentsAnimationListenerSet mRecentsAnimationListener;
    private SwipeAnimationTargetSet mLastAnimationTarget;

    private boolean mLastAnimationCancelled = false;
    private boolean mLastAnimationRunning = false;

    public boolean canGestureBeContinued;
    public boolean goingToLauncher;
    public boolean recentsAnimationFinishInterrupted;
    public int nextRunningTaskId = -1;

    public void setOverviewComponentObserver(OverviewComponentObserver observer) {
        mOverviewComponentObserver = observer;
    }

    @Override
    public final void onRecentsAnimationStart(SwipeAnimationTargetSet targetSet) {
        mLastAnimationTarget = targetSet;

        mLastAnimationCancelled = false;
        mLastAnimationRunning = true;
    }

    private void clearAnimationTarget() {
        if (mLastAnimationTarget != null) {
            mLastAnimationTarget.release();
            mLastAnimationTarget = null;
        }
    }

    @Override
    public final void onRecentsAnimationCanceled() {
        clearAnimationTarget();

        mLastAnimationCancelled = true;
        mLastAnimationRunning = false;
    }

    private void clearListenerState(boolean finishAnimation) {
        if (mRecentsAnimationListener != null) {
            mRecentsAnimationListener.removeListener(this);
            mRecentsAnimationListener.cancelListener();
            if (mLastAnimationRunning && mLastAnimationTarget != null) {
                Utilities.postAsyncCallback(
                    TouchInteractionService.MAIN_THREAD_EXECUTOR.getHandler(),
                        finishAnimation
                                ? mLastAnimationTarget::finishAnimation
                                : mLastAnimationTarget::cancelAnimation);
                mLastAnimationTarget = null;
            }
        }
        mRecentsAnimationListener = null;
        clearAnimationTarget();
        mLastAnimationCancelled = false;
        mLastAnimationRunning = false;
    }

    private void onSwipeAnimationFinished(SwipeAnimationTargetSet targetSet) {
        if (mLastAnimationTarget == targetSet) {
            mLastAnimationRunning = false;
        }
    }

    public RecentsAnimationListenerSet newRecentsAnimationListenerSet() {
        Preconditions.assertUIThread();

        if (mLastAnimationRunning) {
            String msg = "New animation started before completing old animation";
            // TODO: gracefully ignore in production
            throw new IllegalArgumentException(msg);
        }

        clearListenerState(false /* finishAnimation */);
        boolean shouldMinimiseSplitScreen = mOverviewComponentObserver == null ? false
                : mOverviewComponentObserver.getActivityControlHelper().shouldMinimizeSplitScreen();
        mRecentsAnimationListener = new RecentsAnimationListenerSet(
                shouldMinimiseSplitScreen, this::onSwipeAnimationFinished);
        mRecentsAnimationListener.addListener(this);
        return mRecentsAnimationListener;
    }

    public RecentsAnimationListenerSet getActiveListener() {
        return mRecentsAnimationListener;
    }

    public void applyActiveRecentsAnimationState(SwipeAnimationTargetSet.SwipeAnimationListener listener) {
        if (mLastAnimationTarget != null) {
            listener.onRecentsAnimationStart(mLastAnimationTarget);
        } else if (mLastAnimationCancelled) {
            listener.onRecentsAnimationCanceled();
        }
    }

    /**
     * Called when a recents animation has finished, but was interrupted before the next task was
     * launched. The given {@param runningTaskId} should be used as the running task for the
     * continuing input consumer.
     */
    public void setRecentsAnimationFinishInterrupted(int runningTaskId) {
        recentsAnimationFinishInterrupted = true;
        nextRunningTaskId = runningTaskId;
        mLastAnimationTarget = mLastAnimationTarget.cloneWithoutTargets();
    }

    public void clearAllState(boolean finishAnimation) {
        clearListenerState(finishAnimation);
        canGestureBeContinued = false;
        recentsAnimationFinishInterrupted = false;
        nextRunningTaskId = -1;
        goingToLauncher = false;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "goingToLauncher=" + goingToLauncher);
        pw.println(prefix + "canGestureBeContinued=" + canGestureBeContinued);
        pw.println(prefix + "recentsAnimationFinishInterrupted=" + recentsAnimationFinishInterrupted);
        pw.println(prefix + "nextRunningTaskId=" + nextRunningTaskId);
        pw.println(prefix + "lastAnimationCancelled=" + mLastAnimationCancelled);
        pw.println(prefix + "lastAnimationRunning=" + mLastAnimationRunning);
    }
}
