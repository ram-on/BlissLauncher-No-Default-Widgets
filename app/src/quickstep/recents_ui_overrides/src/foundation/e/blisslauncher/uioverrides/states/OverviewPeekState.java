/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_OVERVIEW_FADE;
import static foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder.ANIM_OVERVIEW_TRANSLATE_X;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.INSTANT;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.OVERSHOOT_1_7;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;

public class OverviewPeekState extends OverviewState {
    public OverviewPeekState(int id) {
        super(id);
    }

    @Override
    public ScaleAndTranslation getOverviewScaleAndTranslation(TestActivity launcher) {
        ScaleAndTranslation result = super.getOverviewScaleAndTranslation(launcher);
        result.translationX = NORMAL.getOverviewScaleAndTranslation(launcher).translationX
                - launcher.getResources().getDimension(R.dimen.overview_peek_distance);
        return result;
    }

    @Override
    public void prepareForAtomicAnimation(TestActivity launcher, LauncherState fromState,
            AnimatorSetBuilder builder) {
        if (this == OVERVIEW_PEEK && fromState == NORMAL) {
            builder.setInterpolator(ANIM_OVERVIEW_FADE, INSTANT);
            builder.setInterpolator(ANIM_OVERVIEW_TRANSLATE_X, OVERSHOOT_1_7);
        }
    }
}
