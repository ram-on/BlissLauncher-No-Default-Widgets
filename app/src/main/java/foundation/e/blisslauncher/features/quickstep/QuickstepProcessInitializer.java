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
package foundation.e.blisslauncher.features.quickstep;

import android.content.Context;

import com.android.systemui.shared.system.ThreadedRendererCompat;

@SuppressWarnings("unused")
public class QuickstepProcessInitializer {

    public QuickstepProcessInitializer(Context context) { }

    protected void init(Context context) {
        // Elevate GPU priority for Quickstep and Remote animations.
        ThreadedRendererCompat.setContextPriority(ThreadedRendererCompat.EGL_CONTEXT_PRIORITY_HIGH_IMG);
    }
}
