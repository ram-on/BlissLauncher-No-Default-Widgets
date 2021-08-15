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

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.features.test.IconTextView;
import foundation.e.blisslauncher.features.test.TestActivity;

/**
 * Class for handling clicks on workspace and all-apps items
 */
public class ItemClickHandler {

    /**
     * Instance used for click handling on items
     */
    public static final OnClickListener INSTANCE = ItemClickHandler::onClick;

    private static void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        TestActivity launcher = TestActivity.Companion.getLauncher(v.getContext());
        //TODO:
        /*if (!launcher.getWorkspace().isFinishedSwitchingState()) {
            return;
        }*/

        if(v instanceof IconTextView) {
            boolean result = ((IconTextView) v).tryToHandleUninstallClick(launcher);
            if(result) return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutItem) {
            onClickAppShortcut(v, (ShortcutItem) tag, launcher);
        } else if (tag instanceof FolderItem) {
            onClickFolderIcon(v, launcher);
        } else if (tag instanceof ApplicationItem) {
            startAppShortcutOrInfoActivity(v, (ApplicationItem) tag, launcher);
        }
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link IconTextView}.
     * @param launcher Launcher activity to pass actions.
     */
    private static void onClickFolderIcon(
        View v,
        TestActivity launcher
    ) {
        Log.d("ItemClick", "onClickFolderIcon() called with: v = [" + v + "]");
        launcher.openFolder(v);
    }

    /**
     * Event handler for the app widget view which has not fully restored.
     */
    /*private static void onClickPendingWidget(PendingAppWidgetHostView v, Launcher launcher) {
        if (launcher.getPackageManager().isSafeMode()) {
            Toast.makeText(launcher, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            LauncherAppWidgetProviderInfo appWidgetInfo = AppWidgetManagerCompat
                    .getInstance(launcher).findProvider(info.providerName, info.user);
            if (appWidgetInfo == null) {
                return;
            }
            WidgetAddFlowHandler addFlowHandler = new WidgetAddFlowHandler(appWidgetInfo);

            if (info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // This should not happen, as we make sure that an Id is allocated during bind.
                    return;
                }
                addFlowHandler.startBindFlow(launcher, info.appWidgetId, info,
                        REQUEST_BIND_PENDING_APPWIDGET);
            } else {
                addFlowHandler.startConfigActivity(launcher, info, REQUEST_RECONFIGURE_APPWIDGET);
            }
        } else {
            final String packageName = info.providerName.getPackageName();
            onClickPendingAppItem(v, launcher, packageName, info.installProgress >= 0);
        }
    }*/

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutItem}.
     */
    private static void onClickAppShortcut(View v, LauncherItem shortcut, TestActivity launcher) {
        /*if (shortcut.isDisabled()) {
            final int disabledFlags = shortcut.runtimeStatusFlags & ShortcutInfo.FLAG_DISABLED_MASK;
            if ((disabledFlags &
                    ~FLAG_DISABLED_SUSPENDED &
                    ~FLAG_DISABLED_QUIET_USER) == 0) {
                // If the app is only disabled because of the above flags, launch activity anyway.
                // Framework will tell the user why the app is suspended.
            } else {
                if (!TextUtils.isEmpty(shortcut.disabledMessage)) {
                    // Use a message specific to this shortcut, if it has one.
                    Toast.makeText(launcher, shortcut.disabledMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Otherwise just use a generic error message.
                int error = R.string.activity_not_available;
                if ((shortcut.runtimeStatusFlags & FLAG_DISABLED_SAFEMODE) != 0) {
                    error = R.string.safemode_shortcut_error;
                } else if ((shortcut.runtimeStatusFlags & FLAG_DISABLED_BY_PUBLISHER) != 0 ||
                        (shortcut.runtimeStatusFlags & FLAG_DISABLED_LOCKED_USER) != 0) {
                    error = R.string.shortcut_not_available;
                }
                Toast.makeText(launcher, error, Toast.LENGTH_SHORT).show();
                return;
            }
        }*/

        // Start activities
        startAppShortcutOrInfoActivity(v, shortcut, launcher);
    }

    private static void startAppShortcutOrInfoActivity(View v, LauncherItem item, TestActivity launcher) {
        Intent intent;
        intent = item.getIntent();
        if (intent == null) {
            throw new IllegalArgumentException("Input must have a valid intent");
        }
        if (item instanceof ShortcutItem) {
            ShortcutItem si = (ShortcutItem) item;
            /*if (si.hasStatusFlag(ShortcutInfo.FLAG_SUPPORTS_WEB_UI)
                    && intent.getAction() == Intent.ACTION_VIEW) {
                // make a copy of the intent that has the package set to null
                // we do this because the platform sometimes disables instant
                // apps temporarily (triggered by the user) and fallbacks to the
                // web ui. This only works though if the package isn't set
                intent = new Intent(intent);
                intent.setPackage(null);
            }*/
        }
        launcher.startActivitySafely(v, intent, item);
    }
}
