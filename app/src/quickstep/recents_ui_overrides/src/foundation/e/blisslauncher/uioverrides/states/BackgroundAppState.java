/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;

import foundation.e.blisslauncher.core.customviews.AbstractFloatingView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.quickstep.util.LayoutUtils;
import foundation.e.blisslauncher.quickstep.views.RecentsView;
import foundation.e.blisslauncher.quickstep.views.TaskView;

/**
 * State indicating that the Launcher is behind an app
 */
public class BackgroundAppState extends OverviewState {

    private static final int STATE_FLAGS =
            FLAG_DISABLE_RESTORE | FLAG_OVERVIEW_UI | FLAG_DISABLE_ACCESSIBILITY
                    | FLAG_DISABLE_INTERACTION;

    public BackgroundAppState(int id) {
        super(id, OVERVIEW_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public void onStateEnabled(TestActivity launcher) {
        AbstractFloatingView.closeAllOpenViews(launcher, false);
    }

    @Override
    public float getVerticalProgress(TestActivity launcher) {
        /*int transitionLength = LayoutUtils.getShelfTrackingDistance(launcher,
                launcher.getDeviceProfile());
        float progressDelta = (transitionLength / scrollRange);
        return super.getVerticalProgress(launcher) + progressDelta;*/
        return 1f;
    }

    @Override
    public LauncherState.ScaleAndTranslation getOverviewScaleAndTranslation(TestActivity launcher) {
        // Initialize the recents view scale to what it would be when starting swipe up
        RecentsView recentsView = launcher.getOverviewPanel();
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) {
            return super.getOverviewScaleAndTranslation(launcher);
        }
        TaskView dummyTask = recentsView.getTaskViewAt(Math.max(taskCount - 1,
                recentsView.getCurrentPage()));
        return recentsView.getTempClipAnimationHelper().updateForFullscreenOverview(dummyTask)
                .getScaleAndTranslation();
    }

    @Override
    public float getOverviewFullscreenProgress() {
        return 1;
    }

    @Override
    public int getVisibleElements(TestActivity launcher) {
        return super.getVisibleElements(launcher)
                & ~RECENTS_CLEAR_ALL_BUTTON;
    }

    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(TestActivity launcher) {
        if ((getVisibleElements(launcher) & HOTSEAT_ICONS) != 0) {
            // Translate hotseat offscreen if we show it in overview.
            ScaleAndTranslation scaleAndTranslation = super.getHotseatScaleAndTranslation(launcher);
            scaleAndTranslation.translationY = LayoutUtils.getShelfTrackingDistance(launcher,
                    launcher.getDeviceProfile());
            return scaleAndTranslation;
        }
        return super.getHotseatScaleAndTranslation(launcher);
    }
}
