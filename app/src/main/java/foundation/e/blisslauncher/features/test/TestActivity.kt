package foundation.e.blisslauncher.features.test

import android.Manifest
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.Preferences
import foundation.e.blisslauncher.core.Utilities
import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider
import foundation.e.blisslauncher.core.broadcast.WallpaperChangeReceiver
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView
import foundation.e.blisslauncher.core.customviews.BlissFrameLayout
import foundation.e.blisslauncher.core.customviews.LauncherPagedView
import foundation.e.blisslauncher.core.customviews.RoundedWidgetView
import foundation.e.blisslauncher.core.customviews.SquareFrameLayout
import foundation.e.blisslauncher.core.customviews.WidgetHost
import foundation.e.blisslauncher.core.database.model.ApplicationItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.database.model.ShortcutItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.core.utils.IntSet
import foundation.e.blisslauncher.core.utils.IntegerArray
import foundation.e.blisslauncher.core.utils.PackageUserKey
import foundation.e.blisslauncher.features.launcher.AppsRepository
import foundation.e.blisslauncher.features.launcher.Hotseat
import foundation.e.blisslauncher.features.notification.DotInfo
import foundation.e.blisslauncher.features.notification.NotificationDataProvider
import foundation.e.blisslauncher.features.notification.NotificationListener
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager
import foundation.e.blisslauncher.features.shortcuts.ShortcutKey
import foundation.e.blisslauncher.features.suggestions.SearchSuggestionUtil
import foundation.e.blisslauncher.features.test.LauncherState.*
import foundation.e.blisslauncher.features.test.RotationHelper.REQUEST_NONE
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.features.test.graphics.RotationMode
import foundation.e.blisslauncher.features.weather.WeatherPreferences
import foundation.e.blisslauncher.features.weather.WeatherUpdateService
import foundation.e.blisslauncher.uioverrides.OverlayCallbackImpl
import foundation.e.blisslauncher.uioverrides.UiFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.function.Predicate

class TestActivity : BaseDraggingActivity(), LauncherModel.Callbacks {

    private var mIsEditingName: Boolean = false
    private var mModel: LauncherModel? = null
    private lateinit var overlayCallbackImpl: OverlayCallbackImpl

    // Folder start scale
    private var startScaleFinal: Float = 0f
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

    private val REQUEST_LOCATION_SOURCE_SETTING = 267

    private lateinit var rotationHelper: RotationHelper
    private var mRotationMode = RotationMode.NORMAL
    private lateinit var mStateManager: LauncherStateManager

    // UI and state for the overview panel
    private lateinit var overviewPanel: View

    private val mOnResumeCallbacks = ArrayList<OnResumeCallback>()

    lateinit var mAppWidgetManager: AppWidgetManager
    lateinit var mAppWidgetHost: WidgetHost

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var notificationDataProvider: NotificationDataProvider
    val mHandler = Handler()
    private val mHandleDeferredResume = Runnable { this.handleDeferredResume() }
    private var mDeferredResumePending = false

    private val TAG = "TestActivity"

    private var wallpaperChangeReceiver: WallpaperChangeReceiver? = null

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
        mModel = app.setLauncher(this)
        mOldConfig = Configuration(resources.configuration)

        mAppWidgetManager = BlissLauncher.getApplication(this).appWidgetManager
        mAppWidgetHost = BlissLauncher.getApplication(this).appWidgetHost

        initDeviceProfile(BlissLauncher.getApplication(this).invariantDeviceProfile)
        val wm = getSystemService(WALLPAPER_SERVICE) as WallpaperManager
        wm.suggestDesiredDimensions(mDeviceProfile.widthPx, mDeviceProfile.heightPx)
        dragController = DragController(this)
        rotationHelper = RotationHelper(this)
        mStateManager = LauncherStateManager(this)
        launcherView = LayoutInflater.from(this).inflate(R.layout.activity_test, null)
        setupViews()

