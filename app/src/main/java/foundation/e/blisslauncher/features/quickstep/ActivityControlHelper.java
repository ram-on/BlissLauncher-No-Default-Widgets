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

import static com.android.systemui.shared.system.NavigationBarCompat.HIT_TARGET_BACK;
import static com.android.systemui.shared.system.NavigationBarCompat.HIT_TARGET_ROTATION;
import static foundation.e.blisslauncher.features.quickstep.TouchConsumer.INTERACTION_NORMAL;
import static foundation.e.blisslauncher.features.quickstep.TouchConsumer.INTERACTION_QUICK_SCRUB;
import static foundation.e.blisslauncher.features.test.LauncherState.FAST_OVERVIEW;
import static foundation.e.blisslauncher.features.test.LauncherState.OVERVIEW;
import static foundation.e.blisslauncher.features.test.anim.Interpolators.LINEAR;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.utils.MultiValueAlpha;
import foundation.e.blisslauncher.features.quickstep.uioverrides.FastOverviewState;
import foundation.e.blisslauncher.features.quickstep.util.LayoutUtils;
import foundation.e.blisslauncher.features.quickstep.util.RemoteAnimationProvider;
import foundation.e.blisslauncher.features.quickstep.util.RemoteAnimationTargetSet;
import foundation.e.blisslauncher.features.quickstep.util.TransformedRect;
import foundation.e.blisslauncher.features.quickstep.views.LauncherLayoutListener;
import foundation.e.blisslauncher.features.quickstep.views.RecentsView;
import foundation.e.blisslauncher.features.quickstep.views.RecentsViewContainer;
import foundation.e.blisslauncher.features.test.BaseDraggingActivity;
import foundation.e.blisslauncher.features.test.LauncherAppState;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.anim.AnimatorPlaybackController;
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Utility class which abstracts out the logical differences between Launcher and RecentsActivity.
 */
@TargetApi(Build.VERSION_CODES.P)
public interface ActivityControlHelper<T extends BaseDraggingActivity> {

    LayoutListener createLayoutListener(T activity);

    /**
     * Updates the UI to indicate quick interaction.
     */
    void onQuickInteractionStart(T activity, @Nullable RunningTaskInfo taskInfo,
            boolean activityVisible);

    float getTranslationYForQuickScrub(
        TransformedRect targetRect, VariantDeviceProfile dp,
            Context context);

    void executeOnWindowAvailable(T activity, Runnable action);

    void onTransitionCancelled(T activity, boolean activityVisible);

    int getSwipeUpDestinationAndLength(VariantDeviceProfile dp, Context context,
            @TouchConsumer.InteractionType int interactionType, TransformedRect outRect);

    void onSwipeUpComplete(T activity);

    AnimationFactory prepareRecentsUI(T activity, boolean activityVisible,
            Consumer<AnimatorPlaybackController> callback);

    ActivityInitListener createActivityInitListener(BiPredicate<T, Boolean> onInitListener);

    @Nullable
    T getCreatedActivity();

    @UiThread
    @Nullable
    RecentsView getVisibleRecentsView();

    @UiThread
    boolean switchToRecentsIfVisible(boolean fromRecentsButton);

    Rect getOverviewWindowBounds(Rect homeBounds, RemoteAnimationTargetCompat target);

    boolean shouldMinimizeSplitScreen();

    /**
     * @return {@code true} if recents activity should be started immediately on touchDown,
     *         {@code false} if it should deferred until some threshold is crossed.
     */
    boolean deferStartingActivity(int downHitTarget);

    boolean supportsLongSwipe(T activity);

    MultiValueAlpha.AlphaProperty getAlphaProperty(T activity);

    class LauncherActivityControllerHelper implements ActivityControlHelper<TestActivity> {

        @Override
        public LayoutListener createLayoutListener(TestActivity activity) {
            return new LauncherLayoutListener(activity);
        }

