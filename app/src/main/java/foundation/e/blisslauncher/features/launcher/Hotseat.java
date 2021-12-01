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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.logging.Handler;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider;
import foundation.e.blisslauncher.core.blur.ShaderBlurDrawable;
import foundation.e.blisslauncher.core.customviews.Insettable;
import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout;
import foundation.e.blisslauncher.core.executors.MainThreadExecutor;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

public class Hotseat extends CellLayout implements Insettable, BlurWallpaperProvider.Listener {

    private final TestActivity mLauncher;
    private CellLayout mContent;

    private static final String TAG = "Hotseat";

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;

    private final BlurWallpaperProvider blurWallpaperProvider;
    private ShaderBlurDrawable fullBlurDrawable = null;
    private int blurAlpha = 255;
    private final Drawable.Callback blurDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            new MainThreadExecutor().execute(() -> invalidate());
        }

        @Override
        public void scheduleDrawable(
            @NonNull Drawable who, @NonNull Runnable what, long when
        ) {

        }

        @Override
        public void unscheduleDrawable(
            @NonNull Drawable who, @NonNull Runnable what
        ) {

        }
    };

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = TestActivity.Companion.getLauncher(context);
        setWillNotDraw(false);
        blurWallpaperProvider = BlurWallpaperProvider.Companion.getInstance(getContext());
        createBlurDrawable();
    }

    private void createBlurDrawable() {
        if (isAttachedToWindow() && fullBlurDrawable != null) {
            fullBlurDrawable.stopListening();
        }
        fullBlurDrawable = blurWallpaperProvider.createDrawable();
        fullBlurDrawable.setCallback(blurDrawableCallback);
        fullBlurDrawable.setBounds(getLeft(), getTop(), getRight(), getBottom());
        if (isAttachedToWindow()) fullBlurDrawable.startListening();
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        BlurWallpaperProvider.Companion.getInstance(getContext()).addListener(this);
        if (fullBlurDrawable != null) {
            fullBlurDrawable.startListening();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BlurWallpaperProvider.Companion.getInstance(getContext()).removeListener(this);
        if (fullBlurDrawable != null) {
            fullBlurDrawable.stopListening();
        }
    }

    @Override
    protected void onDraw(@Nullable Canvas canvas) {
        if (fullBlurDrawable != null) {
            fullBlurDrawable.setAlpha(blurAlpha);
            fullBlurDrawable.draw(canvas);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed && fullBlurDrawable != null) {
            fullBlurDrawable.setBounds(left, top, right, bottom);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state or an accessible drag is in progress.
        return false;
    }

    @Override
    public void setInsets(Rect insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        insets = grid.getInsets();
        lp.gravity = Gravity.BOTTOM;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = grid.getHotseatBarSizePx() + insets.bottom;
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

    @Override
    public void onWallpaperChanged() {

    }

    @Override
    public void onEnabledChanged() {
        createBlurDrawable();
    }
}
