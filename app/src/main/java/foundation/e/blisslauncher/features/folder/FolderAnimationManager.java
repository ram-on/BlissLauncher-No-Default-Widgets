/*
 * Copyright (C) 2017 The Android Open Source Project
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

package foundation.e.blisslauncher.features.folder;

import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SCALE_PROPERTY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.Property;
import android.view.View;
import android.view.animation.AnimationUtils;
import androidx.viewpager.widget.ViewPager;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.Folder;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer;

/**
 * Manages the opening and closing animations for a {@link Folder}.
 * <p>
 * All of the animations are done in the Folder.
 * ie. When the user taps on the FolderIcon, we immediately hide the FolderIcon and show the Folder
 * in its place before starting the animation.
 */
public class FolderAnimationManager {

    private Folder mFolder;
    private ViewPager mContent;
    private GradientDrawable mFolderBackground;

    private FolderIcon mFolderIcon;

    private Context mContext;
    private TestActivity mLauncher;

    private final boolean mIsOpening;

    private final int mDuration;
    private final int mDelay;

    private final TimeInterpolator mFolderInterpolator;

    public FolderAnimationManager(Folder folder, boolean isOpening) {
        mFolder = folder;
        mContent = folder.mContent;

        mFolderIcon = folder.getFolderIcon();

        mContext = folder.getContext();
        mLauncher = folder.getLauncher();

        mIsOpening = isOpening;

        Resources res = mContent.getResources();
        mDuration = res.getInteger(R.integer.config_materialFolderExpandDuration);
        mDelay = res.getInteger(R.integer.config_folderDelay);

        mFolderInterpolator = AnimationUtils.loadInterpolator(
            mContext,
            R.anim.folder_interpolator
        );
    }

    /**
     * Prepares the Folder for animating between open / closed states.
     */
    public AnimatorSet getAnimator() {
        final DragLayer.LayoutParams lp = (DragLayer.LayoutParams) mFolder.getLayoutParams();

        // Match position of the FolderIcon
        final Rect folderIconPos = new Rect();
        float scaleRelativeToDragLayer = mLauncher.getDragLayer()
            .getDescendantRectRelativeToSelf(mFolderIcon, folderIconPos);
        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        Rect dragLayerBounds = new Rect();
        Point globalOffset = new Point();

        mLauncher.getDragLayer()
            .getGlobalVisibleRect(dragLayerBounds, globalOffset);
        float startScale = ((float) folderIconPos.width() / dragLayerBounds.width());
        float startHeight = startScale * dragLayerBounds.height();
        float deltaHeight = (startHeight - folderIconPos.height()) / 2;
        folderIconPos.top -= deltaHeight;
        folderIconPos.bottom += deltaHeight;
        float initialScale = (float) folderIconPos.height() / dragLayerBounds.height();
        final float finalScale = 1f;
        float scale = mIsOpening ? initialScale : finalScale;
        float initialAlpha = 0f;
        float finalAlpha = 1f;
        mFolder.setScaleX(scale);
        mFolder.setScaleY(scale);
        mFolder.setPivotX(0);
        mFolder.setPivotY(0);

        // Create the animators.
        AnimatorSet a = new AnimatorSet();

        play(
            a,
            getAnimator(mFolder,
                View.X,
                folderIconPos.left,
                dragLayerBounds.left
            )
        );
        play(a, getAnimator(mFolder, View.Y, folderIconPos.top, dragLayerBounds.top));
        play(a, getAnimator(mFolder, SCALE_PROPERTY, initialScale, finalScale));
        play(a, getAnimator(mFolder, View.ALPHA, initialAlpha, finalAlpha));
        // play(a, getAnimator(mFolderBackground, "color", initialColor, finalColor));

        // Animate the elevation midway so that the shadow is not noticeable in the background.
        int midDuration = mDuration / 2;
        Animator z = getAnimator(mFolder, View.TRANSLATION_Z, -mFolder.getElevation(), 0);
        play(a, z, mIsOpening ? midDuration : 0, midDuration);

        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mFolder.setVisibility(View.VISIBLE);
                mFolder.setPivotX(0f);
                mFolder.setPivotY(0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (mIsOpening) {
                    mFolder.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFolder.setScaleX(1f);
                mFolder.setScaleY(1f);
                if (!mIsOpening) {
                    mFolder.setVisibility(View.GONE);
                }
            }
        });
        return a;
    }

    private void play(AnimatorSet as, Animator a) {
        play(as, a, a.getStartDelay(), mDuration);
    }

    private void play(AnimatorSet as, Animator a, long startDelay, int duration) {
        a.setStartDelay(startDelay);
        a.setDuration(duration);
        as.play(a);
    }

    private TimeInterpolator getPreviewItemInterpolator() {
        return mFolderInterpolator;
    }

    private Animator getAnimator(View view, Property property, float v1, float v2) {
        return mIsOpening
            ? ObjectAnimator.ofFloat(view, property, v1, v2)
            : ObjectAnimator.ofFloat(view, property, v2, v1);
    }

    private Animator getAnimator(GradientDrawable drawable, String property, int v1, int v2) {
        return mIsOpening
            ? ObjectAnimator.ofArgb(drawable, property, v1, v2)
            : ObjectAnimator.ofArgb(drawable, property, v2, v1);
    }
}