        @Override
        public void onQuickInteractionStart(TestActivity activity, RunningTaskInfo taskInfo,
                boolean activityVisible) {
            LauncherState fromState = activity.getStateManager().getState();
            activity.getStateManager().goToState(FAST_OVERVIEW, activityVisible);

            QuickScrubController controller = activity.<RecentsView>getOverviewPanel()
                    .getQuickScrubController();
            controller.onQuickScrubStart(activityVisible && !fromState.overviewUi, this);
        }

        @Override
        public float getTranslationYForQuickScrub(TransformedRect targetRect, VariantDeviceProfile dp,
                Context context) {
            // The padding calculations are exactly same as that of RecentsView.setInsets
            int topMargin = context.getResources()
                    .getDimensionPixelSize(R.dimen.task_thumbnail_top_margin);
            int paddingTop = targetRect.rect.top - topMargin - dp.getInsets().top;
            int paddingBottom = dp.getAvailableHeightPx() + dp.getInsets().top - targetRect.rect.bottom;

            return FastOverviewState.OVERVIEW_TRANSLATION_FACTOR * (paddingBottom - paddingTop);
        }

        @Override
        public void executeOnWindowAvailable(TestActivity activity, Runnable action) {
            //TODO: Fix if needed
            //activity.getLauncherPagedView().runOnOverlayHidden(action);
        }

        @Override
        public int getSwipeUpDestinationAndLength(VariantDeviceProfile dp, Context context,
                @TouchConsumer.InteractionType int interactionType, TransformedRect outRect) {
            LayoutUtils.calculateLauncherTaskSize(context, dp, outRect.rect);
            if (interactionType == INTERACTION_QUICK_SCRUB) {
                outRect.scale = FastOverviewState.getOverviewScale(dp, outRect.rect, context);
            }
            /*if (dp.isVerticalBarLayout()) {
                Rect targetInsets = dp.getInsets();
                int hotseatInset = dp.isSeascape() ? targetInsets.left : targetInsets.right;
                return dp.hotseatBarSizePx + dp.hotseatBarSidePaddingPx + hotseatInset;
            } else {

            }*/
            return dp.getHeightPx() - outRect.rect.bottom;
        }

        @Override
        public void onTransitionCancelled(TestActivity activity, boolean activityVisible) {
            LauncherState startState = activity.getStateManager().getRestState();
            activity.getStateManager().goToState(startState, activityVisible);
        }

        @Override
        public void onSwipeUpComplete(TestActivity activity) {
            // Re apply state in case we did something funky during the transition.
            activity.getStateManager().reapplyState();
        }

        @Override
        public AnimationFactory prepareRecentsUI(TestActivity activity, boolean activityVisible,
                Consumer<AnimatorPlaybackController> callback) {
            final LauncherState startState = activity.getStateManager().getState();

            LauncherState resetState = startState;
            if (startState.disableRestore) {
                resetState = activity.getStateManager().getRestState();
            }
            activity.getStateManager().setRestState(resetState);

            if (!activityVisible) {
                // Since the launcher is not visible, we can safely reset the scroll position.
                // This ensures then the next swipe up to all-apps starts from scroll 0.
                activity.getStateManager().goToState(OVERVIEW, false);
            }

            return new AnimationFactory() {
                @Override
                public void createActivityController(long transitionLength,
                        @TouchConsumer.InteractionType int interactionType) {
                    createActivityControllerInternal(activity, activityVisible, startState,
                            transitionLength, interactionType, callback);
                }

                @Override
                public void onTransitionCancelled() {
                    activity.getStateManager().goToState(startState, false /* animate */);
                }
            };
        }

        private void createActivityControllerInternal(TestActivity activity, boolean wasVisible,
                LauncherState startState, long transitionLength,
                @TouchConsumer.InteractionType int interactionType,
                Consumer<AnimatorPlaybackController> callback) {
            LauncherState endState = interactionType == INTERACTION_QUICK_SCRUB
                    ? FAST_OVERVIEW : OVERVIEW;
            if (wasVisible) {
                VariantDeviceProfile dp = activity.getDeviceProfile();
                long accuracy = 2 * Math.max(dp.getWidthPx(), dp.getHeightPx());
                activity.getStateManager().goToState(startState, false);
                callback.accept(activity.getStateManager()
                        .createAnimationToNewWorkspace(endState, accuracy));
                return;
            }
        }

