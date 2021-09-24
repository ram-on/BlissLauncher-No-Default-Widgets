package foundation.e.blisslauncher.features.test;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static foundation.e.blisslauncher.core.Utilities.SINGLE_FRAME_MS;
import static foundation.e.blisslauncher.core.Utilities.shouldDisableGestures;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView;
import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout;
import foundation.e.blisslauncher.core.utils.MultiValueAlpha;
import java.util.ArrayList;

/**
 * A viewgroup with utility methods for drag-n-drop and touch interception
 */
public abstract class BaseDragLayer<T extends BaseDraggingActivity> extends InsettableFrameLayout {

    protected final int[] mTmpXY = new int[2];
    protected final Rect mHitRect = new Rect();

    protected final T mActivity;

    private final MultiValueAlpha mMultiValueAlpha;

    protected TouchController[] mControllers;
    protected TouchController mActiveController;
    // Touch controller which is being used for the proxy events
    protected TouchController mProxyTouchController;
    private TouchCompleteListener mTouchCompleteListener;

    // Touch is being dispatched through the normal view dispatch system
    private static final int TOUCH_DISPATCHING_VIEW = 1 << 0;
    // Touch is being dispatched through the normal view dispatch system, and started at the
    // system gesture region
    private static final int TOUCH_DISPATCHING_GESTURE = 1 << 1;
    // Touch is being dispatched through a proxy from InputMonitor
    private static final int TOUCH_DISPATCHING_PROXY = 1 << 2;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final RectF mSystemGestureRegion = new RectF();
    private int mTouchDispatchState = 0;

    public BaseDragLayer(Context context, AttributeSet attrs, int alphaChannelCount) {
        super(context, attrs);
        mActivity = (T) BaseActivity.fromContext(context);
        mMultiValueAlpha = new MultiValueAlpha(this, alphaChannelCount);
    }

    public boolean isEventOverView(View view, MotionEvent ev) {
        getDescendantRectRelativeToSelf(view, mHitRect);
        return mHitRect.contains((int) ev.getX(), (int) ev.getY());
    }

