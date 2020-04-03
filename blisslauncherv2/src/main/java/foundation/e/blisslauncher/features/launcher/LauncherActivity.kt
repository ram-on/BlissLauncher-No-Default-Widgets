package foundation.e.blisslauncher.features.launcher

import android.app.ActivityOptions
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.View
import dagger.android.AndroidInjection
import foundation.e.blisslauncher.base.BaseDraggingActivity
import foundation.e.blisslauncher.base.presentation.BaseIntent
import foundation.e.blisslauncher.common.subscribeToState
import foundation.e.blisslauncher.common.util.TraceHelper
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.interactor.LoadLauncher
import foundation.e.blisslauncher.domain.keys.PackageUserKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class LauncherActivity : BaseDraggingActivity(), LauncherView {

    private lateinit var oldConfig: Configuration

    private lateinit var loadLauncher: LoadLauncher

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val loadLauncherIntentPublisher = BehaviorSubject.create<LauncherViewEvent.LoadLauncher>()

    @Inject
    lateinit var launcherViewModel: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }

        TraceHelper.beginSection("Launcher-onCreate")

        super.onCreate(savedInstanceState)
        TraceHelper.partitionSection("Launcher-onCreate", "super call")

        oldConfig = Configuration(resources.configuration)

        compositeDisposable += launcherViewModel.states().subscribeToState { render(it) }
        //TODO set model and state here

        compositeDisposable += intents().subscribe { launcherViewModel::process }
    }

    override fun intents(): Observable<BaseIntent<LauncherState>> {
        return Observable.merge(initialIntent(),
            loadLauncherIntentPublisher.map { launcherViewModel.toIntent(it) }
        )
    }

    private fun initialIntent(): Observable<BaseIntent<LauncherState>> {
        return loadLauncherIntentPublisher.map { launcherViewModel.toIntent(it) }
    }

    override fun getRootView(): View {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun invalidateParent(launcherItem: LauncherItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getActivityLaunchOptions(v: View): ActivityOptions? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        launcherViewModel.terminate()
    }

    companion object {
        const val TAG = "Launcher"
        const val LOGD = false

        const val DEBUG_STRICT_MODE = false

        private const val REQUEST_CREATE_SHORTCUT = 1
        private const val REQUEST_CREATE_APPWIDGET = 5

        private const val REQUEST_PICK_APPWIDGET = 9

        private const val REQUEST_BIND_APPWIDGET = 11

        const val REQUEST_BIND_PENDING_APPWIDGET = 12
        const val REQUEST_RECONFIGURE_APPWIDGET = 13

        // Type: int
        private const val RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen"
        // Type: int
        private const val RUNTIME_STATE = "launcher.state"
        // Type: PendingRequestArgs
        private const val RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args"
        // Type: ActivityResultInfo
        private const val RUNTIME_STATE_PENDING_ACTIVITY_RESULT =
            "launcher.activity_result"
        // Type: SparseArray<Parcelable>
        private const val RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel"
    }

    override fun updateIconBadges(updatedBadges: Set<PackageUserKey>) {
    }

    override fun render(state: LauncherState) {
    }
}