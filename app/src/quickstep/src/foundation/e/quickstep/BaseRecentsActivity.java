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
package foundation.e.quickstep;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import foundation.e.blisslauncher.uioverrides.UiFactory;
import java.io.FileDescriptor;
import java.io.PrintWriter;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.features.quickstep.AbstractFloatingView;
import foundation.e.blisslauncher.features.test.BaseDraggingActivity;
import foundation.e.blisslauncher.features.test.SystemUiController;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

/**
 * A base fallback recents activity that provides support for device profile changes, activity
 * lifecycle tracking, and basic input handling from recents.
 *
 * This class is only used as a fallback in case the default launcher does not have a recents
 * implementation.
 */
public abstract class BaseRecentsActivity extends BaseDraggingActivity {

    private Configuration mOldConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOldConfig = new Configuration(getResources().getConfiguration());
        initDeviceProfile();
        initViews();

        getSystemUiController().updateUiState(
            SystemUiController.UI_STATE_BASE_WINDOW,
                false);
        RecentsActivityTracker.onRecentsActivityCreate(this);
    }

    /**
     * Init drag layer and overview panel views.
     */
    abstract protected void initViews();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            onHandleConfigChanged();
        }
        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Logic for when device configuration changes (rotation, screen size change, multi-window,
     * etc.)
     */
    protected void onHandleConfigChanged() {
        initDeviceProfile();

        AbstractFloatingView.closeOpenViews(this, true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);
        dispatchDeviceProfileChanged();

        reapplyUi();
    }

    /**
     * Initialize/update the device profile.
     */
    private void initDeviceProfile() {
        mDeviceProfile = createDeviceProfile();
        onDeviceProfileInitiated();
    }

    /**
     * Generate the device profile to use in this activity.
     * @return device profile
     */
    protected VariantDeviceProfile createDeviceProfile() {
        VariantDeviceProfile dp = BlissLauncher.getApplication(this).getInvariantDeviceProfile().getDeviceProfile(this);

        // In case we are reusing IDP, create a copy so that we don't conflict with Launcher
        // activity.
        return dp.copy(this);
    }


    @Override
    protected void onStop() {
        super.onStop();

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        UiFactory.onEnterAnimationComplete(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        UiFactory.onTrimMemory(this, level);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RecentsActivityTracker.onRecentsActivityNewIntent(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecentsActivityTracker.onRecentsActivityDestroy(this);
    }

    @Override
    public void onBackPressed() {
        // TODO: Launch the task we came from
        startHome();
    }

    public void startHome() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(prefix + "Misc:");
    }
}
