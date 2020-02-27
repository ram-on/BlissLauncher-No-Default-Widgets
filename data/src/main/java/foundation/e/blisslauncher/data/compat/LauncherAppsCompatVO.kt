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
package foundation.e.blisslauncher.data.compat

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.PackageManager
import android.os.Parcelable
import android.os.UserHandle
import androidx.annotation.Nullable

@TargetApi(26)
class LauncherAppsCompatVO internal constructor(context: Context) :
    LauncherAppsCompatVL(context) {

    override fun getApplicationInfo(
        packageName: String,
        flags: Int,
        user: UserHandle
    ): ApplicationInfo? {
        return try {
            val info = launcherApps.getApplicationInfo(packageName, flags, user)
            if (info.flags and ApplicationInfo.FLAG_INSTALLED == 0 || !info.enabled) null else info
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    companion object {
        //TODO
        /*@Override
        public List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(
                @Nullable PackageUserKey packageUser) {
            List<ShortcutConfigActivityInfo> result = new ArrayList<>();
            UserHandle myUser = Process.myUserHandle();

            final List<UserHandle> users;
            final String packageName;
            if (packageUser == null) {
                users = UserManagerCompat.getInstance(mContext).getUserProfiles();
                packageName = null;
            } else {
                users = new ArrayList<>(1);
                users.add(packageUser.mUser);
                packageName = packageUser.mPackageName;
            }
            for (UserHandle user : users) {
                boolean ignoreTargetSdk = myUser.equals(user);
                List<LauncherActivityInfo> activities =
                        mLauncherApps.getShortcutConfigActivityList(packageName, user);
                for (LauncherActivityInfo activityInfo : activities) {
                    if (ignoreTargetSdk || activityInfo.getApplicationInfo().targetSdkVersion >=
                            Build.VERSION_CODES.O) {
                        result.add(new ShortcutConfigActivityInfoVO(activityInfo));
                    }
                }
            }

            return result;
        }*/

        /**
         * request.accept() will initiate the following flow:
         * -> go-to-system-process for actual processing (a)
         * -> callback-to-launcher on UI thread (b)
         * -> post callback on the worker thread (c)
         * -> Update model and unpin (in system) any shortcut not in out model. (d)
         *
         * Note that (b) will take at-least one frame as it involves posting callback from binder
         * thread to UI thread.
         * If (d) happens before we add this shortcut to our model, we will end up unpinning
         * the shortcut in the system.
         * Here its the caller's responsibility to add the newly created ShortcutInfo immediately
         * to the model (which may involves a single post-to-worker-thread). That will guarantee
         * that (d) happens after model is updated.
         */

        //TODO for shortcuts
        @Nullable
        /*fun createShortcutInfoFromPinItemRequest(
            context: Context?, request: PinItemRequest?, acceptDelay: Long
        ): ShortcutInfo? {
            return if (request != null && request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT &&
                request.isValid
            ) {
                if (acceptDelay <= 0) {
                    if (!request.accept()) {
                        return null
                    }
                } else { // Block the worker thread until the accept() is called.
                    LooperExecutor(LauncherModel.getWorkerLooper()).execute(Runnable {
                        try {
                            Thread.sleep(acceptDelay)
                        } catch (e: InterruptedException) { // Ignore
                        }
                        if (request.isValid) {
                            request.accept()
                        }
                    })
                }
                val compat =
                    ShortcutInfoCompat(request.shortcutInfo)
                val info = ShortcutInfo(compat, context)
                // Apply the unbadged icon and fetch the actual icon asynchronously.
                val li: LauncherIcons = LauncherIcons.obtain(context)
                li.createShortcutIcon(compat, false *//* badged *//*).applyTo(info)
                li.recycle()
                LauncherAppState.getInstance(context).getModel()
                    .updateAndBindShortcutInfo(info, compat)
                info
            } else {
                null
            }
        }*/

        fun getPinItemRequest(intent: Intent): PinItemRequest? {
            val extra =
                intent.getParcelableExtra<Parcelable>(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
            return if (extra is PinItemRequest) extra else null
        }
    }
}