        @Override
        public ActivityInitListener createActivityInitListener(
                BiPredicate<TestActivity, Boolean> onInitListener) {
            return new LauncherInitListener(onInitListener);
        }

        @Nullable
        @Override
        public TestActivity getCreatedActivity() {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app == null) {
                return null;
            }
            return (TestActivity) app.getLauncher();
        }

        @Nullable
        @UiThread
        private TestActivity getVisibleLaucher() {
            TestActivity launcher = getCreatedActivity();
            return (launcher != null) && launcher.isStarted() && launcher.hasWindowFocus() ?
                    launcher : null;
        }

        @Nullable
        @Override
        public RecentsView getVisibleRecentsView() {
            TestActivity launcher = getVisibleLaucher();
            return launcher != null && launcher.getStateManager().getState().overviewUi
                    ? launcher.getOverviewPanel() : null;
        }

        @Override
        public boolean switchToRecentsIfVisible(boolean fromRecentsButton) {
            TestActivity launcher = getVisibleLaucher();
            if (launcher != null) {
                launcher.getStateManager().goToState(OVERVIEW);
                return true;
            }
            return false;
        }

        @Override
        public boolean deferStartingActivity(int downHitTarget) {
            return downHitTarget == HIT_TARGET_BACK || downHitTarget == HIT_TARGET_ROTATION;
        }

        @Override
        public Rect getOverviewWindowBounds(Rect homeBounds, RemoteAnimationTargetCompat target) {
            return homeBounds;
        }

        @Override
        public boolean shouldMinimizeSplitScreen() {
            return true;
        }

        @Override
        public boolean supportsLongSwipe(TestActivity activity) {
            return false;
        }

