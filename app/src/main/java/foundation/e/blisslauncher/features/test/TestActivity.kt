package foundation.e.blisslauncher.features.test

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.usage.UsageStats
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.jakewharton.rxbinding3.widget.textChanges
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.Preferences
import foundation.e.blisslauncher.core.Utilities
import foundation.e.blisslauncher.core.customviews.AbstractFloatingView
import foundation.e.blisslauncher.core.customviews.BlissFrameLayout
import foundation.e.blisslauncher.core.customviews.BlissInput
import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout
import foundation.e.blisslauncher.core.customviews.InsettableScrollLayout
import foundation.e.blisslauncher.core.customviews.LauncherPagedView
import foundation.e.blisslauncher.core.customviews.RoundedWidgetView
import foundation.e.blisslauncher.core.customviews.SquareFrameLayout
import foundation.e.blisslauncher.core.customviews.WidgetHost
import foundation.e.blisslauncher.core.database.DatabaseManager
import foundation.e.blisslauncher.core.database.model.ApplicationItem
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.database.model.ShortcutItem
import foundation.e.blisslauncher.core.executors.AppExecutors
import foundation.e.blisslauncher.core.utils.AppUtils
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.core.utils.ListUtil
import foundation.e.blisslauncher.core.utils.PackageUserKey
import foundation.e.blisslauncher.core.utils.UserHandle
import foundation.e.blisslauncher.features.folder.FolderPagerAdapter
import foundation.e.blisslauncher.features.launcher.Hotseat
import foundation.e.blisslauncher.features.launcher.SearchInputDisposableObserver
import foundation.e.blisslauncher.features.notification.DotInfo
import foundation.e.blisslauncher.features.notification.NotificationDataProvider
import foundation.e.blisslauncher.features.notification.NotificationListener
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager
import foundation.e.blisslauncher.features.shortcuts.ShortcutKey
import foundation.e.blisslauncher.features.suggestions.AutoCompleteAdapter
import foundation.e.blisslauncher.features.suggestions.SearchSuggestionUtil
import foundation.e.blisslauncher.features.suggestions.SuggestionsResult
import foundation.e.blisslauncher.features.test.LauncherState.*
import foundation.e.blisslauncher.features.test.RotationHelper.REQUEST_NONE
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.features.test.graphics.RotationMode
import foundation.e.blisslauncher.features.usagestats.AppUsageStats
import foundation.e.blisslauncher.features.weather.DeviceStatusService
import foundation.e.blisslauncher.features.weather.ForecastBuilder
import foundation.e.blisslauncher.features.weather.WeatherPreferences
import foundation.e.blisslauncher.features.weather.WeatherSourceListenerService
import foundation.e.blisslauncher.features.weather.WeatherUpdateService
import foundation.e.blisslauncher.features.widgets.WidgetManager
import foundation.e.blisslauncher.features.widgets.WidgetViewBuilder
import foundation.e.blisslauncher.features.widgets.WidgetsActivity
import foundation.e.blisslauncher.uioverrides.OverlayCallbackImpl
import foundation.e.blisslauncher.uioverrides.UiFactory
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import me.relex.circleindicator.CircleIndicator

class TestActivity : BaseDraggingActivity(), AutoCompleteAdapter.OnSuggestionClickListener {

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
    private lateinit var widgetPage: InsettableFrameLayout

    private lateinit var mSearchInput: BlissInput

    private val REQUEST_LOCATION_SOURCE_SETTING = 267

    private lateinit var rotationHelper: RotationHelper
    private var mRotationMode = RotationMode.NORMAL
    private lateinit var mStateManager: LauncherStateManager

    // UI and state for the overview panel
    private lateinit var overviewPanel: View

    private lateinit var folderContainer: RelativeLayout

    private val mOnResumeCallbacks = ArrayList<OnResumeCallback>()

    private lateinit var mAppWidgetManager: AppWidgetManager
    private lateinit var mAppWidgetHost: WidgetHost
    private lateinit var widgetContainer: LinearLayout
    private var activeRoundedWidgetView: RoundedWidgetView? = null

