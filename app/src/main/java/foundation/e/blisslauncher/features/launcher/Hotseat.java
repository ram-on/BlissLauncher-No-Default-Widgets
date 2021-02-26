/*
 * Copyright (C) 2011 The Android Open Source Project
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

package foundation.e.blisslauncher.features.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.Insettable;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

public class Hotseat extends FrameLayout implements Insettable {

    private final TestActivity mLauncher;
    private CellLayout mContent;

    private static final String TAG = "Hotseat";

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = TestActivity.Companion.getLauncher(context);
    }

    public CellLayout getLayout() {
        return mContent;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.dockLayout);
        setBackgroundColor(Color.RED);
    }

    void resetLayout(boolean hasVerticalHotseat) {
        mContent.removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
        VariantDeviceProfile idp = mLauncher.getDeviceProfile();
        mContent.setColumnCount(idp.getInv().getNumHotseatIcons());
        mContent.setRowCount(1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state or an accessible drag is in progress.
        return true;
    }

    @Override
    public void setInsets(WindowInsets insets) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        lp.gravity = Gravity.BOTTOM;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.bottomMargin = 0;
        lp.height = grid.getHotseatBarSizePx()+insets.getSystemWindowInsetBottom();
        Rect padding = grid.getHotseatLayoutPadding();
        getLayout().setPadding(padding.left, padding.top, padding.right, padding.bottom);
        setLayoutParams(lp);
        //InsettableFrameLayout.Companion.dispatchInsets(this, insets);
    }
}
