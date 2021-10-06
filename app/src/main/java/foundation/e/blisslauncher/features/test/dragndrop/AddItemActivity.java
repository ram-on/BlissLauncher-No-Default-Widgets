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

package foundation.e.blisslauncher.features.test.dragndrop;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.PinItemRequest;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;

import foundation.e.blisslauncher.features.shortcuts.InstallShortcutReceiver;
import foundation.e.blisslauncher.features.shortcuts.ShortcutConfigActivityInfo;
import foundation.e.blisslauncher.features.test.BaseActivity;
import foundation.e.blisslauncher.features.test.InvariantDeviceProfile;
import foundation.e.blisslauncher.features.test.LauncherAppState;

@TargetApi(Build.VERSION_CODES.O)
public class AddItemActivity extends BaseActivity {

    private PinItemRequest mRequest;
    private LauncherAppState mApp;
    private InvariantDeviceProfile mIdp;

    private boolean mFinishOnPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getAction().equalsIgnoreCase(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT)) {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
             mRequest = launcherApps.getPinItemRequest(getIntent());
            /*
            if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                InstallShortcutReceiver.queueShortcut(
                    new ShortcutInfoCompat(request.getShortcutInfo()), this.getApplicationContext());
                request.accept();
                finish();
            }*/
        }

        if (mRequest == null) {
            finish();
            return;
        }

        mApp = LauncherAppState.getInstance(this);
        mIdp = mApp.getInvariantDeviceProfile();

        // Use the application context to get the device profile, as in multiwindow-mode, the
        // confirmation activity might be rotated.
        mDeviceProfile = mIdp.getDeviceProfile(getApplicationContext());

        if (mRequest.getRequestType() == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            setupShortcut();
            InstallShortcutReceiver.queueShortcut(mRequest.getShortcutInfo(), this);
            mRequest.accept();
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFinishOnPause) {
            finish();
        }
    }

    private void setupShortcut() {
        ShortcutConfigActivityInfo shortcutInfo =
            new ShortcutConfigActivityInfo(mRequest, this);
        // Maybe add ability to update shortcut title here.
    }
}
