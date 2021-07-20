package foundation.e.blisslauncher.features.test

import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.LayoutInflater
import android.view.View
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView
import foundation.e.blisslauncher.core.customviews.LauncherPagedView
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.launcher.Hotseat
import foundation.e.blisslauncher.features.test.LauncherState.*
import foundation.e.blisslauncher.features.test.RotationHelper.REQUEST_NONE
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.features.test.graphics.RotationMode
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

    // Type: int
    private val RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen"

    // Type: int
    private val RUNTIME_STATE = "launcher.state"

    // Type: PendingRequestArgs
    private val RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args"

    // Type: int
    private val RUNTIME_STATE_PENDING_REQUEST_CODE = "launcher.request_code"

    // Type: ActivityResultInfo
    private val RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result"

    private lateinit var launcherView: View
    private lateinit var dragLayer: DragLayer
    private lateinit var workspace: LauncherPagedView
    private lateinit var hotseat: Hotseat

    private lateinit var rotationHelper: RotationHelper
    private var mRotationMode = RotationMode.NORMAL
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

        val app = LauncherAppState.getInstance(this)
        app.launcher = this
        mOldConfig = Configuration(resources.configuration)
        initDeviceProfile(BlissLauncher.getApplication(this).invariantDeviceProfile)
        dragController = DragController(this)
        rotationHelper = RotationHelper(this)
        mStateManager = LauncherStateManager(this)
        launcherView = LayoutInflater.from(this).inflate(R.layout.activity_test, null)
        setupViews()

        mAppTransitionManager = LauncherAppTransitionManager.newInstance(this)

        val internalStateHandled = InternalStateHandler.handleCreate(this, intent)
        if (internalStateHandled) {
            savedInstanceState?.remove(RUNTIME_STATE)
        }
        restoreState(savedInstanceState)
        mStateManager.reapplyState()

        setContentView(launcherView)
        rootView.dispatchInsets()

        systemUiController.updateUiState(
            SystemUiController.UI_STATE_BASE_WINDOW,
            false
        )
        rotationHelper.initialize()
        TraceHelper.endSection("Launcher-onCreate")
        mStateManager.addStateListener(object : LauncherStateManager.StateListener {
            override fun onStateTransitionStart(toState: LauncherState?) {}
            override fun onStateTransitionComplete(finalState: LauncherState) {
                if (finalState === NORMAL) {
                    // TODO: may use later
                } else if (finalState === OVERVIEW || finalState === OVERVIEW_PEEK) {
                    // TODO: may use later
                } else {
                    // TODO: may use later
                }
            }
        })
        createOrUpdateIconGrid()
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private fun restoreState(savedState: Bundle?) {
        if (savedState == null) {
            return
        }
        val stateOrdinal =
            savedState.getInt(RUNTIME_STATE, NORMAL.ordinal)
        val stateValues = LauncherState.values()
        val state = stateValues[stateOrdinal]
        if (!state.disableRestore) {
            mStateManager.goToState(state, false /* animated */)
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        UiFactory.onEnterAnimationComplete(this)
        rotationHelper.setCurrentTransitionRequest(REQUEST_NONE)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val diff = newConfig.diff(mOldConfig)

        if (diff and ActivityInfo.CONFIG_LOCALE != 0) {
            // Folder.setLocaleDependentFields(resources, true /* force */)
            // TODO: Update folder here.
        }

        if (diff and (ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_SCREEN_SIZE) != 0) {
            // onIdpChanged(mDeviceProfile.inv)
            // TODO: update device profile here.
        }

        mOldConfig.setTo(newConfig)
        UiFactory.onLauncherStateOrResumeChanged(this)
        super.onConfigurationChanged(newConfig)
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

        // Setup the drag controller (drop targets have to be added in reverse order in priority)

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setMoveTarget(workspace)
    }

    override fun <T : View> findViewById(id: Int): T {
        return launcherView.findViewById(id)
    }

    fun getRotationHelper() = rotationHelper

    private fun initDeviceProfile(idp: InvariantDeviceProfile) {
        // Load device specific profile
        mDeviceProfile = idp.getDeviceProfile(this)
        if (isInMultiWindowMode) {
            val display = windowManager.defaultDisplay
            val mwSize = Point()
            display.getSize(mwSize)
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize)
        }
        mRotationMode = RotationMode.NORMAL
        onDeviceProfileInitiated()
    }

    override fun onPause() {
        super.onPause()
        dragController.cancelDrag()
        dragController.resetLastGestureUpTime()
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

        rotationHelper.destroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        UiFactory.onLauncherStateOrResumeChanged(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mStateManager.onWindowFocusChanged()
    }

    fun getRotationMode(): RotationMode {
        return mRotationMode
    }

    fun getHotseat() = hotseat

    fun isWorkspaceLoading() = false

    override fun getDragLayer(): DragLayer = dragLayer

    override fun <T : View> getOverviewPanel(): T = overviewPanel as T

    override fun getRootView(): LauncherRootView = launcherView as LauncherRootView

    override fun getActivityLaunchOptions(v: View?): ActivityOptions {
        return mAppTransitionManager.getActivityLaunchOptions(this, v)
    }

    fun getLauncherPagedView(): LauncherPagedView = workspace

    fun getStateManager() = mStateManager

    fun getAppTransitionManager() = mAppTransitionManager

    override fun reapplyUi() {
        rootView.dispatchInsets()
        getStateManager().reapplyState(true /* cancelCurrentAnimation */)
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

    override fun onNewIntent(intent: Intent?) {
        TraceHelper.beginSection("NEW_INTENT")
        super.onNewIntent(intent)
        val alreadyOnHome = hasWindowFocus() && (intent!!.flags and
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)

        // Check this condition before handling isActionMain, as this will get reset.
        val shouldMoveToDefaultScreen = (alreadyOnHome && isInState(NORMAL) &&
            AbstractFloatingView.getTopOpenView(this) == null)
        val isActionMain = Intent.ACTION_MAIN == intent!!.action
        val internalStateHandled = InternalStateHandler
            .handleNewIntent(this, intent, isStarted)

        if (isActionMain) {
            if (!internalStateHandled) {
                // In all these cases, only animate if we're already on home
                AbstractFloatingView.closeAllOpenViews(this, isStarted)
                if (!isInState(NORMAL)) {
                    // Only change state, if not already the same. This prevents cancelling any
                    // animations running as part of resume
                    mStateManager.goToState(NORMAL)
                }

                // Reset the apps view
                if (!alreadyOnHome) {
                    // TODO: maybe stop icon giggling or search view here.
                }
                if (shouldMoveToDefaultScreen && !workspace.isHandlingTouch) {
                    workspace.post(workspace::moveToDefaultScreen)
                }
            }

            val v = window.peekDecorView()
            if (v != null && v.windowToken != null) {
                UiThreadHelper.hideKeyboardAsync(this, v.windowToken)
            }
        }

        TraceHelper.endSection("NEW_INTENT")
    }

    override fun onBackPressed() {
        if (finishAutoCancelActionMode()) {
            return
        }

        if (dragController.isDragging()) {
            dragController.cancelDrag()
            return
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        val topView = AbstractFloatingView.getTopOpenView(this)
        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
        } else {
            mStateManager.state.onBackPressed(this)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        UiFactory.onTrimMemory(this, level)
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