    private lateinit var mWeatherPanel: View
    private lateinit var mWeatherSetupTextView: View
    private val allAppsDisplayed = false
    private val forceRefreshSuggestedApps = false

    private var mUsageStats: List<UsageStats>? = null

    private var enableLocationDialog: AlertDialog? = null

    private var currentAnimator: AnimatorSet? = null

    private var activeFolder: FolderItem? = null
    private var activeFolderView: IconTextView? = null
    private var activeFolderStartBounds = Rect()

    private var mFolderAppsViewPager: ViewPager? = null
    private var mFolderTitleInput: BlissInput? = null

    private val mWeatherReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!intent.getBooleanExtra(WeatherUpdateService.EXTRA_UPDATE_CANCELLED, false)) {
                updateWeatherPanel()
            }
        }
    }

    private lateinit var notificationDataProvider: NotificationDataProvider
    val mHandler = Handler()
    private val mHandleDeferredResume = Runnable { this.handleDeferredResume() }
    private var mDeferredResumePending = false

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

        mAppWidgetManager = BlissLauncher.getApplication(this).appWidgetManager
        mAppWidgetHost = BlissLauncher.getApplication(this).appWidgetHost

        initDeviceProfile(BlissLauncher.getApplication(this).invariantDeviceProfile)
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
            true
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
        setLauncherOverlay(OverlayCallbackImpl(this))
    }

    private fun askForNotificationIfFirstTime() {
        if (Preferences.shouldAskForNotificationAccess(this)) {
            val cr = contentResolver
            val setting = "enabled_notification_listeners"
            var permissionString = Settings.Secure.getString(cr, setting)
            val cn = ComponentName(this, NotificationListener::class.java)

            val enabled = permissionString != null && (permissionString.contains(cn.flattenToString()) || permissionString.contains(cn.flattenToShortString()))

            if (!enabled) {
                val launcherApps: LauncherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
                val launcherInfo = launcherApps.getApplicationInfo(packageName, 0, Process.myUserHandle())
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
        folderContainer = findViewById(R.id.folder_window_container)
        mFolderAppsViewPager = findViewById(R.id.folder_apps)
        mFolderTitleInput = findViewById(R.id.folder_title)
        launcherView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        dragLayer.setup(dragController, workspace)
        mCancelTouchController = UiFactory.enableLiveUIChanges(this)
        workspace.setup(dragController)
        setupWidgetPage()
        workspace.bindAndInitFirstScreen(null)
        dragController.addDragListener(workspace)
        dragController.addDropTarget(workspace)

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setMoveTarget(workspace)
    }

    private fun setupWidgetPage() {
        widgetPage = layoutInflater.inflate(R.layout.widgets_page, rootView, false) as InsettableFrameLayout
        rootView.addView(widgetPage)

        widgetContainer = widgetPage.findViewById(R.id.widget_container)

        widgetPage.visibility = View.GONE
        widgetPage.translationX = (widgetPage.measuredWidth * -1.00f)
        val scrollView: InsettableScrollLayout =
            widgetPage.findViewById(R.id.widgets_scroll_container)
        scrollView.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (widgetPage.findViewById<View>(R.id.widget_resizer_container)
                    .visibility
                == View.VISIBLE
            ) {
                hideWidgetResizeContainer()
            }
            false
        }
        widgetPage.findViewById<View>(R.id.used_apps_layout).clipToOutline = true
        widgetPage.tag = "Widget page"

        // TODO: replace with app predictions
        // Prepare app suggestions view
        // [[BEGIN]]
        widgetPage.findViewById<View>(R.id.openUsageAccessSettings).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        }

        // divided by 2 because of left and right padding.

        // divided by 2 because of left and right padding.
        val padding = (mDeviceProfile.availableWidthPx / 2 - Utilities.pxFromDp(8, this) -
            (2 *
            mDeviceProfile.cellWidthPx)).toInt()
        widgetPage.findViewById<View>(R.id.suggestedAppGrid).setPadding(padding, 0, padding, 0)
        // [[END]]

        // Prepare search suggestion view
        // [[BEGIN]]
        mSearchInput = widgetPage.findViewById(R.id.search_input)
        val clearSuggestions: ImageView =
            widgetPage.findViewById(R.id.clearSuggestionImageView)
        clearSuggestions.setOnClickListener {
            mSearchInput.setText("")
            mSearchInput.clearFocus()
        }

        mSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isEmpty()) {
                    clearSuggestions.visibility = View.GONE
                } else {
                    clearSuggestions.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val suggestionRecyclerView: RecyclerView =
            widgetPage.findViewById(R.id.suggestionRecyclerView)
        val suggestionAdapter = AutoCompleteAdapter(this)
        suggestionRecyclerView.setHasFixedSize(true)
        suggestionRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        suggestionRecyclerView.adapter = suggestionAdapter
        val dividerItemDecoration = DividerItemDecoration(
            this,
            DividerItemDecoration.VERTICAL
        )
        suggestionRecyclerView.addItemDecoration(dividerItemDecoration)
        getCompositeDisposable().add(
            mSearchInput.textChanges()
                .debounce(300, TimeUnit.MILLISECONDS)
                .map { obj: CharSequence -> obj.toString() }
                .distinctUntilChanged()
                .switchMap { charSequence: String? ->
                    if (charSequence != null && charSequence.isNotEmpty()) {
                        searchForQuery(charSequence)
                    } else {
                        Observable.just(
                            SuggestionsResult(charSequence)
                        )
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    SearchInputDisposableObserver(this, suggestionAdapter, widgetPage)
                )
        )

        mSearchInput.onFocusChangeListener = View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }

        mSearchInput.setOnEditorActionListener { _: TextView?, action: Int, _: KeyEvent? ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(mSearchInput)
                runSearch(mSearchInput.text.toString())
                mSearchInput.setText("")
                mSearchInput.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        // [[END]]

        // Prepare edit widgets button
        findViewById<View>(R.id.edit_widgets_button).setOnClickListener { view: View? ->
            startActivity(
                Intent(
                    this,
                    WidgetsActivity::class.java
                )
            )
        }

        // Prepare weather widget view
        // [[BEGIN]]
        findViewById<View>(R.id.weather_setting_imageview).setOnClickListener { v: View? ->
            startActivity(
                Intent(
                    this,
                    WeatherPreferences::class.java
                )
            )
        }

        mWeatherSetupTextView = findViewById<View>(R.id.weather_setup_textview)
        mWeatherPanel = findViewById(R.id.weather_panel)
        mWeatherPanel.setOnClickListener(View.OnClickListener { v: View? ->
            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    "foundation.e.weather"
                )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        })
        updateWeatherPanel()

        if (foundation.e.blisslauncher.features.weather.WeatherUtils.isWeatherServiceAvailable(
                this
            )
        ) {
            startService(Intent(this, WeatherSourceListenerService::class.java))
            startService(Intent(this, DeviceStatusService::class.java))
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mWeatherReceiver, IntentFilter(
                WeatherUpdateService.ACTION_UPDATE_FINISHED
            )
        )

        if (!Preferences.useCustomWeatherLocation(this)) {
            if (!WeatherPreferences.hasLocationPermission(this)) {
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                requestPermissions(
                    permissions,
                    WeatherPreferences.LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                    Preferences.getEnableLocation(this)
                ) {
                    showLocationEnableDialog()
                    Preferences.setEnableLocation(this)
                } else {
                    startService(
                        Intent(this, WeatherUpdateService::class.java)
                            .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE)
                    )
                }
            }
        } else {
            startService(
                Intent(this, WeatherUpdateService::class.java)
                    .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE)
            )
        }
        // [[END]]

        val widgetIds: IntArray = mAppWidgetHost.appWidgetIds
        Arrays.sort(widgetIds)
        for (id in widgetIds) {
            val appWidgetInfo: AppWidgetProviderInfo? = mAppWidgetManager.getAppWidgetInfo(id)
            if (appWidgetInfo != null) {
                val hostView: RoundedWidgetView = mAppWidgetHost.createView(
                    applicationContext, id,
                    appWidgetInfo
                ) as RoundedWidgetView
                hostView.setAppWidget(id, appWidgetInfo)
                getCompositeDisposable().add(DatabaseManager.getManager(this).getHeightOfWidget(id)
                    .subscribeOn(Schedulers.from(AppExecutors.getInstance().diskIO()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ height: Int ->
                        val widgetView = WidgetViewBuilder.create(this, hostView)
                        if (height != 0) {
                            val minHeight = hostView.appWidgetInfo.minResizeHeight
                            val maxHeight = mDeviceProfile.availableHeightPx * 3 / 4
                            val normalisedDifference = (maxHeight - minHeight) / 100
                            val newHeight = minHeight + normalisedDifference * height
                            widgetView.layoutParams.height = newHeight
                        }
                        addWidgetToContainer(widgetView)
                    }) { obj: Throwable -> obj.printStackTrace() })
            }
        }
    }

    private fun addWidgetToContainer(widgetView: RoundedWidgetView) {
        widgetView.setPadding(0, 0, 0, 0)
        widgetContainer!!.addView(widgetView)
    }

    private fun updateWeatherPanel() {
        if (Preferences.getCachedWeatherInfo(this) == null) {
            mWeatherSetupTextView.visibility = View.VISIBLE
            mWeatherPanel.visibility = View.GONE
            mWeatherSetupTextView.setOnClickListener { v: View? ->
                startActivity(
                    Intent(this, WeatherPreferences::class.java)
                )
            }
            return
        }
        mWeatherSetupTextView.visibility = View.GONE
        mWeatherPanel.visibility = View.VISIBLE
        ForecastBuilder.buildLargePanel(
            this, mWeatherPanel,
            Preferences.getCachedWeatherInfo(this)
        )
    }

    private fun showLocationEnableDialog() {
        val builder = AlertDialog.Builder(this)
        // Build and show the dialog
        builder.setTitle(R.string.weather_retrieve_location_dialog_title)
        builder.setMessage(R.string.weather_retrieve_location_dialog_message)
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.weather_retrieve_location_dialog_enable_button
        ) { dialog1, whichButton ->
            val intent =
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivityForResult(
                intent, REQUEST_LOCATION_SOURCE_SETTING
            )
        }
        builder.setNegativeButton(R.string.cancel, null)
        enableLocationDialog = builder.create()
        enableLocationDialog?.show()
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

    private fun searchForQuery(
        charSequence: CharSequence
    ): ObservableSource<SuggestionsResult?>? {
        val launcherItems = searchForLauncherItems(
            charSequence.toString()
        ).subscribeOn(Schedulers.io())
        val networkItems = searchForNetworkItems(
            charSequence
        ).subscribeOn(Schedulers.io())
        return launcherItems.mergeWith(networkItems)
    }

    private fun searchForLauncherItems(
        charSequence: CharSequence
    ): Observable<SuggestionsResult?> {
        val query = charSequence.toString().toLowerCase()
        val suggestionsResult = SuggestionsResult(
            query
        )
        val launcherItems: MutableList<LauncherItem> = ArrayList()
        workspace.mWorkspaceScreens.forEach { gridLayout: GridLayout ->
            for (i in 0 until gridLayout.childCount) {
                val blissFrameLayout = gridLayout.getChildAt(i) as BlissFrameLayout
                val launcherItem = blissFrameLayout.launcherItem
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    val folderItem = launcherItem as FolderItem
                    for (item in folderItem.items) {
                        if (item.title.toString().toLowerCase().contains(query)) {
                            launcherItems.add(item)
                        }
                    }
                } else if (launcherItem.title.toString().toLowerCase().contains(query)) {
                    launcherItems.add(launcherItem)
                }
            }
        }
        val hotseat = getHotseat()
        for (i in 0 until hotseat.childCount) {
            val blissFrameLayout = hotseat.getChildAt(i) as BlissFrameLayout
            val launcherItem = blissFrameLayout.launcherItem
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                val folderItem = launcherItem as FolderItem
                for (item in folderItem.items) {
                    if (item.title.toString().toLowerCase().contains(query)) {
                        launcherItems.add(item)
                    }
                }
            } else if (launcherItem.title.toString().toLowerCase().contains(query)) {
                launcherItems.add(launcherItem)
            }
        }
        launcherItems.sortWith(Comparator.comparing { launcherItem: LauncherItem ->
            launcherItem.title.toString().toLowerCase().indexOf(query)
        })
        if (launcherItems.size > 4) {
            suggestionsResult.launcherItems = launcherItems.subList(0, 4)
        } else {
            suggestionsResult.launcherItems = launcherItems
        }
        return Observable.just(suggestionsResult)
            .onErrorReturn { throwable: Throwable? ->
                suggestionsResult.launcherItems = ArrayList()
                suggestionsResult
            }
    }

    private fun searchForNetworkItems(charSequence: CharSequence): Observable<SuggestionsResult?> {
        val query = charSequence.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
        val suggestionProvider = SearchSuggestionUtil().getSuggestionProvider(
            this
        )
        return suggestionProvider.query(query).toObservable()
    }

    fun hideWidgetResizeContainer() {
        val widgetResizeContainer: RelativeLayout = widgetPage.findViewById<RelativeLayout>(
            R.id.widget_resizer_container
        )
        if (widgetResizeContainer.visibility == View.VISIBLE) {
            currentAnimator?.cancel()
            val set = AnimatorSet()
            set.play(
                ObjectAnimator.ofFloat(
                    widgetResizeContainer, View.Y,
                    mDeviceProfile.availableHeightPx
                        .toFloat()
                )
            )
            set.duration = 200
            set.interpolator = LinearInterpolator()
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    (widgetPage.findViewById<View>(
                        R.id.widget_resizer_seekbar
                    ) as SeekBar).setOnSeekBarChangeListener(null)
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    currentAnimator = null
                    widgetResizeContainer.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    currentAnimator = null
                    widgetResizeContainer.visibility = View.GONE
                    activeRoundedWidgetView?.removeBorder()
                }
            }
            )
            set.start()
            currentAnimator = set
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

    private fun runSearch(query: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            SearchSuggestionUtil().getUriForQuery(this, query)
        )
        startActivity(intent)
    }

    fun showWidgetResizeContainer(roundedWidgetView: RoundedWidgetView) {
        val widgetResizeContainer: RelativeLayout = widgetPage.findViewById<RelativeLayout>(
            R.id.widget_resizer_container
        )
        if (widgetResizeContainer.visibility != View.VISIBLE) {
            activeRoundedWidgetView = roundedWidgetView
            val seekBar = widgetResizeContainer.findViewById<SeekBar>(R.id.widget_resizer_seekbar)
            if (currentAnimator != null) {
                currentAnimator!!.cancel()
            }
            seekBar.setOnTouchListener { v: View?, event: MotionEvent? ->
                seekBar.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            val set = AnimatorSet()
            set.play(
                ObjectAnimator.ofFloat(
                    widgetResizeContainer, View.Y,
                    mDeviceProfile.availableHeightPx.toFloat(),
                    mDeviceProfile.availableHeightPx - Utilities.pxFromDp(48, this)
                )
            )
            set.duration = 200
            set.interpolator = LinearInterpolator()
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    widgetResizeContainer.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    currentAnimator = null
                    widgetResizeContainer.visibility = View.GONE
                    roundedWidgetView.removeBorder()
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    currentAnimator = null
                    prepareWidgetResizeSeekBar(seekBar)
                    roundedWidgetView.addBorder()
                }
            }
            )
            set.start()
            currentAnimator = set
        }
    }

    private fun prepareWidgetResizeSeekBar(seekBar: SeekBar) {
        val minHeight = activeRoundedWidgetView!!.appWidgetInfo.minResizeHeight
        val maxHeight = mDeviceProfile.availableHeightPx * 3 / 4
        val normalisedDifference = (maxHeight - minHeight) / 100
        val defaultHeight = activeRoundedWidgetView!!.height
        val currentProgress = (defaultHeight - minHeight) * 100 / (maxHeight - minHeight)
        seekBar.max = 100
        seekBar.progress = currentProgress
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newHeight = minHeight + normalisedDifference * progress
                val layoutParams = activeRoundedWidgetView!!.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = newHeight
                activeRoundedWidgetView!!.layoutParams = layoutParams
                val newOps = Bundle()
                newOps.putInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    mDeviceProfile.maxWidgetWidth
                )
                newOps.putInt(
                    AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                    mDeviceProfile.maxWidgetWidth
                )
                newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, newHeight)
                newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, newHeight)
                activeRoundedWidgetView!!.updateAppWidgetOptions(newOps)
                activeRoundedWidgetView!!.requestLayout()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                DatabaseManager.getManager(this@TestActivity).saveWidget(
                    activeRoundedWidgetView!!.appWidgetId, seekBar.progress
                )
            }
        })
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

        if (mWeatherPanel != null) {
            updateWeatherPanel()
        }

        if (widgetPage != null) {
            refreshSuggestedApps(widgetPage, forceRefreshSuggestedApps)
        }

        if (widgetContainer != null) {
            val widgetManager = WidgetManager.getInstance()
            var id = widgetManager.dequeRemoveId()
            while (id != null) {
                for (i in 0 until widgetContainer.childCount) {
                    if (widgetContainer.getChildAt(i) is RoundedWidgetView) {
                        val appWidgetHostView = widgetContainer.getChildAt(i) as RoundedWidgetView
                        if (appWidgetHostView.appWidgetId == id) {
                            widgetContainer.removeViewAt(i)
                            DatabaseManager.getManager(this).removeWidget(id)
                            break
                        }
                    }
                }
                id = widgetManager.dequeRemoveId()
            }
            var widgetView = widgetManager.dequeAddWidgetView()
            while (widgetView != null) {
                widgetView = WidgetViewBuilder.create(this, widgetView)
                addWidgetToContainer(widgetView)
                widgetView = widgetManager.dequeAddWidgetView()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        NotificationListener.removeNotificationsChangedListener()
        getStateManager().moveToRestState()

        UiFactory.onLauncherStateOrResumeChanged(this)
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

    /**
     * Call this after onCreate to set or clear overlay.
     */
    fun setLauncherOverlay(overlay: LauncherOverlay) {
        overlay.setOverlayCallbacks(LauncherOverlayCallbacksImpl())
        workspace.setLauncherOverlay(overlay)
    }

    fun isInState(state: LauncherState): Boolean {
        return mStateManager.state === state
    }

    fun addOnResumeCallback(callback: OnResumeCallback) {
        mOnResumeCallbacks.add(callback)
    }

    fun refreshSuggestedApps(viewGroup: ViewGroup, forceRefresh: Boolean) {
        val openUsageAccessSettingsTv =
            viewGroup.findViewById<TextView>(R.id.openUsageAccessSettings)
        val suggestedAppsGridLayout = viewGroup.findViewById<GridLayout>(R.id.suggestedAppGrid)
        val appUsageStats = AppUsageStats(this)
        val usageStats = appUsageStats.usageStats
        if (usageStats.size > 0) {
            openUsageAccessSettingsTv.visibility = View.GONE
            suggestedAppsGridLayout.visibility = View.VISIBLE

            // Check if usage stats have been changed or not to avoid unnecessary flickering
            if (forceRefresh || mUsageStats == null || mUsageStats!!.size != usageStats.size || !ListUtil.areEqualLists(
                    mUsageStats,
                    usageStats
                )
            ) {
                mUsageStats = usageStats
                if (suggestedAppsGridLayout.childCount > 0) {
                    suggestedAppsGridLayout.removeAllViews()
                }
                var i = 0
                while (suggestedAppsGridLayout.childCount < 4 && i < mUsageStats!!.size) {
                    val appItem = AppUtils.createAppItem(
                        this,
                        mUsageStats!![i].packageName, UserHandle()
                    )
                    if (appItem != null) {
                        val view: BlissFrameLayout = prepareSuggestedApp(appItem)
                        addAppToGrid(suggestedAppsGridLayout, view)
                    }
                    i++
                }
            }
        } else {
            openUsageAccessSettingsTv.visibility = View.VISIBLE
            suggestedAppsGridLayout.visibility = View.GONE
        }
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
        iconLayoutParams.height = mDeviceProfile.cellHeightPx
        iconLayoutParams.width = mDeviceProfile.cellWidthPx
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
            AbstractFloatingView.getTopOpenView(this) == null)
        val isActionMain = Intent.ACTION_MAIN == intent!!.action
        val internalStateHandled = InternalStateHandler
            .handleNewIntent(this, intent, isStarted)

        if (isActionMain) {
            if (!internalStateHandled) {
                // In all these cases, only animate if we're already on home
                AbstractFloatingView.closeAllOpenViews(this, isStarted)

                if (folderContainer.visibility == View.VISIBLE) {
                    closeFolder()
                    return
                }
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

        if (dragController.isDragging) {
            dragController.cancelDrag()
            return
        }

        // We expire wobble animation as soon a back press is done and return if the animation was running.
        if (getLauncherPagedView().setWobbleExpirationAlarm(0)) {
            return
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        val topView = AbstractFloatingView.getTopOpenView(this)

        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
        } else if (folderContainer.visibility == View.VISIBLE) {
            closeFolder()
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
        fun onScrollChange(progress: Float, rtl: Boolean)

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?)
    }

    interface LauncherOverlayCallbacks {
        fun onScrollChanged(progress: Float)
        fun onScrollBegin()
    }

    inner class LauncherOverlayCallbacksImpl : LauncherOverlayCallbacks {
        override fun onScrollChanged(progress: Float) {
            workspace.onOverlayScrollChanged(progress)
            widgetPage.translationX = widgetPage.measuredWidth * (progress - 1)
        }

        override fun onScrollBegin() {
            widgetPage.visibility = View.VISIBLE
        }
    }

    override fun onClick(suggestion: String) {
        mSearchInput.setText(suggestion)
        runSearch(suggestion)
        mSearchInput.clearFocus()
        mSearchInput.setText("")
    }

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
        val count = resolver.delete(
            Uri.parse("content://foundation.e.pwaplayer.provider/pwa"),
            null, arrayOf(id)
        )
        Log.d("LauncherActivity", "Items deleted from pwa provider: $count")
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

    fun openFolder(folderView: View) {
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
        /*ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 18);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blur((Integer) animation.getAnimatedValue()));*/
        /*ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 18);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blur((Integer) animation.getAnimatedValue()));*/set.play(
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
            .with(ObjectAnimator.ofFloat(hotseat, View.ALPHA, 0f))
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

        mFolderAppsViewPager?.adapter = FolderPagerAdapter(this, activeFolder?.items, mDeviceProfile)
        // We use same size for height and width as we want to look it like sqaure
        val height = mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding)
        mFolderAppsViewPager?.layoutParams?.width =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding) * 2
        mFolderAppsViewPager?.layoutParams?.height =
            (mDeviceProfile.cellHeightPx + mDeviceProfile.iconDrawablePaddingPx * 2)*3 + resources.getDimensionPixelSize(R.dimen.folder_padding) * 2
        (launcherView.findViewById<View>(R.id.indicator) as CircleIndicator).setViewPager(
            mFolderAppsViewPager
        )
    }

    fun closeFolder() {
        mFolderTitleInput?.clearFocus()
        currentAnimator?.cancel()

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.

        val set = AnimatorSet()
        /*ValueAnimator valueAnimator = ValueAnimator.ofInt(18, 0);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blurWithLauncherView(mergedView, (Integer) animation.getAnimatedValue()));*/
        /*ValueAnimator valueAnimator = ValueAnimator.ofInt(18, 0);
        valueAnimator.addUpdateListener(animation ->
                BlurWallpaperProvider.getInstance(this).blurWithLauncherView(mergedView, (Integer) animation.getAnimatedValue()));*/set.play(
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
    }
}
