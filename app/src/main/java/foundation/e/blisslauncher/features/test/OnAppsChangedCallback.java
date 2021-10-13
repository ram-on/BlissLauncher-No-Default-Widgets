/*
 * Copyright (C) 2014 The Android Open Source Project
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

package foundation.e.blisslauncher.features.test;

import android.content.pm.ShortcutInfo;
import android.os.UserHandle;

import java.util.List;

public interface OnAppsChangedCallback {
    void onPackageRemoved(String packageName, UserHandle user);
    void onPackageAdded(String packageName, UserHandle user);
    void onPackageChanged(String packageName, UserHandle user);
    void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing);
    void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing);
    void onPackagesSuspended(String[] packageNames, UserHandle user);
    void onPackagesUnsuspended(String[] packageNames, UserHandle user);
    void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts,
        UserHandle user);
}
