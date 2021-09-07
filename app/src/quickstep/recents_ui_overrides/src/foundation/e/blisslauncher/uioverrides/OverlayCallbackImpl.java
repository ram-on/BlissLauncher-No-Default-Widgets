package foundation.e.blisslauncher.uioverrides;

import android.util.Log;

import org.jetbrains.annotations.Nullable;

import foundation.e.blisslauncher.features.test.TestActivity;

public class OverlayCallbackImpl implements TestActivity.LauncherOverlay {

    private final TestActivity mLauncher;
    private static final String TAG = "OverlayCallbackImpl";
    private float mProgress = 0;
    private boolean scrollFromWorkspace = false;

    private TestActivity.LauncherOverlayCallbacks mLauncherOverlayCallbacks;

    public OverlayCallbackImpl(TestActivity launcher) {
        this.mLauncher = launcher;
    }

    @Override
    public void onScrollInteractionBegin() {
        Log.d(TAG, "onScrollInteractionBegin() called");
        mLauncherOverlayCallbacks.onScrollBegin();
    }

    @Override
    public void onScrollInteractionEnd() {
        Log.d(TAG, "onScrollInteractionEnd() called "+mProgress);
        if(scrollFromWorkspace) {
            if(mProgress >= 0.5f) mLauncherOverlayCallbacks.onScrollEnd(1f, true);
            else mLauncherOverlayCallbacks.onScrollEnd(0f, true);
        } else {
            if(mProgress < 0.5f) mLauncherOverlayCallbacks.onScrollEnd(0f, false);
            else mLauncherOverlayCallbacks.onScrollEnd(1f, false);
        }
    }

    @Override
    public void onScrollChange(float progress, boolean scrollFromWorkspace, boolean rtl) {
        if(mLauncherOverlayCallbacks != null) {
            mLauncherOverlayCallbacks.onScrollChanged(progress, scrollFromWorkspace);
            mProgress = progress;
            this.scrollFromWorkspace = scrollFromWorkspace;
        }
    }

    @Override
    public void setOverlayCallbacks(@Nullable TestActivity.LauncherOverlayCallbacks callbacks) {
        mLauncherOverlayCallbacks = callbacks;
    }
}
