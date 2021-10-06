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

package foundation.e.blisslauncher.features.shortcuts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.features.test.LauncherAppState;
import foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils;

/**
 * Wrapper class for representing a shortcut configure activity.
 */
public class ShortcutConfigActivityInfo implements ComponentWithLabel {

    private static final String TAG = "SCActivityInfo";

    // Class name used in the target component, such that it will never represent an
    // actual existing class.
    private static final String DUMMY_COMPONENT_CLASS = "pinned-shortcut";

    private final LauncherApps.PinItemRequest mRequest;
    private final ShortcutInfo mInfo;
    private final Context mContext;
    private final ComponentName mCn;
    private final UserHandle mUser;

    public ShortcutConfigActivityInfo(LauncherApps.PinItemRequest request, Context context) {
        mCn = new ComponentName(request.getShortcutInfo().getPackage(), DUMMY_COMPONENT_CLASS);
        mUser = request.getShortcutInfo().getUserHandle();
        mRequest = request;
        mInfo = request.getShortcutInfo();
        mContext = context;
    }

    @Override
    public ComponentName getComponent() {
        return mCn;
    }

    @Override
    public UserHandle getUser() {
        return mUser;
    }

    @Override
    public CharSequence getLabel(PackageManager pm) {
        return mInfo.getShortLabel();
    }

    public int getItemType() {
        return Constants.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Return a WorkspaceItemInfo, if it can be created directly on drop, without requiring any
     * {@link #startConfigActivity(Activity, int)}.
     */
    public LauncherItem createWorkspaceItemInfo() {
        return null;
    }

    public boolean startConfigActivity(Activity activity, int requestCode) {
        return false;
    }

    public Drawable getFullResIcon() {
        Drawable d = mContext.getSystemService(LauncherApps.class)
            .getShortcutIconDrawable(mInfo, LauncherAppState.getIDP(mContext).getFillResIconDpi());
        return d;
    }

    /*public ShortcutItem createShortcutItemInfo() {
        // Total duration for the drop animation to complete.
        long duration = mContext.getResources().getInteger(R.integer.config_dropAnimMaxDuration) +
            LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY +
            LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;
        // Delay the actual accept() call until the drop animation is complete.
        return createWorkspaceItemFromPinItemRequest(
            mContext, mRequest, duration);
    }*/
}
