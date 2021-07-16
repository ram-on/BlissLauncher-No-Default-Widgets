package foundation.e.blisslauncher.features.test

import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.LayoutInflater
import android.view.View
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.customviews.LauncherPagedView
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.launcher.Hotseat
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.uioverrides.UiFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import java.util.ArrayList

class TestActivity : BaseDraggingActivity() {

    private var mCancelTouchController: Runnable? = null
    private var mOnResumeCallback: OnResumeCallback? = null
    private lateinit var mAppTransitionManager: LauncherAppTransitionManager
    lateinit var dragController: DragController
    private lateinit var mOldConfig: Configuration
    private var mCompositeDisposable: CompositeDisposable? = null

    private lateinit var launcherView: View
    private lateinit var dragLayer: DragLayer
    private lateinit var workspace: LauncherPagedView
    private lateinit var hotseat: Hotseat

    private lateinit var rotationHelper: RotationHelper
    private lateinit var mStateManager: LauncherStateManager

    // UI and state for the overview panel
    private lateinit var overviewPanel: View

    private val mOnResumeCallbacks = ArrayList<OnResumeCallback>()

    private val TAG = "TestActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
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

        mOldConfig = Configuration(resources.configuration)
        initDeviceProfile(BlissLauncher.getApplication(this).invariantDeviceProfile)
        dragController = DragController(this)
        rotationHelper = RotationHelper(this)
        mStateManager = LauncherStateManager(this)
        launcherView = LayoutInflater.from(this).inflate(R.layout.activity_test, null)
        setupViews()
        mAppTransitionManager = LauncherAppTransitionManager.newInstance(this)

        setContentView(launcherView)
        createOrUpdateIconGrid()
    }

    private fun setupViews() {
        dragLayer = findViewById(R.id.drag_layer)
        workspace = dragLayer.findViewById(R.id.workspace)
        workspace.initParentViews(dragLayer)
        overviewPanel = findViewById(R.id.overview_panel)
        hotseat = findViewById(R.id.hotseat)
        launcherView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        dragLayer.setup(dragController, workspace)
        mCancelTouchController = UiFactory.enableLiveUIChanges(this)
        workspace.setup(dragController)
        workspace.bindAndInitFirstScreen(null)
        dragController.addDragListener(workspace)
        dragController.addDropTarget(workspace)
    }

    override fun <T : View> findViewById(id: Int): T {
        return launcherView.findViewById(id)
    }

    fun getRotationHelper() = rotationHelper

    private fun initDeviceProfile(idp: InvariantDeviceProfile) {
        // Load device specific profile
        mDeviceProfile = idp.getDeviceProfile(this)
        onDeviceProfileInitiated()
    }

    override fun onResume() {
        super.onResume()
        if (mOnResumeCallbacks.isNotEmpty()) {
            val resumeCallbacks = ArrayList(mOnResumeCallbacks)
            mOnResumeCallbacks.clear()
            for (i in resumeCallbacks.indices.reversed()) {
                resumeCallbacks[i].onLauncherResume()
            }
            resumeCallbacks.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCancelTouchController != null) {
            mCancelTouchController!!.run()
            mCancelTouchController = null
        }
    }

    fun getHotseat() = hotseat

    fun isWorkspaceLoading() = false

    override fun getDragLayer(): DragLayer = dragLayer

    override fun <T : View?> getOverviewPanel(): T = overviewPanel as T

    override fun getRootView(): LauncherRootView = launcherView as LauncherRootView

    override fun getActivityLaunchOptions(v: View?): ActivityOptions {
        return mAppTransitionManager.getActivityLaunchOptions(this, v)
    }

    fun getLauncherPagedView(): LauncherPagedView = workspace

    fun getStateManager() = mStateManager

    fun getAppTransitionManager() = mAppTransitionManager

    override fun reapplyUi() {
    }

    fun isWorkspaceLocked() = false

    private fun getCompositeDisposable(): CompositeDisposable {
        if (mCompositeDisposable == null || mCompositeDisposable!!.isDisposed) {
            mCompositeDisposable = CompositeDisposable()
        }
        return mCompositeDisposable!!
    }

    private fun createOrUpdateIconGrid() {
        getCompositeDisposable().add(
            BlissLauncher.getApplication(this)
                .appProvider
                .appsRepository
                .appsRelay
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<LauncherItem?>?>() {
                    override fun onNext(launcherItems: List<LauncherItem?>) {
                        showApps(launcherItems)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {}
                })
        )
    }

    private fun showApps(launcherItems: List<LauncherItem?>) {
        if (hotseat != null) hotseat.resetLayout(false)
        workspace.bindItems(launcherItems)
    }

    fun getCellLayout(container: Long, screenId: Long): CellLayout? {
        return if (container == Constants.CONTAINER_HOTSEAT) {
            if (hotseat != null) {
                hotseat.getLayout()
            } else {
                null
            }
        } else {
            workspace.getScreenWithId(screenId)
        }
    }

    fun isHotseatLayout(layout: View?): Boolean =
        hotseat != null && layout != null && layout is CellLayout && layout == hotseat.layout

    fun getViewIdForItem(info: LauncherItem): Int {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return info.keyId
    }

    fun setOnResumeCallback(callback: OnResumeCallback) {
        mOnResumeCallback?.onLauncherResume()
        mOnResumeCallback = callback
    }

    fun isInState(state: LauncherState): Boolean {
        return mStateManager.getState() === state
    }

    fun addOnResumeCallback(callback: OnResumeCallback) {
        mOnResumeCallbacks.add(callback)
    }

    companion object {
        const val TAG = "Launcher"
        const val LOGD = false

        const val DEBUG_STRICT_MODE = false

        // TODO: Remove after test is finished
        fun getLauncher(context: Context): TestActivity {
            return if (context is TestActivity) {
                context
            } else (context as ContextWrapper).baseContext as TestActivity
        }
    }

    /**
     * Callback for listening for onResume
     */
    interface OnResumeCallback {
        fun onLauncherResume()
    }
}
