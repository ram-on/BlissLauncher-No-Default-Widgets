package foundation.e.blisslauncher.core.executors;


import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class MainThreadExecutor extends LooperExecutor {

    public MainThreadExecutor() {
        super(Looper.getMainLooper());
    }

}
