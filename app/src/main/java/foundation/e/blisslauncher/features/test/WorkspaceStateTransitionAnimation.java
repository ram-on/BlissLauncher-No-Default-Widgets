/*
 * Copyright (C) 2015 The Android Open Source Project
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

package foundation.e.blisslauncher.features.test;

import static foundation.e.blisslauncher.features.test.LauncherState.HOTSEAT_ICONS;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.LINEAR;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.ZOOM_OUT;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SCALE_PROPERTY;
import static foundation.e.blisslauncher.features.test.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;

import android.view.View;
import android.view.animation.Interpolator;
import foundation.e.blisslauncher.core.customviews.LauncherPagedView;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.PropertySetter;

/**
 * Manages the animations between each of the workspace states.
 */
public class WorkspaceStateTransitionAnimation {

    private final TestActivity mLauncher;
    private final LauncherPagedView mWorkspace;

    private float mNewScale;

    public WorkspaceStateTransitionAnimation(TestActivity launcher, LauncherPagedView workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
    }

    public void setState(LauncherState toState) {
        setWorkspaceProperty(toState, NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(),
                new LauncherStateManager.AnimationConfig());
    }

    public void setStateWithAnimation(LauncherState toState, AnimatorSetBuilder builder,
            LauncherStateManager.AnimationConfig config) {
        setWorkspaceProperty(toState, config.getPropertySetter(builder), builder, config);
    }

    public float getFinalScale() {
        return mNewScale;
    }

    /**
     * Starts a transition animation for the workspace.
     */
    private void setWorkspaceProperty(LauncherState state, PropertySetter propertySetter,
            AnimatorSetBuilder builder, LauncherStateManager.AnimationConfig config) {
        float[] scaleAndTranslation = state.getWorkspaceScaleAndTranslation(mLauncher);
        mNewScale = scaleAndTranslation[0];
        LauncherState.PageAlphaProvider pageAlphaProvider = state.getWorkspacePageAlphaProvider(mLauncher);
        final int childCount = mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            applyChildState(state, (CellLayout) mWorkspace.getChildAt(i), i, pageAlphaProvider,
                    propertySetter, builder, config);
        }

        int elements = state.getVisibleElements(mLauncher);
        Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                pageAlphaProvider.interpolator);
        boolean playAtomicComponent = config.playAtomicComponent();
        if (playAtomicComponent) {
            Interpolator scaleInterpolator = builder.getInterpolator(ANIM_WORKSPACE_SCALE, ZOOM_OUT);
            propertySetter.setFloat(mWorkspace, SCALE_PROPERTY, mNewScale, scaleInterpolator);
            float hotseatIconsAlpha = (elements & HOTSEAT_ICONS) != 0 ? 1 : 0;
            propertySetter.setViewAlpha(mLauncher.getHotseat().getLayout(), hotseatIconsAlpha,
                    fadeInterpolator);
            propertySetter.setViewAlpha(mLauncher.getLauncherPagedView().getPageIndicator(),
                    hotseatIconsAlpha, fadeInterpolator);
        }

        if (!config.playNonAtomicComponent()) {
            // Only the alpha and scale, handled above, are included in the atomic animation.
            return;
        }

        Interpolator translationInterpolator = !playAtomicComponent ? LINEAR : ZOOM_OUT;
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_X,
                scaleAndTranslation[1], translationInterpolator);
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_Y,
                scaleAndTranslation[2], translationInterpolator);
    }

    public void applyChildState(LauncherState state, CellLayout cl, int childIndex) {
        applyChildState(state, cl, childIndex, state.getWorkspacePageAlphaProvider(mLauncher),
                NO_ANIM_PROPERTY_SETTER, new AnimatorSetBuilder(), new LauncherStateManager.AnimationConfig());
    }

    private void applyChildState(LauncherState state, CellLayout cl, int childIndex,
            LauncherState.PageAlphaProvider pageAlphaProvider, PropertySetter propertySetter,
            AnimatorSetBuilder builder, LauncherStateManager.AnimationConfig config) {
        float pageAlpha = pageAlphaProvider.getPageAlpha(childIndex);
        int drawableAlpha = Math.round(pageAlpha * (state.hasWorkspacePageBackground ? 255 : 0));

        if (config.playAtomicComponent()) {
            Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                    pageAlphaProvider.interpolator);
            propertySetter.setFloat(cl, View.ALPHA,
                    pageAlpha, fadeInterpolator);
        }
    }
}