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
package foundation.e.quickstep.util;

import static foundation.e.blisslauncher.uioverrides.RecentsUiFactory.ROTATION_LANDSCAPE;
import static foundation.e.blisslauncher.uioverrides.RecentsUiFactory.ROTATION_SEASCAPE;
import static foundation.e.quickstep.SysUINavigationMode.Mode.NO_BUTTON;

import android.content.Context;
import android.view.Surface;
import android.view.WindowManager;

import foundation.e.blisslauncher.features.test.graphics.RotationMode;
import foundation.e.quickstep.SysUINavigationMode;

/**
 * Utility class to check nav bar position
 */
public class NavBarPosition {

    private final SysUINavigationMode.Mode mMode;
    private final int mDisplayRotation;

    public NavBarPosition(Context context) {
        mMode = SysUINavigationMode.getMode(context);
        mDisplayRotation = context.getSystemService(WindowManager.class)
                .getDefaultDisplay().getRotation();
    }

    public boolean isRightEdge() {
        return mMode != NO_BUTTON && mDisplayRotation == Surface.ROTATION_90;
    }

    public boolean isLeftEdge() {
        return mMode != NO_BUTTON && mDisplayRotation == Surface.ROTATION_270;
    }

    public RotationMode getRotationMode() {
        return isLeftEdge() ? ROTATION_SEASCAPE
                : (isRightEdge() ? ROTATION_LANDSCAPE : RotationMode.NORMAL);
    }
}
