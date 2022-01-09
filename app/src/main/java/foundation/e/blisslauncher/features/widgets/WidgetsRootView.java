package foundation.e.blisslauncher.features.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import org.jetbrains.annotations.NotNull;

import foundation.e.blisslauncher.features.test.TestActivity;

public class WidgetsRootView extends HorizontalScrollView {

    private static final String TAG = "WidgetsRootView";
    private TestActivity.LauncherOverlay overlay;

    private boolean shouldScrollWorkspace = true;

    // The following constants need to be scaled based on density. The scaled versions will be
    // assigned to the corresponding member variables below.
    private static final int FLING_THRESHOLD_VELOCITY = 500;

    public WidgetsRootView(Context context) {
        super(context);
        init();
    }

    public WidgetsRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WidgetsRootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    public WidgetsRootView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (overlay != null) {
            overlay.onScrollChange(
                ((float) ((ViewGroup) this.getParent())
                    .getWidth() - scrollX) / (float) ((ViewGroup) this.getParent()).getWidth(),
                false,
                false
            );
        }
    }

    @Override
    public void fling(int velocityX) {
        super.fling(velocityX);
        float density = getResources().getDisplayMetrics().density;
        if (Math.abs(velocityX) > FLING_THRESHOLD_VELOCITY * density) {
            shouldScrollWorkspace = !shouldScrollWorkspace;
            overlay.onScrollInteractionEnd();
        }
    }

    @Override
    protected boolean overScrollBy(
        int deltaX,
        int deltaY,
        int scrollX,
        int scrollY,
        int scrollRangeX,
        int scrollRangeY,
        int maxOverScrollX,
        int maxOverScrollY,
        boolean isTouchEvent
    ) {

        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
            computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
            computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS ||
            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS ||
            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;

        maxOverScrollX = ((ViewGroup) this.getParent()).getWidth();
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = 0;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    public void setLauncherOverlay(@NotNull TestActivity.LauncherOverlay overlay) {
        this.overlay = overlay;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onTouchEvent() called with: ev = [" + ev + "]");
        if(ev.getAction() == MotionEvent.ACTION_MOVE && shouldScrollWorkspace) {
            shouldScrollWorkspace = false;
            overlay.onScrollInteractionBegin();
        }
         else if (ev.getAction() == MotionEvent.ACTION_UP) {
            shouldScrollWorkspace = true;
            overlay.onScrollInteractionEnd();
        }
        return super.onTouchEvent(ev);
    }
}