package foundation.e.blisslauncher.uioverrides.touchcontrollers;

import static foundation.e.blisslauncher.core.Utilities.EDGE_NAV_BAR;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;

import android.view.MotionEvent;
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView;
import foundation.e.blisslauncher.core.touch.AbstractStateChangeTouchController;
import foundation.e.blisslauncher.core.touch.SwipeDetector;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.quickstep.RecentsModel;

/**
 * Touch controller for handling edge swipes in landscape/seascape UI
 */
public class LandscapeEdgeSwipeController extends AbstractStateChangeTouchController {

    private static final String TAG = "LandscapeEdgeSwipeCtrl";

    public LandscapeEdgeSwipeController(TestActivity l) {
        super(l, SwipeDetector.HORIZONTAL);
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (mCurrentAnimation != null) {
            // If we are already animating from a previous state, we can intercept.
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false;
        }
        return mLauncher.isInState(NORMAL) && (ev.getEdgeFlags() & EDGE_NAV_BAR) != 0;
    }

    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        boolean draggingFromNav = !isDragTowardPositive;
        return draggingFromNav ? OVERVIEW : NORMAL;
    }

    protected float getShiftRange() {
        return mLauncher.getDragLayer().getWidth();
    }

    @Override
    protected float initCurrentAnimation(@LauncherStateManager.AnimationComponents int animComponent) {
        float range = getShiftRange();
        long maxAccuracy = (long) (2 * range);
        mCurrentAnimation = mLauncher.getStateManager().createAnimationToNewWorkspace(mToState,
                maxAccuracy, animComponent);
        return -2 / range;
    }

    @Override
    protected void onSwipeInteractionCompleted(LauncherState targetState) {
        super.onSwipeInteractionCompleted(targetState);
        if (mStartState == NORMAL && targetState == OVERVIEW) {
            RecentsModel.INSTANCE.get(mLauncher).onOverviewShown(true, TAG);
        }
    }
}
