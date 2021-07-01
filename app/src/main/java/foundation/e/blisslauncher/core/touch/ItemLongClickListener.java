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

import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;

import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions;

/**
 * Class to handle long-clicks on workspace items and start drag as a result.
 */
public class ItemLongClickListener {

    public static OnLongClickListener INSTANCE_WORKSPACE =
        ItemLongClickListener::onWorkspaceItemLongClick;

    private static final String TAG = "ItemLongClickListener";

    private static boolean onWorkspaceItemLongClick(View v) {
        int[] temp = new int[2];
        v.getLocationOnScreen(temp);
        Log.i(TAG,
            "onWorkspaceItemLongClick: [" + v.getLeft() + ", " + v.getTop() + "] ["+temp[0]+", "+temp[1]+"]"
        );
        TestActivity launcher = TestActivity.Companion.getLauncher(v.getContext());
        if (!canStartDrag(launcher)) return false;
        //if (!launcher.isInState(NORMAL) && !launcher.isInState(OVERVIEW)) return false;
        if (!(v.getTag() instanceof LauncherItem)) return false;

        //launcher.setWaitingForResult(null);
        beginDrag(v, launcher, (LauncherItem) v.getTag(), new DragOptions());
        return true;
    }

    public static void beginDrag(
        View v, TestActivity launcher, LauncherItem info,
        DragOptions dragOptions
    ) {
        //TODO: Enable when supporting folders
       /* if (info.container >= 0) {
            Folder folder = Folder.getOpen(launcher);
            if (folder != null) {
                if (!folder.getItemsInReadingOrder().contains(v)) {
                    folder.close(true);
                } else {
                    folder.startDrag(v, dragOptions);
                    return;
                }
            }
        }*/

        CellLayout.CellInfo longClickCellInfo = new CellLayout.CellInfo(v, info);
        launcher.getWorkspace().startDrag(longClickCellInfo, dragOptions);
    }

    public static boolean canStartDrag(TestActivity launcher) {
        if (launcher == null) {
            return false;
        }
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        if (launcher.isWorkspaceLocked()) return false;
        // Return early if an item is already being dragged (e.g. when long-pressing two shortcuts)
        if (launcher.getDragController().isDragging()) return false;

        return true;
    }
}
