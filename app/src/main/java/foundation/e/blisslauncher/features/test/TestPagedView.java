package foundation.e.blisslauncher.features.test;

import android.content.Context;
import android.util.AttributeSet;

import foundation.e.blisslauncher.core.customviews.PagedView;
import foundation.e.blisslauncher.core.customviews.pageindicators.PageIndicatorDots;

public class TestPagedView extends PagedView<PageIndicatorDots> {

    private static final String TAG = "TestPagedView";
    public TestPagedView(Context context) {
        super(context);
    }

    public TestPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }
}
