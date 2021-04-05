package foundation.e.blisslauncher.features.test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowInsets;

import org.jetbrains.annotations.Nullable;

import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout;

import static foundation.e.blisslauncher.features.test.SystemUiController.FLAG_DARK_NAV;
import static foundation.e.blisslauncher.features.test.SystemUiController.UI_STATE_NORMAL;

public class LauncherRootView extends InsettableFrameLayout {

    private final TestActivity mLauncher;

    private final Paint mOpaquePaint;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final Rect mConsumedInsets = new Rect();

    private View mAlignedView;
    private WindowStateListener mWindowStateListener;

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mOpaquePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOpaquePaint.setColor(Color.BLACK);
        mOpaquePaint.setStyle(Paint.Style.FILL);

        mLauncher = TestActivity.Companion.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 0) {
            // LauncherRootView contains only one child, which should be aligned
            // based on the horizontal insets.
            mAlignedView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    @Nullable
    @Override
    public WindowInsets onApplyWindowInsets(@Nullable WindowInsets insets) {
        mConsumedInsets.setEmpty();
        boolean drawInsetBar = false;

        mLauncher.getSystemUiController().updateUiState(
            UI_STATE_NORMAL, drawInsetBar ? FLAG_DARK_NAV : 0);

        // Update device profile before notifying th children.
        mLauncher.getDeviceProfile().updateInsets(insets);
        boolean resetState = !insets.equals(insets);
        setInsets(insets);

        if (mAlignedView != null) {
            // Apply margins on aligned view to handle consumed insets.
            MarginLayoutParams lp = (MarginLayoutParams) mAlignedView.getLayoutParams();
            if (lp.leftMargin != mConsumedInsets.left || lp.rightMargin != mConsumedInsets.right ||
                lp.bottomMargin != mConsumedInsets.bottom) {
                lp.leftMargin = mConsumedInsets.left;
                lp.rightMargin = mConsumedInsets.right;
                lp.topMargin = mConsumedInsets.top;
                lp.bottomMargin = mConsumedInsets.bottom;
                mAlignedView.setLayoutParams(lp);
            }
        }
        if (resetState) {
            // TODO: Enable state if necessary.
            //mLauncher.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
        }

        return insets; // I'll take it from here
    }

    @Override
    public void setInsets(WindowInsets insets) {
        // If the insets haven't changed, this is a no-op. Avoid unnecessary layout caused by
        // modifying child layout params.
        if (!insets.equals(getWindowInsets())) {
            super.setInsets(insets);
        }
    }

    public void dispatchInsets() {
        mLauncher.getDeviceProfile().updateInsets(getWindowInsets());
        super.setInsets(getWindowInsets());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // If the right inset is opaque, draw a black rectangle to ensure that is stays opaque.
        if (mConsumedInsets.right > 0) {
            int width = getWidth();
            canvas.drawRect(width - mConsumedInsets.right, 0, width, getHeight(), mOpaquePaint);
        }
        if (mConsumedInsets.left > 0) {
            canvas.drawRect(0, 0, mConsumedInsets.left, getHeight(), mOpaquePaint);
        }
        if (mConsumedInsets.bottom > 0) {
            int height = getHeight();
            canvas.drawRect(0, height - mConsumedInsets.bottom, getWidth(), height, mOpaquePaint);
        }
    }

    public void setWindowStateListener(WindowStateListener listener) {
        mWindowStateListener = listener;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowFocusChanged(hasWindowFocus);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowVisibilityChanged(visibility);
        }
    }

    public interface WindowStateListener {

        void onWindowFocusChanged(boolean hasFocus);

        void onWindowVisibilityChanged(int visibility);
    }
}
