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
package foundation.e.blisslauncher.features.quickstep.uioverrides;

import static foundation.e.blisslauncher.features.test.RotationHelper.REQUEST_ROTATE;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.DEACCEL_2;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;

import android.view.View;
import foundation.e.blisslauncher.core.customviews.LauncherPagedView;
import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;

/**
 * Definition for overview state
 */
public class OverviewState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED
        | FLAG_DISABLE_RESTORE | FLAG_OVERVIEW_UI | FLAG_DISABLE_ACCESSIBILITY;

    public OverviewState(int id) {
        this(id, OVERVIEW_TRANSITION_MS, STATE_FLAGS);
    }

    protected OverviewState(int id, int transitionDuration, int stateFlags) {
        super(id, transitionDuration, stateFlags);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(TestActivity launcher) {
        RecentsView recentsView = launcher.getOverviewPanel();
        LauncherPagedView workspace = launcher.getLauncherPagedView();
        View workspacePage = workspace.getPageAt(workspace.getCurrentPage());
        float workspacePageWidth = workspacePage != null && workspacePage.getWidth() != 0
            ? workspacePage.getWidth() : launcher.getDeviceProfile().getAvailableWidthPx();
        recentsView.getTaskSize(sTempRect);
        float scale = (float) sTempRect.width() / workspacePageWidth;
        float parallaxFactor = 0.5f;
        return new float[]{scale, 0, parallaxFactor};
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(TestActivity launcher) {
        return new float[]{1f, 0f};
    }

    @Override
    public void onStateEnabled(TestActivity launcher) {
        RecentsView rv = launcher.getOverviewPanel();
        rv.setOverviewStateEnabled(true);
        AbstractFloatingView.closeAllOpenViews(launcher);
    }

    @Override
    public void onStateDisabled(TestActivity launcher) {
        RecentsView rv = launcher.getOverviewPanel();
        rv.setOverviewStateEnabled(false);
    }

    @Override
    public void onStateTransitionEnd(TestActivity launcher) {
        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_ROTATE);
    }

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
}
