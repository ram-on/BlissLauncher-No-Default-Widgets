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

package foundation.e.blisslauncher.uioverrides;

import static android.app.Activity.RESULT_CANCELED;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Base64;
import com.android.systemui.shared.system.ActivityCompat;
import foundation.e.blisslauncher.QuickstepAppTransitionManagerImpl;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.proxy.ProxyActivityStarter;
import foundation.e.blisslauncher.proxy.StartActivityParams;
import foundation.e.blisslauncher.quickstep.OverviewInteractionState;
import foundation.e.blisslauncher.quickstep.RecentsModel;
import foundation.e.blisslauncher.quickstep.SysUINavigationMode;
import foundation.e.blisslauncher.quickstep.util.RemoteFadeOutAnimationListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.zip.Deflater;

public class UiFactory extends RecentsUiFactory {

    public static Runnable enableLiveUIChanges(TestActivity launcher) {
        SysUINavigationMode.NavigationModeChangeListener listener = m -> {
            launcher.getDragLayer().recreateControllers();
            launcher.getRotationHelper().setRotationHadDifferentUI(m != SysUINavigationMode.Mode.NO_BUTTON);
        };
        SysUINavigationMode mode = SysUINavigationMode.INSTANCE.get(launcher);
        SysUINavigationMode.Mode m = mode.addModeChangeListener(listener);
        launcher.getRotationHelper().setRotationHadDifferentUI(m != SysUINavigationMode.Mode.NO_BUTTON);
        return () -> mode.removeModeChangeListener(listener);
    }

    public static LauncherStateManager.StateHandler[] getStateHandler(TestActivity launcher) {
        return new LauncherStateManager.StateHandler[] {
                launcher.getLauncherPagedView(),
                createRecentsViewStateController(launcher),
                new BackButtonAlphaHandler(launcher)};
    }

    /**
     * Sets the back button visibility based on the current state/window focus.
     */
    public static void onLauncherStateOrFocusChanged(TestActivity launcher) {
        boolean shouldBackButtonBeHidden = launcher != null
                && launcher.getStateManager().getState().hideBackButton
                && launcher.hasWindowFocus();
        if (shouldBackButtonBeHidden) {
            // Show the back button if there is a floating view visible.
            shouldBackButtonBeHidden = true;
        }
        OverviewInteractionState.INSTANCE.get(launcher)
                .setBackButtonAlpha(shouldBackButtonBeHidden ? 0 : 1, true /* animate */);
        if (launcher != null && launcher.getDragLayer() != null) {
            launcher.getRootView().setDisallowBackGesture(shouldBackButtonBeHidden);
        }
    }

    public static void onCreate(TestActivity launcher) {

        /*if (!launcher.getSharedPrefs().getBoolean(SHELF_BOUNCE_SEEN, false)) {
            launcher.getStateManager().addStateListener(new LauncherStateManager.StateListener() {
                @Override
                public void onStateTransitionStart(LauncherState toState) {
                }

                @Override
                public void onStateTransitionComplete(LauncherState finalState) {
                    LauncherState prevState = launcher.getStateManager().getLastState();

                    if ((finalState == ALL_APPS && prevState == OVERVIEW) || BOUNCE_MAX_COUNT <=
                            launcher.getSharedPrefs().getInt(SHELF_BOUNCE_COUNT, 0)) {
                        launcher.getSharedPrefs().edit().putBoolean(SHELF_BOUNCE_SEEN, true).apply();
                        launcher.getStateManager().removeStateListener(this);
                    }
                }
            });
        }*/
    }

    public static void onEnterAnimationComplete(Context context) {
        // After the transition to home, enable the high-res thumbnail loader if it wasn't enabled
        // as a part of quickstep, so that high-res thumbnails can load the next time we enter
        // overview
        RecentsModel.INSTANCE.get(context).getThumbnailCache()
                .getHighResLoadingState().setVisible(true);
    }

    public static void onTrimMemory(Context context, int level) {
        RecentsModel model = RecentsModel.INSTANCE.get(context);
        if (model != null) {
            model.onTrimMemory(level);
        }
    }

    public static void useFadeOutAnimationForLauncherStart(TestActivity launcher,
            CancellationSignal cancellationSignal) {
        QuickstepAppTransitionManagerImpl appTransitionManager =
                (QuickstepAppTransitionManagerImpl) launcher.getAppTransitionManager();
        appTransitionManager.setRemoteAnimationProvider((targets) -> {

            // On the first call clear the reference.
            cancellationSignal.cancel();

            ValueAnimator fadeAnimation = ValueAnimator.ofFloat(1, 0);
            fadeAnimation.addUpdateListener(new RemoteFadeOutAnimationListener(targets));
            AnimatorSet anim = new AnimatorSet();
            anim.play(fadeAnimation);
            return anim;
        }, cancellationSignal);
    }

    public static boolean dumpActivity(Activity activity, PrintWriter writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!(new ActivityCompat(activity).encodeViewHierarchy(out))) {
            return false;
        }

        Deflater deflater = new Deflater();
        deflater.setInput(out.toByteArray());
        deflater.finish();

        out.reset();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            out.write(buffer, 0, count);
        }

        writer.println("--encoded-view-dump-v0--");
        writer.println(Base64.encodeToString(
                out.toByteArray(), Base64.NO_WRAP | Base64.NO_PADDING));
        return true;
    }

    public static boolean startIntentSenderForResult(Activity activity, IntentSender intent,
            int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            Bundle options) {
        StartActivityParams params = new StartActivityParams(activity, requestCode);
        params.intentSender = intent;
        params.fillInIntent = fillInIntent;
        params.flagsMask = flagsMask;
        params.flagsValues = flagsValues;
        params.extraFlags = extraFlags;
        params.options = options;
        ((Context) activity).startActivity(ProxyActivityStarter.getLaunchIntent(activity, params));
        return true;
    }

    public static boolean startActivityForResult(Activity activity, Intent intent, int requestCode,
            Bundle options) {
        StartActivityParams params = new StartActivityParams(activity, requestCode);
        params.intent = intent;
        params.options = options;
        activity.startActivity(ProxyActivityStarter.getLaunchIntent(activity, params));
        return true;
    }

    /**
     * Removes any active ProxyActivityStarter task and sends RESULT_CANCELED to Launcher.
     *
     * ProxyActivityStarter is started with clear task to reset the task after which it removes the
     * task itself.
     */
    public static void resetPendingActivityResults(TestActivity launcher, int requestCode) {
        launcher.onActivityResult(requestCode, RESULT_CANCELED, null);
        launcher.startActivity(ProxyActivityStarter.getLaunchIntent(launcher, null));
    }

    public static LauncherState.ScaleAndTranslation getOverviewScaleAndTranslationForNormalState(TestActivity l) {
        if (SysUINavigationMode.getMode(l) == SysUINavigationMode.Mode.NO_BUTTON) {
            float offscreenTranslationX = l.getDeviceProfile().getWidthPx()
                    - l.getOverviewPanel().getPaddingStart();
            return new LauncherState.ScaleAndTranslation(1f, offscreenTranslationX, 0f);
        }
        return new LauncherState.ScaleAndTranslation(1.1f, 0f, 0f);
    }
}
