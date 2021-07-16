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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.Insettable;
import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

public class Hotseat extends CellLayout implements Insettable {

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
        setBackgroundColor(0x33000000);
    }

    // TODO: Remove this later.
    public CellLayout getLayout() {
        return this;
    }

    public void resetLayout(boolean hasVerticalHotseat) {
        removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
        VariantDeviceProfile idp = mLauncher.getDeviceProfile();
        setGridSize(idp.getInv().getNumHotseatIcons(), 1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state or an accessible drag is in progress.
        return false;
    }

    @Override
    public void setInsets(WindowInsets insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        lp.gravity = Gravity.BOTTOM;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        Log.d(TAG, "Bottom inset: "+insets.getSystemWindowInsetBottom());
        lp.height = grid.getHotseatBarSizePx() + insets.getSystemWindowInsetBottom();
        Rect padding = grid.getHotseatLayoutPadding();
        getLayout().setPadding(padding.left, padding.top, padding.right, padding.bottom);
        setLayoutParams(lp);
        InsettableFrameLayout.Companion.dispatchInsets(this, insets);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Don't let if follow through to workspace
        return true;
    }
}
