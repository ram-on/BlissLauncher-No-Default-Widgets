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
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_HOTSEAT_SCALE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_HOTSEAT_TRANSLATE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_TRANSLATE;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.LINEAR;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.ZOOM_OUT;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SCALE_PROPERTY;
import static foundation.e.blisslauncher.features.test.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;

import android.view.View;
import android.view.animation.Interpolator;
import foundation.e.blisslauncher.core.customviews.LauncherPagedView;
import foundation.e.blisslauncher.features.launcher.Hotseat;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.PropertySetter;
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer;

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
        LauncherState.ScaleAndTranslation scaleAndTranslation = state.getWorkspaceScaleAndTranslation(mLauncher);
        LauncherState.ScaleAndTranslation hotseatScaleAndTranslation = state.getHotseatScaleAndTranslation(
            mLauncher);
        mNewScale = scaleAndTranslation.scale;
        LauncherState.PageAlphaProvider pageAlphaProvider = state.getWorkspacePageAlphaProvider(mLauncher);
        final int childCount = mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            applyChildState(state, (CellLayout) mWorkspace.getChildAt(i), i, pageAlphaProvider,
                propertySetter, builder, config);
        }

        int elements = state.getVisibleElements(mLauncher);
        Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
            pageAlphaProvider.interpolator);
        boolean playAtomicComponent = config.playAtomicOverviewScaleComponent();
        Hotseat hotseat = mWorkspace.getHotseat();
        if (playAtomicComponent) {
            Interpolator scaleInterpolator = builder.getInterpolator(ANIM_WORKSPACE_SCALE, ZOOM_OUT);
            propertySetter.setFloat(mWorkspace, SCALE_PROPERTY, mNewScale, scaleInterpolator);

            DragLayer dragLayer = mLauncher.getDragLayer();
            float[] workspacePivot =
                new float[]{ mWorkspace.getPivotX(), mWorkspace.getPivotY() };
            dragLayer.getDescendantCoordRelativeToSelf(mWorkspace, workspacePivot);
            dragLayer.mapCoordInSelfToDescendant(hotseat, workspacePivot);
            hotseat.setPivotX(workspacePivot[0]);
            hotseat.setPivotY(workspacePivot[1]);
            float hotseatScale = hotseatScaleAndTranslation.scale;
            Interpolator hotseatScaleInterpolator = builder.getInterpolator(ANIM_HOTSEAT_SCALE,
                scaleInterpolator);
            propertySetter.setFloat(hotseat, SCALE_PROPERTY, hotseatScale,
                hotseatScaleInterpolator);

            float hotseatIconsAlpha = (elements & HOTSEAT_ICONS) != 0 ? 1 : 0;
            propertySetter.setViewAlpha(hotseat, hotseatIconsAlpha, fadeInterpolator);
            propertySetter.setViewAlpha(mLauncher.getLauncherPagedView().getPageIndicator(),
                hotseatIconsAlpha, fadeInterpolator);
        }

        if (!config.playNonAtomicComponent()) {
            // Only the alpha and scale, handled above, are included in the atomic animation.
            return;
        }

        Interpolator translationInterpolator = !playAtomicComponent
            ? LINEAR
            : builder.getInterpolator(ANIM_WORKSPACE_TRANSLATE, ZOOM_OUT);
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_X,
            scaleAndTranslation.translationX, translationInterpolator);
        propertySetter.setFloat(mWorkspace, View.TRANSLATION_Y,
            scaleAndTranslation.translationY, translationInterpolator);

        Interpolator hotseatTranslationInterpolator = builder.getInterpolator(
            ANIM_HOTSEAT_TRANSLATE, translationInterpolator);
        propertySetter.setFloat(hotseat, View.TRANSLATION_Y,
            hotseatScaleAndTranslation.translationY, hotseatTranslationInterpolator);
        propertySetter.setFloat(mWorkspace.getPageIndicator(), View.TRANSLATION_Y,
            hotseatScaleAndTranslation.translationY, hotseatTranslationInterpolator);
        setScrim(propertySetter, state);
    }


    // TODO: Enable it when find a fix for Tonal class.
    public void setScrim(PropertySetter propertySetter, LauncherState state) {
        /*WorkspaceAndHotseatScrim scrim = mLauncher.getDragLayer().getScrim();
        propertySetter.setFloat(scrim, SCRIM_PROGRESS, state.getWorkspaceScrimAlpha(mLauncher),
            LINEAR);
        propertySetter.setFloat(scrim, SYSUI_PROGRESS, state.hasSysUiScrim ? 1 : 0, LINEAR);*/
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

        if (config.playAtomicOverviewScaleComponent()) {
            Interpolator fadeInterpolator = builder.getInterpolator(ANIM_WORKSPACE_FADE,
                pageAlphaProvider.interpolator);
            propertySetter.setFloat(cl, View.ALPHA,
                pageAlpha, fadeInterpolator);
        }
    }
}