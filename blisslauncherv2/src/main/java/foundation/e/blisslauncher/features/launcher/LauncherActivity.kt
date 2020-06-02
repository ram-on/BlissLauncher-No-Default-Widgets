package foundation.e.blisslauncher.features.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import dagger.android.AndroidInjection
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.common.DeviceProfile
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.util.TraceHelper
import foundation.e.blisslauncher.domain.dto.WorkspaceModel
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.features.LauncherStore
import foundation.e.blisslauncher.features.base.BaseDraggingActivity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_launcher.*
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class LauncherActivity : BaseDraggingActivity(), LauncherView {

    private lateinit var oldConfig: Configuration

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val intentSubject = PublishSubject.create<LauncherStore.LauncherIntent>()

    override val events: Observable<LauncherStore.LauncherIntent>
        get() = intentSubject

    @Inject
    lateinit var launcherStore: LauncherStore

    @Inject
    lateinit var idp: InvariantDeviceProfile

    lateinit var deviceProfile: DeviceProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        deviceProfile = idp.getDeviceProfile(this)
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

        setContentView(R.layout.activity_launcher)

        TraceHelper.beginSection("Launcher-onCreate")

        super.onCreate(savedInstanceState)
        TraceHelper.partitionSection("Launcher-onCreate", "super call")

        oldConfig = Configuration(resources.configuration)

        compositeDisposable += Observable.wrap(launcherStore).subscribe { render(it) }
        //TODO set model and state here
        compositeDisposable += events.subscribe(launcherStore)

        intentSubject.onNext(LauncherStore.LauncherIntent.InitialIntent)
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

        fun getLauncher(context: Context): LauncherActivity {
            return if (context is LauncherActivity) {
                context
            } else (context as ContextWrapper).baseContext as LauncherActivity
        }
    }

    override fun render(state: LauncherState) {
        Timber.d("Current state is $state")
        if (state is LauncherState.Loaded) {
            val model = state.workspaceModel
            bindScreens(model)
            bindWorkspaceItems(model.workspaceItems)
        }
    }

    private fun bindScreens(model: WorkspaceModel) {
        workspace.addExtraEmptyScreen()
        model.workspaceScreens.forEach {
            workspace.insertNewWorkspaceScreen(it)
        }

        Timber.d("Total child in workspace is: ${workspace.childCount}")
    }

    private fun bindWorkspaceItems(workspaceItems: ArrayList<LauncherItem>) {
        Timber.d("Total workspace items: ${workspaceItems.size}")
        workspaceItems.forEach {
            if (it is LauncherItemWithIcon) {
                val view = TextView(this)
                view.setText(it.title)
                workspace.addInScreenFromBind(view, it)
            }
        }
    }
}