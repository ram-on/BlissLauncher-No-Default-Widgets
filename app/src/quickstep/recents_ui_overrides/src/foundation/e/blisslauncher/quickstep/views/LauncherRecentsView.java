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
package foundation.e.blisslauncher.quickstep.views;

import static foundation.e.blisslauncher.core.Flags.ENABLE_QUICKSTEP_LIVE_TILE;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.LauncherState.RECENTS_CLEAR_ALL_BUTTON;

import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import foundation.e.blisslauncher.features.launcher.Hotseat;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.quickstep.SysUINavigationMode;
import foundation.e.blisslauncher.quickstep.util.ClipAnimationHelper;
import foundation.e.blisslauncher.quickstep.util.ClipAnimationHelper.TransformParams;
import foundation.e.blisslauncher.quickstep.util.LayoutUtils;

/**
 * {@link RecentsView} used in Launcher activity
 */
@TargetApi(Build.VERSION_CODES.O)
public class LauncherRecentsView extends RecentsView<TestActivity> implements
    LauncherStateManager.StateListener {

    private final TransformParams mTransformParams = new TransformParams();

    public LauncherRecentsView(Context context) {
        this(context, null);
    }

    public LauncherRecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LauncherRecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setContentAlpha(0);
        mActivity.getStateManager().addStateListener(this);
    }

    @Override
    public void startHome() {
        mActivity.getStateManager().goToState(NORMAL);
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        if (ENABLE_QUICKSTEP_LIVE_TILE) {
            LauncherState state = mActivity.getStateManager().getState();
            if (state == OVERVIEW) {
                redrawLiveTile(false);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        maybeDrawEmptyMessage(canvas);
        super.draw(canvas);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        updateEmptyMessage();
    }

    @Override
    protected void onTaskStackUpdated() {
        // Lazily update the empty message only when the task stack is reapplied
        updateEmptyMessage();
    }

    /**
     * Animates adjacent tasks and translate hotseat off screen as well.
     */
    @Override
    public AnimatorSet createAdjacentPageAnimForTaskLaunch(TaskView tv,
            ClipAnimationHelper helper) {
        AnimatorSet anim = super.createAdjacentPageAnimForTaskLaunch(tv, helper);

        if (!SysUINavigationMode.getMode(mActivity).hasGestures) {
            // Hotseat doesn't move when opening recents with the button,
            // so don't animate it here either.
            return anim;
        }
        return anim;
    }

    @Override
    protected void getTaskSize(VariantDeviceProfile dp, Rect outRect) {
        LayoutUtils.calculateLauncherTaskSize(getContext(), dp, outRect);
    }

    @Override
    protected void onTaskLaunchAnimationUpdate(float progress, TaskView tv) {
        if (ENABLE_QUICKSTEP_LIVE_TILE) {
            if (mRecentsAnimationWrapper.targetSet != null && tv.isRunningTask()) {
                mTransformParams.setProgress(1 - progress)
                        .setSyncTransactionApplier(mSyncTransactionApplier)
                        .setForLiveTile(true);
                mClipAnimationHelper.applyTransform(mRecentsAnimationWrapper.targetSet,
                        mTransformParams);
            } else {
                redrawLiveTile(true);
            }
        }
    }

    @Override
    protected void onTaskLaunched(boolean success) {
        if (success) {
            mActivity.getStateManager().goToState(NORMAL, false /* animate */);
        }
        super.onTaskLaunched(success);
    }

    @Override
    public boolean shouldUseMultiWindowTaskSizeStrategy() {
        return mActivity.isInMultiWindowMode();
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        if (ENABLE_QUICKSTEP_LIVE_TILE && mEnableDrawingLiveTile) {
            redrawLiveTile(true);
        }
    }

    @Override
    public void redrawLiveTile(boolean mightNeedToRefill) {
        if (!mEnableDrawingLiveTile || mRecentsAnimationWrapper == null
                || mClipAnimationHelper == null) {
            return;
        }
        TaskView taskView = getRunningTaskView();
        if (taskView != null) {
            taskView.getThumbnail().getGlobalVisibleRect(mTempRect);
            int offsetX = (int) (mTaskWidth * taskView.getScaleX() * getScaleX()
                    - mTempRect.width());
            int offsetY = (int) (mTaskHeight * taskView.getScaleY() * getScaleY()
                    - mTempRect.height());
            if (((mCurrentPage != 0) || mightNeedToRefill) && offsetX > 0) {
                if (mTempRect.left - offsetX < 0) {
                    mTempRect.left -= offsetX;
                } else {
                    mTempRect.right += offsetX;
                }
            }
            if (mightNeedToRefill && offsetY > 0) {
                mTempRect.top -= offsetY;
            }
            mTempRectF.set(mTempRect);
            mTransformParams.setProgress(1f)
                    .setCurrentRectAndTargetAlpha(mTempRectF, taskView.getAlpha())
                    .setSyncTransactionApplier(mSyncTransactionApplier);
            if (mRecentsAnimationWrapper.targetSet != null) {
                mClipAnimationHelper.applyTransform(mRecentsAnimationWrapper.targetSet,
                        mTransformParams);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        // TODO: May use it when adding predictions.
    }

    @Override
    public void onStateTransitionStart(LauncherState toState) {
        setOverviewStateEnabled(toState.overviewUi);
        setFreezeViewVisibility(true);
    }

    @Override
    public void onStateTransitionComplete(LauncherState finalState) {
        if (finalState == NORMAL) {
            // Clean-up logic that occurs when recents is no longer in use/visible.
            reset();
        }
        setOverlayEnabled(finalState == OVERVIEW);
        setFreezeViewVisibility(false);
    }

    @Override
    public void setOverviewStateEnabled(boolean enabled) {
        super.setOverviewStateEnabled(enabled);
        if (enabled) {
            LauncherState state = mActivity.getStateManager().getState();
            boolean hasClearAllButton = (state.getVisibleElements(mActivity)
                    & RECENTS_CLEAR_ALL_BUTTON) != 0;
            setDisallowScrollToClearAll(!hasClearAllButton);
        }
    }

    @Override
    protected boolean shouldStealTouchFromSiblingsBelow(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Allow touches to go through to the hotseat.
            Hotseat hotseat = mActivity.getHotseat();
            boolean touchingHotseat = hotseat.isShown()
                    && mActivity.getDragLayer().isEventOverView(hotseat, ev, this);
            return !touchingHotseat;
        }
        return super.shouldStealTouchFromSiblingsBelow(ev);
    }
}
