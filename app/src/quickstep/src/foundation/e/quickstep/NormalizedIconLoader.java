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
package foundation.e.quickstep;

import android.annotation.TargetApi;
import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.util.LruCache;
import android.util.SparseArray;

import com.android.systemui.shared.recents.model.IconLoader;
import com.android.systemui.shared.recents.model.TaskKeyLruCache;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.IconsHandler;

/**
 * Extension of {@link IconLoader} with icon normalization support
 */
@TargetApi(Build.VERSION_CODES.O)
public class NormalizedIconLoader extends IconLoader {

    private final SparseArray<Drawable> mDefaultIcons = new SparseArray<>();

    public NormalizedIconLoader(Context context, TaskKeyLruCache<Drawable> iconCache,
            LruCache<ComponentName, ActivityInfo> activityInfoCache,
            boolean disableColorExtraction) {
        super(context, iconCache, activityInfoCache);
    }

    @Override
    public Drawable getDefaultIcon(int userId) {
        synchronized (mDefaultIcons) {
            Drawable info = mDefaultIcons.get(userId);
            if (info == null) {
                info = getBitmapInfo(Resources.getSystem()
                    .getDrawable(android.R.drawable.sym_def_app_icon), userId);
                mDefaultIcons.put(userId, info);
            }

            return info;
        }
    }

    @Override
    protected Drawable createBadgedDrawable(Drawable drawable, int userId, TaskDescription desc) {
        return getBitmapInfo(drawable, userId);
    }

    private synchronized Drawable getBitmapInfo(Drawable drawable, int userId) {
        IconsHandler iconsHandler = BlissLauncher.getApplication(mContext).getIconsHandler();
        return iconsHandler.getBadgedIcon(drawable, UserHandle.of(userId));
    }

    @Override
    protected Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int userId,
            TaskDescription desc) {
        return getBitmapInfo(
            activityInfo.loadUnbadgedIcon(mContext.getPackageManager()),
            userId);
    }
}