    /**
     * Given a motion event in evView's coordinates, return whether the event is within another
     * view's bounds.
     */
    public boolean isEventOverView(View view, MotionEvent ev, View evView) {
        int[] xy = new int[] {(int) ev.getX(), (int) ev.getY()};
        getDescendantCoordRelativeToSelf(evView, xy);
        getDescendantRectRelativeToSelf(view, mHitRect);
        return mHitRect.contains(xy[0], xy[1]);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mTouchCompleteListener != null) {
                mTouchCompleteListener.onTouchComplete();
            }
            mTouchCompleteListener = null;
        } else if (action == MotionEvent.ACTION_DOWN) {
            mActivity.finishAutoCancelActionMode();
        }
        return findActiveController(ev);
    }

    protected boolean findActiveController(MotionEvent ev) {
        mActiveController = null;

        //TODO: Implement when adding folder grouping.
        if ((mTouchDispatchState & (TOUCH_DISPATCHING_GESTURE | TOUCH_DISPATCHING_PROXY)) == 0) {
            // Only look for controllers if we are not dispatching from gesture area and proxy is
            // not active
            mActiveController = findControllerToHandleTouch(ev);

            if (mActiveController != null) return true;
        }
        return false;
    }

    private TouchController findControllerToHandleTouch(MotionEvent ev) {
        if (shouldDisableGestures(ev)) return null;

        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(mActivity);
        if (topView != null && topView.onControllerInterceptTouchEvent(ev)) {
            return topView;
        }

        for (TouchController controller : mControllers) {
            if (controller.onControllerInterceptTouchEvent(ev)) {
                return controller;
            }
        }
        return null;
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);

        if (child instanceof AbstractFloatingView) {
            // Handles the case where the view is removed without being properly closed.
            // This can happen if something goes wrong during a state change/transition.
            postDelayed(() -> {
                AbstractFloatingView floatingView = (AbstractFloatingView) child;
                if (floatingView.isOpen()) {
                    floatingView.close(false);
                }
            }, SINGLE_FRAME_MS);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mTouchCompleteListener != null) {
                mTouchCompleteListener.onTouchComplete();
            }
            mTouchCompleteListener = null;
        }

        if (mActiveController != null) {
            return mActiveController.onControllerTouchEvent(ev);
        } else {
            // In case no child view handled the touch event, we may not get onIntercept anymore
            return findActiveController(ev);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case ACTION_DOWN: {
                float x = ev.getX();
                float y = ev.getY();
                mTouchDispatchState |= TOUCH_DISPATCHING_VIEW;

                if ((y < mSystemGestureRegion.top
                    || x < mSystemGestureRegion.left
                    || x > (getWidth() - mSystemGestureRegion.right)
                    || y > (getHeight() - mSystemGestureRegion.bottom))) {
                    mTouchDispatchState |= TOUCH_DISPATCHING_GESTURE;
                } else {
                    mTouchDispatchState &= ~TOUCH_DISPATCHING_GESTURE;
                }
                break;
            }
            case ACTION_CANCEL:
            case ACTION_UP:
                mTouchDispatchState &= ~TOUCH_DISPATCHING_GESTURE;
                mTouchDispatchState &= ~TOUCH_DISPATCHING_VIEW;
                break;
        }
        super.dispatchTouchEvent(ev);

        // We want to get all events so that mTouchDispatchSource is maintained properly
        return true;
    }

    /**
     * Called before we are about to receive proxy events.
     *
     * @return false if we can't handle proxy at this time
     */
    public boolean prepareProxyEventStarting() {
        mProxyTouchController = null;
        if ((mTouchDispatchState & TOUCH_DISPATCHING_VIEW) != 0 && mActiveController != null) {
            // We are already dispatching using view system and have an active controller, we can't
            // handle another controller.

            // This flag was already cleared in proxy ACTION_UP or ACTION_CANCEL. Added here just
            // to be safe
            mTouchDispatchState &= ~TOUCH_DISPATCHING_PROXY;
            return false;
        }

        mTouchDispatchState |= TOUCH_DISPATCHING_PROXY;
        return true;
    }

    /**
     * Determine the rect of the descendant in this DragLayer's coordinates
     *
     * @param descendant The descendant whose coordinates we want to find.
     * @param r The rect into which to place the results.
     * @return The factor by which this descendant is scaled relative to this DragLayer.
     */
    public float getDescendantRectRelativeToSelf(View descendant, Rect r) {
        mTmpXY[0] = 0;
        mTmpXY[1] = 0;
        float scale = getDescendantCoordRelativeToSelf(descendant, mTmpXY);

        r.set(mTmpXY[0], mTmpXY[1],
            (int) (mTmpXY[0] + scale * descendant.getMeasuredWidth()),
            (int) (mTmpXY[1] + scale * descendant.getMeasuredHeight()));
        return scale;
    }

    public float getLocationInDragLayer(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        return getDescendantCoordRelativeToSelf(child, loc);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
        return getDescendantCoordRelativeToSelf(descendant, coord, false);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, float[] coord) {
        return getDescendantCoordRelativeToSelf(descendant, coord, false);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in this DragLayer's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the root descendant:
     *          sometimes this is relevant as in a child's coordinates within the root descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord,
        boolean includeRootScroll) {
        return Utilities.getDescendantCoordRelativeToAncestor(descendant, this,
            coord, includeRootScroll);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in this DragLayer's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the root descendant:
     *          sometimes this is relevant as in a child's coordinates within the root descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public float getDescendantCoordRelativeToSelf(View descendant, float[] coord,
        boolean includeRootScroll) {
        return Utilities.getDescendantCoordRelativeToAncestor(descendant, this,
            coord, includeRootScroll);
    }

    /**
     * Inverse of {@link #getDescendantCoordRelativeToSelf(View, int[])}.
     */
    public void mapCoordInSelfToDescendant(View descendant, int[] coord) {
        Utilities.mapCoordInSelfToDescendant(descendant, this, coord);
    }

    /**
     * Inverse of {@link #getDescendantCoordRelativeToSelf(View, float[])}.
     */
    public void mapCoordInSelfToDescendant(View descendant, float[] coord) {
        Utilities.mapCoordInSelfToDescendant(descendant, this, coord);
    }

    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];

        int[] temp = new int[2];
        getLocationOnScreen(temp);

        Log.i("BaseDragLayer", "getViewRectRelativeToSelf iNWindow: "+x+" "+y);
        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];
        Log.i("BaseDragLayer", "getViewRectRelativeToSelf ofView: "+vX+" "+vY);

        int left = vX - x;
        int top = vY - y;
        r.set(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
    }

    /**
     * Proxies the touch events to the gesture handlers
     */
    public boolean proxyTouchEvent(MotionEvent ev) {
        boolean handled;
        if (mProxyTouchController != null) {
            handled = mProxyTouchController.onControllerTouchEvent(ev);
        } else {
            mProxyTouchController = findControllerToHandleTouch(ev);
            handled = mProxyTouchController != null;
        }
        int action = ev.getAction();
        if (action == ACTION_UP || action == ACTION_CANCEL) {
            mProxyTouchController = null;
            mTouchDispatchState &= ~TOUCH_DISPATCHING_PROXY;
        }
        return handled;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        // Consume the unhandled move if a container is open, to avoid switching pages underneath.
        return false;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        View topView = AbstractFloatingView.getTopOpenView(mActivity);
        if (topView != null) {
            return topView.requestFocus(direction, previouslyFocusedRect);
        } else {
            return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
        }
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        View topView = AbstractFloatingView.getTopOpenView(mActivity);
        if (topView != null) {
            topView.addFocusables(views, direction);
        } else {
            super.addFocusables(views, direction, focusableMode);
        }
        super.addFocusables(views, direction, focusableMode);
    }

    public void setTouchCompleteListener(TouchCompleteListener listener) {
        mTouchCompleteListener = listener;
    }

    public MultiValueAlpha.AlphaProperty getAlphaProperty(int index) {
        return mMultiValueAlpha.getProperty(index);
    }

    public interface TouchCompleteListener {
        void onTouchComplete();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends InsettableFrameLayout.LayoutParams {
        public int x, y;
        public boolean customPosition = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams lp) {
            super(lp);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            final FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) child.getLayoutParams();
            if (flp instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) flp;
                if (lp.customPosition) {
                    child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            }
        }
    }
}
