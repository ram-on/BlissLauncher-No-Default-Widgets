package foundation.e.blisslauncher.core.executors;

import android.os.Looper;

public class MainThreadExecutor extends LooperExecutor {

    public MainThreadExecutor() {
        super(Looper.getMainLooper());
    }
}
