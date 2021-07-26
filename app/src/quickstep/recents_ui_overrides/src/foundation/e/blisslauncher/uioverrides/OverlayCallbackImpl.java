package foundation.e.blisslauncher.uioverrides;

import android.util.Log;

import org.jetbrains.annotations.Nullable;

import foundation.e.blisslauncher.features.test.TestActivity;

public class OverlayCallbackImpl implements TestActivity.LauncherOverlay {

    private final TestActivity mLauncher;
    private static final String TAG = "OverlayCallbackImpl";
    private float mProgress = 0;

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
        Log.d(TAG, "onScrollInteractionEnd() called");
        if(mProgress > 0.5f) mLauncherOverlayCallbacks.onScrollChanged(1f);
        else mLauncherOverlayCallbacks.onScrollChanged(0f);
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        Log.d(
            TAG,
            "onScrollChange() called with: progress = [" + progress + "], rtl = [" + rtl + "]"
        );
        if(mLauncherOverlayCallbacks != null) {
            mLauncherOverlayCallbacks.onScrollChanged(progress);
            mProgress = progress;
        }
    }

    @Override
    public void setOverlayCallbacks(@Nullable TestActivity.LauncherOverlayCallbacks callbacks) {
        mLauncherOverlayCallbacks = callbacks;
    }
}
