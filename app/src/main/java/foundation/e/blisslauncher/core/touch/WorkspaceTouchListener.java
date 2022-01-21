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
package foundation.e.blisslauncher.core.touch;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import foundation.e.blisslauncher.core.customviews.LauncherPagedView;
import foundation.e.blisslauncher.features.test.TestActivity;

/**
 * Helper class to handle touch on empty space in workspace and show options popup on long press
 */
public class WorkspaceTouchListener extends GestureDetector.SimpleOnGestureListener
        implements OnTouchListener {

    /**
     * STATE_PENDING_PARENT_INFORM is the state between longPress performed & the next motionEvent.
     * This next event is used to send an ACTION_CANCEL to Workspace, to that it clears any
     * temporary scroll state. After that, the state is set to COMPLETED, and we just eat up all
     * subsequent motion events.
     */
    private static final int STATE_CANCELLED = 0;
    private static final int STATE_REQUESTED = 1;
    private static final int STATE_PENDING_PARENT_INFORM = 2;
    private static final int STATE_COMPLETED = 3;

    private final Rect mTempRect = new Rect();
    private final TestActivity mLauncher;
    private final LauncherPagedView mWorkspace;
    private final PointF mTouchDownPoint = new PointF();
    private final float mTouchSlop;

    private final GestureDetector mGestureDetector;

    public WorkspaceTouchListener(TestActivity launcher, LauncherPagedView workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
        // Use twice the touch slop as we are looking for long press which is more
        // likely to cause movement.
        mTouchSlop = 2 * ViewConfiguration.get(launcher).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(workspace.getContext(), this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();
        if (action == ACTION_DOWN) {
            mWorkspace.onTouchEvent(ev);
            // Return true to keep receiving touch events
            return true;
        }

        final boolean result = false;

        if (action == ACTION_UP || action == ACTION_POINTER_UP) {
            if (!mWorkspace.isHandlingTouch()) {
                final View currentPage = mWorkspace.getChildAt(mWorkspace.getCurrentPage());
                if (currentPage != null) {
                    mWorkspace.onWallpaperTap(ev);
                }
            }
        }
        return result;
    }

}