        @Override
        public MultiValueAlpha.AlphaProperty getAlphaProperty(TestActivity activity) {
            return activity.getDragLayer().getAlphaProperty(DragLayer.ALPHA_INDEX_SWIPE_UP);
        }

    }

    class FallbackActivityControllerHelper implements ActivityControlHelper<RecentsActivity> {

        private final ComponentName mHomeComponent;
        private final Handler mUiHandler = new Handler(Looper.getMainLooper());

        public FallbackActivityControllerHelper(ComponentName homeComponent) {
            mHomeComponent = homeComponent;
        }

        @Override
        public void onQuickInteractionStart(RecentsActivity activity, RunningTaskInfo taskInfo,
                boolean activityVisible) {
            QuickScrubController controller = activity.<RecentsView>getOverviewPanel()
                    .getQuickScrubController();

            // TODO: match user is as well
            boolean startingFromHome = !activityVisible &&
                    (taskInfo == null || Objects.equals(taskInfo.topActivity, mHomeComponent));
            controller.onQuickScrubStart(startingFromHome, this);
            if (activityVisible) {
                mUiHandler.postDelayed(controller::onFinishedTransitionToQuickScrub,
                        OVERVIEW_TRANSITION_MS);
            }
        }

        @Override
        public float getTranslationYForQuickScrub(TransformedRect targetRect, VariantDeviceProfile dp,
                Context context) {
            return 0;
        }

        @Override
        public void executeOnWindowAvailable(RecentsActivity activity, Runnable action) {
            action.run();
        }

        @Override
        public void onTransitionCancelled(RecentsActivity activity, boolean activityVisible) {
            // TODO:
        }

        @Override
        public int getSwipeUpDestinationAndLength(VariantDeviceProfile dp, Context context,
                @TouchConsumer.InteractionType int interactionType, TransformedRect outRect) {
            LayoutUtils.calculateFallbackTaskSize(context, dp, outRect.rect);

            // TODO: Fix this when supporting landscape
            /*if (dp.isVerticalBarLayout()) {
                Rect targetInsets = dp.getInsets();
                int hotseatInset = dp.isSeascape() ? targetInsets.left : targetInsets.right;
                return dp.hotseatBarSizePx + dp.hotseatBarSidePaddingPx + hotseatInset;
            } else {
                return dp.heightPx - outRect.rect.bottom;
            }*/
            return dp.getHeightPx() - outRect.rect.bottom;
        }

        @Override
        public void onSwipeUpComplete(RecentsActivity activity) {
            // TODO:
        }

        @Override
        public AnimationFactory prepareRecentsUI(RecentsActivity activity, boolean activityVisible,
                Consumer<AnimatorPlaybackController> callback) {
            if (activityVisible) {
                return (transitionLength, interactionType) -> { };
            }

            RecentsViewContainer rv = activity.getOverviewPanelContainer();
            rv.setContentAlpha(0);

            return new AnimationFactory() {

                boolean isAnimatingHome = false;

                @Override
                public void onRemoteAnimationReceived(RemoteAnimationTargetSet targets) {
                    isAnimatingHome = targets != null && targets.isAnimatingHome();
                    if (!isAnimatingHome) {
                        rv.setContentAlpha(1);
                    }
                    createActivityController(getSwipeUpDestinationAndLength(
                            activity.getDeviceProfile(), activity, INTERACTION_NORMAL,
                            new TransformedRect()), INTERACTION_NORMAL);
                }

                @Override
                public void createActivityController(long transitionLength, int interactionType) {
                    if (!isAnimatingHome) {
                        return;
                    }

                    ObjectAnimator anim = ObjectAnimator
                            .ofFloat(rv, RecentsViewContainer.CONTENT_ALPHA, 0, 1);
                    anim.setDuration(transitionLength).setInterpolator(LINEAR);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.play(anim);
                    callback.accept(AnimatorPlaybackController.wrap(animatorSet, transitionLength));
                }
            };
        }

        @Override
        public LayoutListener createLayoutListener(RecentsActivity activity) {
            // We do not change anything as part of layout changes in fallback activity. Return a
            // default layout listener.
            return new LayoutListener() {
                @Override
                public void open() { }

                @Override
                public void setHandler(WindowTransformSwipeHandler handler) { }

                @Override
                public void finish() { }
            };
        }

        @Override
        public ActivityInitListener createActivityInitListener(
                BiPredicate<RecentsActivity, Boolean> onInitListener) {
            return new RecentsActivityTracker(onInitListener);
        }

        @Nullable
        @Override
        public RecentsActivity getCreatedActivity() {
            return RecentsActivityTracker.getCurrentActivity();
        }

        @Nullable
        @Override
        public RecentsView getVisibleRecentsView() {
            RecentsActivity activity = getCreatedActivity();
            if (activity != null && activity.hasWindowFocus()) {
                return activity.getOverviewPanel();
            }
            return null;
        }

        @Override
        public boolean switchToRecentsIfVisible(boolean fromRecentsButton) {
            return false;
        }

        @Override
        public boolean deferStartingActivity(int downHitTarget) {
            // Always defer starting the activity when using fallback
            return true;
        }

        @Override
        public Rect getOverviewWindowBounds(Rect homeBounds, RemoteAnimationTargetCompat target) {
            // TODO: Remove this once b/77875376 is fixed
            return target.sourceContainerBounds;
        }

        @Override
        public boolean shouldMinimizeSplitScreen() {
            // TODO: Remove this once b/77875376 is fixed
            return false;
        }

        @Override
        public boolean supportsLongSwipe(RecentsActivity activity) {
            return false;
        }


        @Override
        public MultiValueAlpha.AlphaProperty getAlphaProperty(RecentsActivity activity) {
            return activity.getDragLayer().getAlphaProperty(0);
        }
    }

    interface LayoutListener {

        void open();

        void setHandler(WindowTransformSwipeHandler handler);

        void finish();
    }

    interface ActivityInitListener {

        void register();

        void unregister();

        void registerAndStartActivity(Intent intent, RemoteAnimationProvider animProvider,
                Context context, Handler handler, long duration);
    }

    interface AnimationFactory {

        default void onRemoteAnimationReceived(RemoteAnimationTargetSet targets) { }

        void createActivityController(long transitionLength, @TouchConsumer.InteractionType int interactionType);

        default void onTransitionCancelled() { }
    }
}
