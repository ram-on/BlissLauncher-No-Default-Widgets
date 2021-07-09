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
package foundation.e.blisslauncher.features.quickstep.uioverrides;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.features.quickstep.QuickScrubController;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

/**
 * Extension of overview state used for QuickScrub
 */
public class FastOverviewState extends OverviewState {

    private static final float MAX_PREVIEW_SCALE_UP = 1.3f;
    /**
     * Vertical transition of the task previews relative to the full container.
     */
    public static final float OVERVIEW_TRANSLATION_FACTOR = 0.4f;

    private static final int STATE_FLAGS = FLAG_DISABLE_RESTORE | FLAG_DISABLE_INTERACTION
            | FLAG_OVERVIEW_UI | FLAG_HIDE_BACK_BUTTON | FLAG_DISABLE_ACCESSIBILITY;

    public FastOverviewState(int id) {
        super(id, QuickScrubController.QUICK_SCRUB_FROM_HOME_START_DURATION, STATE_FLAGS);
    }

    @Override
    public void onStateTransitionEnd(TestActivity launcher) {
        super.onStateTransitionEnd(launcher);
        RecentsView recentsView = launcher.getOverviewPanel();
        recentsView.getQuickScrubController().onFinishedTransitionToQuickScrub();
    }

    @Override
    public int getVisibleElements(TestActivity launcher) {
        return NONE;
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(TestActivity launcher) {
        RecentsView recentsView = launcher.getOverviewPanel();
        recentsView.getTaskSize(sTempRect);

        return new float[] {getOverviewScale(launcher.getDeviceProfile(), sTempRect, launcher),
                OVERVIEW_TRANSLATION_FACTOR};
    }

    public static float getOverviewScale(VariantDeviceProfile dp, Rect taskRect, Context context) {

        Resources res = context.getResources();
        float usedHeight = taskRect.height() + res.getDimension(R.dimen.task_thumbnail_top_margin);
        float usedWidth = taskRect.width() + 2 * (res.getDimension(R.dimen.recents_page_spacing)
                + res.getDimension(R.dimen.quickscrub_adjacent_visible_width));
        return Math.min(Math.min(dp.getAvailableHeightPx() / usedHeight,
                dp.getAvailableWidthPx() / usedWidth), MAX_PREVIEW_SCALE_UP);
    }

    @Override
    public void onStateDisabled(TestActivity launcher) {
        super.onStateDisabled(launcher);
        launcher.<RecentsView>getOverviewPanel().getQuickScrubController().cancelActiveQuickscrub();
    }
}
