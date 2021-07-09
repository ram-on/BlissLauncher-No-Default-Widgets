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

package foundation.e.blisslauncher.features.quickstep.uioverrides;

import static android.view.View.VISIBLE;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SCALE_PROPERTY;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.CancellationSignal;
import com.android.systemui.shared.system.WindowManagerWrapper;
import foundation.e.blisslauncher.features.quickstep.LauncherAppTransitionManagerImpl;
import foundation.e.blisslauncher.features.quickstep.OverviewInteractionState;
import foundation.e.blisslauncher.features.quickstep.RecentsModel;
import foundation.e.blisslauncher.features.quickstep.util.RemoteFadeOutAnimationListener;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.TouchController;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;

public class UiFactory {

    public static TouchController[] createTouchControllers(TestActivity launcher) {
        boolean swipeUpEnabled = OverviewInteractionState.getInstance(launcher)
            .isSwipeUpGestureEnabled();
        if (!swipeUpEnabled) {
            return new TouchController[]{
                launcher.getDragController(),
                new LauncherTaskViewController(launcher)};
        }

        // TODO: Enable when supporting landscape mode
        /*if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return new TouchController[] {
                    launcher.getDragController(),
                    new OverviewToAllAppsTouchController(launcher),
                    new LandscapeEdgeSwipeController(launcher),
                    new LauncherTaskViewController(launcher)};
        } else {
            return new TouchController[] {
                    launcher.getDragController(),
                    new PortraitStatesTouchController(launcher),
                    new LauncherTaskViewController(launcher)};
        }*/
        return new TouchController[]{
            launcher.getDragController(),
            new PortraitStatesTouchController(launcher),
            new LauncherTaskViewController(launcher)};
    }

    public static void setOnTouchControllersChangedListener(Context context, Runnable listener) {
        OverviewInteractionState.getInstance(context).setOnSwipeUpSettingChangedListener(listener);
    }

    public static LauncherStateManager.StateHandler[] getStateHandler(TestActivity launcher) {
        return new LauncherStateManager.StateHandler[]{launcher.getLauncherPagedView(),
            new RecentsViewStateController(launcher), new BackButtonAlphaHandler(launcher)};
    }

    /**
     * Sets the back button visibility based on the current state/window focus.
     */
    public static void onLauncherStateOrFocusChanged(TestActivity launcher) {
        boolean shouldBackButtonBeHidden = launcher != null
            && launcher.getStateManager().getState().hideBackButton
            && launcher.hasWindowFocus();
        OverviewInteractionState.getInstance(launcher)
            .setBackButtonAlpha(shouldBackButtonBeHidden ? 0 : 1, true /* animate */);
    }

    public static void resetOverview(TestActivity launcher) {
        RecentsView recents = launcher.getOverviewPanel();
        recents.reset();
    }

    public static void onCreate(TestActivity launcher) {
        // TODO: May consider adding bounce animation here.
    }

    public static void onStart(Context context) {
        RecentsModel model = RecentsModel.getInstance(context);
        if (model != null) {
            model.onStart();
        }
    }

    public static void onLauncherStateOrResumeChanged(TestActivity launcher) {
        LauncherState state = launcher.getStateManager().getState();
        VariantDeviceProfile profile = launcher.getDeviceProfile();
        WindowManagerWrapper.getInstance().setShelfHeight(
            launcher.isUserActive(),
            profile.getHotseatBarSizePx()
        );

        if (state == NORMAL) {
            launcher.<RecentsView>getOverviewPanel().setSwipeDownShouldLaunchApp(false);
        }
    }

    public static void onTrimMemory(Context context, int level) {
        RecentsModel model = RecentsModel.getInstance(context);
        if (model != null) {
            model.onTrimMemory(level);
        }
    }

    public static void useFadeOutAnimationForLauncherStart(
        TestActivity launcher,
        CancellationSignal cancellationSignal
    ) {
        LauncherAppTransitionManagerImpl appTransitionManager =
            (LauncherAppTransitionManagerImpl) launcher.getAppTransitionManager();
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

    public static void prepareToShowOverview(TestActivity launcher) {
        RecentsView overview = launcher.getOverviewPanel();
        if (overview.getVisibility() != VISIBLE || overview.getContentAlpha() == 0) {
            SCALE_PROPERTY.set(overview, 1.33f);
        }
    }

    private static class LauncherTaskViewController extends TaskViewTouchController<TestActivity> {

        public LauncherTaskViewController(TestActivity activity) {
            super(activity);
        }

        @Override
        protected boolean isRecentsInteractive() {
            return mActivity.isInState(OVERVIEW);
        }

        @Override
        protected void onUserControlledAnimationCreated(AnimatorPlaybackController animController) {
            mActivity.getStateManager().setCurrentUserControlledAnimation(animController);
        }
    }
}