        askForNotificationIfFirstTime()

        notificationDataProvider = NotificationDataProvider(this)

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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
        createOrUpdateIconGrid()
        overlayCallbackImpl = OverlayCallbackImpl(this)
        // setLauncherOverlay(overlayCallbackImpl)
    }

    private fun askForNotificationIfFirstTime() {
        if (Preferences.shouldAskForNotificationAccess(this)) {
            val cr = contentResolver
            val setting = "enabled_notification_listeners"
            var permissionString = Settings.Secure.getString(cr, setting)
            val cn = ComponentName(this, NotificationListener::class.java)

            val enabled =
                permissionString != null && (permissionString.contains(cn.flattenToString()) || permissionString.contains(
                    cn.flattenToShortString()
                ))

            if (!enabled) {
                val launcherApps: LauncherApps =
                    getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
                val launcherInfo =
                    launcherApps.getApplicationInfo(packageName, 0, Process.myUserHandle())
                if (launcherInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                    val args = Bundle()
                    args.putString(":settings:fragment_args_key", cn.flattenToString())
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(":settings:fragment_args_key", cn.flattenToString())
                        .putExtra(":settings:show_fragment_args", args)
                    startActivity(
                        intent
                    )
                } else {
                    if (permissionString == null) {
                        permissionString = ""
                    } else {
                        permissionString += ":"
                    }
                    permissionString += cn.flattenToString()

                    // Requires WRITE_SECURE_SETTINGS permission.
                    Settings.Secure.putString(cr, setting, permissionString)
                }
            }
            Preferences.setNotToAskForNotificationAccess(this)
        }
    }

    private fun handleDeferredResume() {
        mDeferredResumePending = if (hasBeenResumed() && !mStateManager.state.disableInteraction) {
            UiFactory.onLauncherStateOrResumeChanged(this)

            // Process any items that were added while Launcher was away.
            // TODO: Enable it when adding folder support
            /*InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED, this
            )*/

            // Refresh shortcuts if the permission changed.
            // mModel.refreshShortcutsIfRequired()

            // Set the notification listener and fetch updated notifications when we resume
            NotificationListener.setNotificationsChangedListener(notificationDataProvider)
            false
        } else {
            true
        }
    }

    fun onStateSet(state: LauncherState) {
        if (mDeferredResumePending) {
            handleDeferredResume()
        }
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
        dragController.addDragListener(workspace)
        dragController.addDropTarget(workspace)

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setMoveTarget(workspace)

        wallpaperChangeReceiver = WallpaperChangeReceiver(workspace)
        workspace.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                wallpaperChangeReceiver?.setWindowToken(v.windowToken)
            }

            override fun onViewDetachedFromWindow(v: View) {
                wallpaperChangeReceiver?.setWindowToken(null)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == WeatherPreferences.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // We only get here if user tried to enable the preference,
                // hence safe to turn it on after permission is granted
                val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    workspace.showLocationEnableDialog()
                    Preferences.setEnableLocation(this)
                } else {
                    startService(
                        Intent(this, WeatherUpdateService::class.java)
                            .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE)
                    )
                }
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                BlurWallpaperProvider.getInstance(applicationContext).updateAsync()
            }
        } else super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_LOCATION_SOURCE_SETTING) {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(
                    this, "Set custom location in weather settings.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startService(
                    Intent(this, WeatherUpdateService::class.java)
                        .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE)
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager = (getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager)
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: View?) {
        val inputMethodManager = (getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager)
        inputMethodManager.showSoftInput(view, 0)
    }

    fun runSearch(query: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            SearchSuggestionUtil().getUriForQuery(this, query)
        )
        startActivity(intent)
    }

    fun showWidgetResizeContainer(roundedWidgetView: RoundedWidgetView) {
        workspace.showWidgetResizeContainer(roundedWidgetView)
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
        workspace.hideWidgetResizeContainer()
    }

    override fun onResume() {
        super.onResume()
        mHandler.removeCallbacks(mHandleDeferredResume)
        Utilities.postAsyncCallback(mHandler, mHandleDeferredResume)

        if (mOnResumeCallbacks.isNotEmpty()) {
            val resumeCallbacks = ArrayList(mOnResumeCallbacks)
            mOnResumeCallbacks.clear()
            for (i in resumeCallbacks.indices.reversed()) {
                resumeCallbacks[i].onLauncherResume()
            }
            resumeCallbacks.clear()
        }

        workspace.updateWeatherPanel()
        workspace.refreshSuggestedApps(false) // TODO: Update with app remove event
        workspace.updateWidgets()
    }

    override fun onStop() {
        super.onStop()
        NotificationListener.removeNotificationsChangedListener()
        getStateManager().moveToRestState()

        UiFactory.onLauncherStateOrResumeChanged(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        workspace.removeFolderListeners()
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

    fun getCompositeDisposable(): CompositeDisposable {
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
                .allItemsRelay
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<AppsRepository.AllItems>() {
                    override fun onNext(items: AppsRepository.AllItems) {
                        mainHandler.post { showApps(items) }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {}
                })
        )
    }

    private fun showApps(allItems: AppsRepository.AllItems) {
        hotseat.resetLayout(false)
        val populatedItems = populateItemPositions(allItems.items)
        val orderedScreenIds = IntegerArray()
        orderedScreenIds.addAll(collectWorkspaceScreens(populatedItems))
        workspace.removeAllWorkspaceScreens()
        workspace.bindScreens(orderedScreenIds)
        workspace.post(workspace::moveToDefaultScreen)
        workspace.bindItems(populatedItems, false)
        workspace.bindItemsAdded(allItems.newAddedItems)
    }

    private fun populateItemPositions(launcherItems: List<LauncherItem>): List<LauncherItem> {
        var newScreenId = 0
        var cell = 0
        val maxChild = deviceProfile.inv.numRows * deviceProfile.inv.numColumns
        return launcherItems.map { item ->
            if (item.screenId > newScreenId) {
                newScreenId = item.screenId + 1
            }

            if (item.container == Constants.CONTAINER_DESKTOP && item.screenId == -1) {
                if (cell == maxChild) {
                    cell = 0
                    newScreenId++
                }
                item.screenId = newScreenId
                item.cell = cell
                cell++
            }
            item
        }
    }

    private fun collectWorkspaceScreens(launcherItems: List<LauncherItem>): IntegerArray {
        val screenSet = IntSet()
        launcherItems.filter { it.container == Constants.CONTAINER_DESKTOP && it.screenId != -1 }
            .forEach { screenSet.add(it.screenId) }
        return screenSet.array
    }

    fun getCellLayout(container: Long, screenId: Int): CellLayout? {
        return if (container == Constants.CONTAINER_HOTSEAT) {
            hotseat.layout
        } else {
            workspace.getScreenWithId(screenId)
        }
    }

    fun isHotseatLayout(layout: View?): Boolean =
        layout != null && layout is CellLayout && layout == hotseat.layout

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

    /**
     * Call this after onCreate to set or clear overlay.
     */
    /*private fun setLauncherOverlay(overlay: LauncherOverlay) {
        overlay.setOverlayCallbacks(LauncherOverlayCallbacksImpl())
        workspace.setLauncherOverlay(overlay)
        widgetRootView.setLauncherOverlay(overlay)
    }*/

    fun isInState(state: LauncherState): Boolean {
        return mStateManager.state === state
    }

    fun addOnResumeCallback(callback: OnResumeCallback) {
        mOnResumeCallbacks.add(callback)
    }

    fun prepareSuggestedApp(launcherItem: LauncherItem): BlissFrameLayout {
        val v = layoutInflater.inflate(
            R.layout.app_view,
            null
        ) as BlissFrameLayout
        v.launcherItem = launcherItem
        val icon: SquareFrameLayout = v.findViewById(R.id.app_icon)
        icon.setOnClickListener { view: View? ->
            startActivitySafely(
                view,
                launcherItem.intent,
                launcherItem
            )
        }
        return v
    }

    fun addAppToGrid(grid: GridLayout, view: BlissFrameLayout) {
        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED)
        val iconLayoutParams = GridLayout.LayoutParams(rowSpec, colSpec)
        val emptySpace = mDeviceProfile.availableWidthPx - 2 * Utilities.pxFromDp(16, this) - 4 *
            mDeviceProfile.cellWidthPx
        val padding = emptySpace / 10
        val topBottomPadding = Utilities.pxFromDp(8, this).toInt()
        iconLayoutParams.height = (mDeviceProfile.cellHeightPx + topBottomPadding)
        iconLayoutParams.width = (mDeviceProfile.cellWidthPx + padding * 2).toInt()

        view.setPadding(
            padding.toInt(),
            (topBottomPadding / 2), padding.toInt(), topBottomPadding / 2
        )
        view.findViewById<View>(R.id.app_label).visibility = View.VISIBLE
        view.layoutParams = iconLayoutParams
        view.setWithText(true)
        grid.addView(view)
        // DatabaseManager.getManager(this).saveLayouts(pages, mDock);
    }

    override fun onNewIntent(intent: Intent?) {
        TraceHelper.beginSection("NEW_INTENT")
        super.onNewIntent(intent)
        val alreadyOnHome = hasWindowFocus() && (intent!!.flags and
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)

        // Check this condition before handling isActionMain, as this will get reset.
        val shouldMoveToDefaultScreen = (alreadyOnHome && isInState(NORMAL) &&
            AbstractFloatingView.getTopOpenView(this) == null) &&
            ((workspace.activeRoundedWidgetView?.isWidgetActivated ?: false).not())
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
                } else {
                    workspace.hideWidgetResizeContainer()
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

        if (dragController.isDragging) {
            dragController.cancelDrag()
            return
        }

        // We expire wobble animation as soon a back press is done and return if the animation was running.
        if (getLauncherPagedView().setWobbleExpirationAlarm(0)) {
            return
        }

        workspace.clearWidgetState()

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        val topView = AbstractFloatingView.getTopOpenView(this)

        if (topView != null && topView.onBackPressed()) {
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

        private val STORAGE_PERMISSION_REQUEST_CODE: Int = 586

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

    interface LauncherOverlay {
        /**
         * Touch interaction leading to overscroll has begun
         */
        fun onScrollInteractionBegin()

        /**
         * Touch interaction related to overscroll has ended
         */
        fun onScrollInteractionEnd()

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        fun onScrollChange(progress: Float, scrollFromWorkspace: Boolean, rtl: Boolean)

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?)
    }

    interface LauncherOverlayCallbacks {
        fun onScrollChanged(progress: Float, scrollFromWorkspace: Boolean)
        fun onScrollBegin()
        fun onScrollEnd(finalProgress: Float, scrollFromWorkspace: Boolean)
    }

    // TODO: Maybe we need to simplify the logic here.
    /*inner class LauncherOverlayCallbacksImpl : LauncherOverlayCallbacks {

        private var currentProgress = 0f
        private var isScrolling = false
        private var animator: ValueAnimator? = null

        override fun onScrollChanged(progress: Float, scrollFromWorkspace: Boolean) {
            this.currentProgress = progress

            if (animator == null && isScrolling) {
                if (scrollFromWorkspace) {
                    workspace.onOverlayScrollChanged(progress)
                    widgetPage.translationX = widgetPage.measuredWidth * (progress - 1)
                    widgetPage.changeBlurBounds(progress, true)
                } else {
                    workspace.onOverlayScrollChanged(progress)
                    Log.i(TAG, "onScrollChanged: $progress")
                    widgetPage.changeBlurBounds(progress, false)
                }
            }
        }

        override fun onScrollBegin() {
            isScrolling = true
        }

        override fun onScrollEnd(finalProgress: Float, scrollFromWorkspace: Boolean) {
            isScrolling = false
            if (scrollFromWorkspace) {
                val workspaceAnim = ValueAnimator.ofFloat(currentProgress, finalProgress)
                workspaceAnim.addUpdateListener {
                    workspace.onOverlayScrollChanged(it.animatedValue as Float)
                    widgetPage.translationX =
                        widgetPage.measuredWidth * (it.animatedValue as Float - 1)
                    widgetPage.changeBlurBounds(it.animatedValue as Float, true)
                }
                workspaceAnim.duration = 300
                workspaceAnim.interpolator = AccelerateDecelerateInterpolator()
                workspaceAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        workspace.onOverlayScrollChanged(0f)
                        widgetPage.translationX = (-widgetPage.measuredWidth).toFloat()
                        widgetPage.changeBlurBounds(0f, true)
                        animator = null
                    }
                })
                animator = workspaceAnim
            } else {
                val workspaceAnim = ValueAnimator.ofFloat(currentProgress, finalProgress)
                workspaceAnim.addUpdateListener {
                    workspace.onOverlayScrollChanged(it.animatedValue as Float)
                    widgetPage.translationX =
                        widgetPage.measuredWidth * (it.animatedValue as Float - 1)
                    widgetPage.changeBlurBounds(it.animatedValue as Float, false)
                }
                workspaceAnim.duration = 300
                workspaceAnim.interpolator = AccelerateDecelerateInterpolator()
                workspaceAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        workspace.onOverlayScrollChanged(1f)
                        widgetPage.translationX = 0f
                        widgetPage.changeBlurBounds(1f, false)
                        animator = null
                    }
                })
                animator = workspaceAnim
            }
            animator?.start()
        }
    }*/

    fun updateNotificationDots(updatedDots: Predicate<PackageUserKey>) {
        workspace.updateNotificationBadge(updatedDots)
    }

    fun getDotInfoForItem(info: LauncherItem?): DotInfo? {
        return notificationDataProvider.getDotInfoForItem(info)
    }

    fun removeShortcut(shortcut: ShortcutItem): Boolean {
        val dialog: AlertDialog = AlertDialog.Builder(
            ContextThemeWrapper(
                this,
                R.style.AlertDialogCustom
            )
        )
            .setTitle(shortcut.title)
            .setMessage(R.string.uninstall_shortcut_dialog)
            .setPositiveButton(R.string.ok) { dialog1, which ->
                if (shortcut.packageName != null) {
                    DeepShortcutManager.getInstance(this)
                        .unpinShortcut(ShortcutKey.fromItem(shortcut))
                    if (DeepShortcutManager.getInstance(this).wasLastCallSuccess()) {
                        deleteShortcutFromProvider(shortcut.id)
                        // TODO: Enable when adding shortcut support
                        // removeShortcutView(shortcut, blissFrameLayout)
                    }
                } else {
                    // Null package name generally comes for nougat shortcuts so don't unpin here, just directly delete it.
                    deleteShortcutFromProvider(shortcut.id)
                    // TODO: Enable when adding shortcut support
                    // removeShortcutView(shortcut, blissFrameLayout)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setIcon(shortcut.icon)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.color_blue))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.color_blue))
        }
        dialog.show()
        return true
    }

    private fun deleteShortcutFromProvider(id: String) {
        val resolver = contentResolver
        try {
            val count = resolver.delete(
                Uri.parse("content://foundation.e.pwaplayer.provider/pwa"),
                null, arrayOf(id)
            )
            Log.d("LauncherActivity", "Items deleted from pwa provider: $count")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * @return the component name that should be uninstalled or null.
     */
    private fun getUninstallTarget(item: LauncherItem?): ComponentName? {
        var intent: Intent? = null
        var user: android.os.UserHandle? = null
        if (item != null && item.itemType == Constants.ITEM_TYPE_APPLICATION) {
            intent = item.intent
            user = item.user.realHandle
        }
        if (intent != null) {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val info: LauncherActivityInfo = launcherApps.resolveActivity(intent, user)
            if (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                return info.componentName
            }
        }
        return null
    }

    fun uninstallApplication(applicationItem: ApplicationItem): Boolean {
        val cn = getUninstallTarget(applicationItem)
        if (cn == null) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            Toast.makeText(this, R.string.uninstall_system_app_text, Toast.LENGTH_SHORT)
                .show()
            return true
        }

        return try {
            val i: Intent =
                Intent.parseUri(getString(R.string.delete_package_intent), 0)
                    .setData(Uri.fromParts("package", cn.packageName, cn.className))
                    .putExtra(Intent.EXTRA_USER, applicationItem.user.realHandle)
            startActivity(i)
            true
        } catch (e: URISyntaxException) {
            Log.e(
                "IconTextView",
                "Failed to parse intent to start uninstall activity for item=$applicationItem"
            )
            true
        }
    }

    /*fun openFolder(folderView: View) {
        if (currentAnimator != null) {
            currentAnimator!!.cancel()
        }

        activeFolder = folderView.tag as FolderItem?
        activeFolderView = folderView as IconTextView

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBounds = Rect()
        val finalBounds = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).

        folderView.getGlobalVisibleRect(startBounds)
        findViewById<View>(R.id.workspace)
            .getGlobalVisibleRect(finalBounds, globalOffset)
        startBounds.offset(-globalOffset.x, -globalOffset.y)
        finalBounds.offset(-globalOffset.x, -globalOffset.y)

        activeFolderStartBounds.set(startBounds)
        val startScale: Float
        if (finalBounds.width() / finalBounds.height()
            > startBounds.width() / startBounds.height()
        ) {
            // Extend start bounds horizontally
            startScale = (startBounds.height() / finalBounds.height()).toFloat()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = (startBounds.width() / finalBounds.width()).toFloat()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        val set = AnimatorSet()
        *//*ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 18);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blur((Integer) animation.getAnimatedValue()));*//*
        *//*ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 18);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blur((Integer) animation.getAnimatedValue()));*//*
        set.play(
            ObjectAnimator.ofFloat(
                folderContainer, View.X,
                startBounds.left.toFloat(), finalBounds.left
                    .toFloat()
            )
        )
            .with(
                ObjectAnimator.ofFloat(
                    folderContainer, View.Y,
                    startBounds.top.toFloat(), finalBounds.top
                        .toFloat()
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    folderContainer, View.SCALE_X,
                    startScale, 1f
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    folderContainer,
                    View.SCALE_Y, startScale, 1f
                )
            )
            .with(ObjectAnimator.ofFloat<View>(getLauncherPagedView(), View.ALPHA, 0f))
            // .with(ObjectAnimator.ofFloat<View>(mIndicator, View.ALPHA, 0f))
            .with()
        set.duration = 300
        set.interpolator = LinearInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                folderContainer.visibility = View.VISIBLE

                // Set the pivot point for SCALE_X and SCALE_Y transformations
                // to the top-left corner of the zoomed-in view (the default
                // is the center of the view).
                folderContainer.pivotX = 0f
                folderContainer.pivotY = 0f
                // BlurWallpaperProvider.getInstance(LauncherActivity.this).clear();
            }

            override fun onAnimationEnd(animation: Animator) {
                currentAnimator = null
                // blurLayer.setAlpha(1f)
                getLauncherPagedView().alpha = 0f
                hotseat.alpha = 0f
            }

            override fun onAnimationCancel(animation: Animator) {
                currentAnimator = null
                folderContainer.visibility = View.GONE
                // blurLayer.setAlpha(0f)
                getLauncherPagedView().alpha = 1f
                // mIndicator.setAlpha(1f)
                hotseat.alpha = 1f
            }
        })
        set.start()
        currentAnimator = set
        startScaleFinal = startScale

        mFolderTitleInput?.setText(activeFolder?.title)
        mFolderTitleInput?.isCursorVisible = false

        mFolderAppsViewPager?.adapter =
            FolderPagerAdapter(this, activeFolder?.items, mDeviceProfile)
        // We use same size for height and width as we want to look it like sqaure
        val height =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding)
        mFolderAppsViewPager?.layoutParams?.width =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding) * 2
        mFolderAppsViewPager?.layoutParams?.height =
            (mDeviceProfile.cellHeightPx + mDeviceProfile.iconDrawablePaddingPx * 2) * 3 + resources.getDimensionPixelSize(
                R.dimen.folder_padding
            ) * 2
        (launcherView.findViewById<View>(R.id.indicator) as CircleIndicator).setViewPager(
            mFolderAppsViewPager
        )
    }

    fun closeFolder() {
        if (isEditingName()) {
            mFolderTitleInput.dispatchBackKey()
        }

        currentAnimator?.cancel()

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.

        val set = AnimatorSet()
        *//*ValueAnimator valueAnimator = ValueAnimator.ofInt(18, 0);
        valueAnimator.addUpdateListener(animati on ->
                BlurWallpaperProvider.getInstance(this).blurWithLauncherView(mergedView, (Integer) animation.getAnimatedValue()));*//*
        *//*ValueAnimator valueAnimator = ValueAnimator.ofInt(18, 0);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blurWithLauncherView(mergedView, (Integer) animation.getAnimatedValue()));*//*set.play(
            ObjectAnimator
                .ofFloat(folderContainer, View.X, activeFolderStartBounds.left.toFloat())
        )
            .with(
                ObjectAnimator
                    .ofFloat(
                        folderContainer,
                        View.Y, activeFolderStartBounds.top
                            .toFloat()
                    )
            )
            .with(
                ObjectAnimator
                    .ofFloat<View>(
                        folderContainer,
                        View.SCALE_X, startScaleFinal
                    )
            )
            .with(
                ObjectAnimator
                    .ofFloat(
                        folderContainer,
                        View.SCALE_Y, startScaleFinal
                    )
            )
            // .with(ObjectAnimator.ofFloat<View>(blurLayer, View.ALPHA, 0f))
            .with(ObjectAnimator.ofFloat(getLauncherPagedView(), View.ALPHA, 1f))
            .with(ObjectAnimator.ofFloat(getLauncherPagedView().pageIndicator, View.ALPHA, 1f))
            .with(ObjectAnimator.ofFloat(hotseat, View.ALPHA, 1f))
        // .with(valueAnimator);
        set.duration = 300
        set.interpolator = LinearInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                getLauncherPagedView().isVisible = true
                hotseat.visibility = View.VISIBLE
                getLauncherPagedView().pageIndicator.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                folderContainer.visibility = View.GONE
                currentAnimator = null
                // blurLayer.setAlpha(0f)
                getLauncherPagedView().alpha = 1f
                getLauncherPagedView().pageIndicator.alpha = 1f
                hotseat.alpha = 1f
            }

            override fun onAnimationCancel(animation: Animator) {
                folderContainer.visibility = View.GONE
                currentAnimator = null
                // blurLayer.setAlpha(0f)
                getLauncherPagedView().alpha = 1f
                getLauncherPagedView().pageIndicator.alpha = 1f
                hotseat.alpha = 1f
            }
        })
        set.start()
        currentAnimator = set
    }*/

    override fun bindAppsAdded(items: MutableList<LauncherItem>) {
        if (items.isEmpty()) return
        workspace.bindItemsAdded(items)
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    override fun bindWorkspaceComponentsRemoved(matcher: LauncherItemMatcher) {
        workspace.removeItemsByMatcher(matcher)
        dragController.onAppsRemoved(matcher)
    }
}
