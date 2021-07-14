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
package foundation.e.blisslauncher.uioverrides.states;

import static android.view.View.VISIBLE;
import static foundation.e.blisslauncher.features.test.RotationHelper.REQUEST_ROTATE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_OVERVIEW_FADE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_OVERVIEW_SCALE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_OVERVIEW_TRANSLATE_X;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_WORKSPACE_TRANSLATE;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.ACCEL;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.DEACCEL_2;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.OVERSHOOT_1_2;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.OVERSHOOT_1_7;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SCALE_PROPERTY;

import android.graphics.Rect;
import android.view.View;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.LauncherPagedView;
import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.quickstep.SysUINavigationMode;
import foundation.e.quickstep.views.RecentsView;
import foundation.e.quickstep.views.TaskView;

/**
 * Definition for overview state
 */
public class OverviewState extends LauncherState {

    // Scale recents takes before animating in
    private static final float RECENTS_PREPARE_SCALE = 1.33f;

    protected static final Rect sTempRect = new Rect();

    private static final int STATE_FLAGS = FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED
            | FLAG_DISABLE_RESTORE | FLAG_OVERVIEW_UI | FLAG_DISABLE_ACCESSIBILITY;

    public OverviewState(int id) {
        this(id, OVERVIEW_TRANSITION_MS, STATE_FLAGS);
    }

    protected OverviewState(int id, int transitionDuration, int stateFlags) {
        super(id, transitionDuration, stateFlags);
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(TestActivity launcher) {
        RecentsView recentsView = launcher.getOverviewPanel();
        LauncherPagedView workspace = launcher.getLauncherPagedView();
        View workspacePage = workspace.getPageAt(workspace.getCurrentPage());
        float workspacePageWidth = workspacePage != null && workspacePage.getWidth() != 0
                ? workspacePage.getWidth() : launcher.getDeviceProfile().getAvailableWidthPx();
        recentsView.getTaskSize(sTempRect);
        float scale = (float) sTempRect.width() / workspacePageWidth;
        float parallaxFactor = 0.5f;
        return new ScaleAndTranslation(scale, 0, -getDefaultSwipeHeight(launcher) * parallaxFactor);
    }

    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(TestActivity launcher) {
        if ((getVisibleElements(launcher) & HOTSEAT_ICONS) != 0) {
            // If the hotseat icons are visible in overview, keep them in their normal position.
            return super.getWorkspaceScaleAndTranslation(launcher);
        }
        return getWorkspaceScaleAndTranslation(launcher);
    }

    @Override
    public ScaleAndTranslation getOverviewScaleAndTranslation(TestActivity launcher) {
        return new ScaleAndTranslation(1f, 0f, 0f);
    }

    @Override
    public void onStateEnabled(TestActivity launcher) {
        AbstractFloatingView.closeAllOpenViews(launcher);
    }

    @Override
    public void onStateTransitionEnd(TestActivity launcher) {
        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_ROTATE);
    }

    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(TestActivity launcher) {
        return new PageAlphaProvider(DEACCEL_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0;
            }
        };
    }

    @Override
    public int getVisibleElements(TestActivity launcher) {
            return HOTSEAT_ICONS;
    }

    @Override
    public float getWorkspaceScrimAlpha(TestActivity launcher) {
        return 0.5f;
    }

    @Override
    public float getVerticalProgress(TestActivity launcher) {
        return 1f;
    }

    public static float getDefaultVerticalProgress(TestActivity launcher) {
        return 1 - (getDefaultSwipeHeight(launcher)
                / launcher.getAllAppsController().getShiftRange());
    }

    @Override
    public String getDescription(TestActivity launcher) {
        return launcher.getString(R.string.accessibility_desc_recent_apps);
    }

    public static float getDefaultSwipeHeight(TestActivity launcher) {
        return getDefaultSwipeHeight(launcher.getDeviceProfile());
    }

    public static float getDefaultSwipeHeight(VariantDeviceProfile dp) {
        return dp.ge - dp.allAppsIconTextSizePx;
    }

    @Override
    public void onBackPressed(TestActivity launcher) {
        TaskView taskView = launcher.<RecentsView>getOverviewPanel().getRunningTaskView();
        if (taskView != null) {
            taskView.launchTask(true);
        } else {
            super.onBackPressed(launcher);
        }
    }

    @Override
    public void prepareForAtomicAnimation(TestActivity launcher, LauncherState fromState,
            AnimatorSetBuilder builder) {
        if (fromState == NORMAL && this == OVERVIEW) {
            if (SysUINavigationMode.getMode(launcher) == SysUINavigationMode.Mode.NO_BUTTON) {
                builder.setInterpolator(ANIM_WORKSPACE_SCALE, ACCEL);
                builder.setInterpolator(ANIM_WORKSPACE_TRANSLATE, ACCEL);
            } else {
                builder.setInterpolator(ANIM_WORKSPACE_SCALE, OVERSHOOT_1_2);

                // Scale up the recents, if it is not coming from the side
                RecentsView overview = launcher.getOverviewPanel();
                if (overview.getVisibility() != VISIBLE || overview.getContentAlpha() == 0) {
                    SCALE_PROPERTY.set(overview, RECENTS_PREPARE_SCALE);
                }
            }
            builder.setInterpolator(ANIM_WORKSPACE_FADE, OVERSHOOT_1_2);
            builder.setInterpolator(ANIM_OVERVIEW_SCALE, OVERSHOOT_1_2);
            builder.setInterpolator(ANIM_OVERVIEW_TRANSLATE_X, OVERSHOOT_1_7);
            builder.setInterpolator(ANIM_OVERVIEW_FADE, OVERSHOOT_1_2);
        }
    }

    public static OverviewState newBackgroundState(int id) {
        return new BackgroundAppState(id);
    }

    public static OverviewState newPeekState(int id) {
        return new OverviewPeekState(id);
    }

    public static OverviewState newSwitchState(int id) {
        return new QuickSwitchState(id);
    }
}
