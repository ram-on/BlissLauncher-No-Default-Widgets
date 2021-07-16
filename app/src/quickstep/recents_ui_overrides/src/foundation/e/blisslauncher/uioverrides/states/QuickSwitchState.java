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

import android.os.Handler;
import android.os.Looper;

import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.quickstep.WindowTransformSwipeHandler;
import foundation.e.blisslauncher.quickstep.views.RecentsView;
import foundation.e.blisslauncher.quickstep.views.TaskView;

/**
 * State to indicate we are about to launch a recent task. Note that this state is only used when
 * quick switching from launcher; quick switching from an app uses WindowTransformSwipeHelper.
 * @see WindowTransformSwipeHandler.GestureEndTarget#NEW_TASK
 */
public class QuickSwitchState extends BackgroundAppState {

    private static final String TAG = "QuickSwitchState";

    public QuickSwitchState(int id) {
        super(id);
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(TestActivity launcher) {
        // TODO: May need to fix it.
        return new ScaleAndTranslation(1, 0, 0);
    }

    @Override
    public int getVisibleElements(TestActivity launcher) {
        return NONE;
    }

    @Override
    public void onStateTransitionEnd(TestActivity launcher) {
        TaskView tasktolaunch = launcher.<RecentsView>getOverviewPanel().getTaskViewAt(0);
        if (tasktolaunch != null) {
            tasktolaunch.launchTask(false, success -> {
                if (!success) {
                    launcher.getStateManager().goToState(OVERVIEW);
                    tasktolaunch.notifyTaskLaunchFailed(TAG);
                } else {
                    launcher.getStateManager().moveToRestState();
                }
            }, new Handler(Looper.getMainLooper()));
        } else {
            launcher.getStateManager().goToState(NORMAL);
        }
    }
}
