package foundation.e.blisslauncher.features.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.WallpaperChangeReceiver
import foundation.e.blisslauncher.common.DeviceProfile
import foundation.e.blisslauncher.common.InvariantDeviceProfile
import foundation.e.blisslauncher.common.util.LongArrayMap
import foundation.e.blisslauncher.domain.entity.LauncherConstants
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.widget.Insettable
import foundation.e.blisslauncher.widget.PagedView
import timber.log.Timber
import javax.inject.Inject

class Workspace @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PagedView(context, attrs, defStyleAttr), Insettable {

    private val mTempXY: IntArray = IntArray(2)
    private var mYDown: Float = 0.0f
    private var mXDown: Float = 0.0f
    private val FADE_EMPTY_SCREEN_DURATION: Int = 150
    private val SNAP_OFF_EMPTY_SCREEN_DURATION: Int = 400

    private lateinit var wallpaperReceiver: WallpaperChangeReceiver

    @get:JvmName("getLayoutTransition_")
    private val layoutTransition: LayoutTransition by lazy {
        LayoutTransition().apply {
            this.enableTransitionType(LayoutTransition.DISAPPEARING)
            this.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
            this.disableTransitionType(LayoutTransition.APPEARING)
            this.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
        }
    }

    private val wallpaperManager: WallpaperManager

    @Inject
    lateinit var invariantDeviceProfile: InvariantDeviceProfile
    lateinit var deviceProfile: DeviceProfile

    private var maxDistanceForFolderCreation: Float = 0.0f
    private val screenOrder = ArrayList<Long>()
    private val workspaceScreens = LongArrayMap<GridLayout>()

    init {

        deviceProfile = LauncherActivity.getLauncher(context).deviceProfile
        invariantDeviceProfile = deviceProfile.inv

        wallpaperReceiver = WallpaperChangeReceiver(this)
        isHapticFeedbackEnabled = false

        wallpaperManager = WallpaperManager.getInstance(context)
        currentPage = DEFAULT_PAGE
        clipToPadding = false

        setLayoutTransition(layoutTransition)


        // Set the wallpaper dimensions when Launcher starts up
        setWallpaperDimension()
        isMotionEventSplittingEnabled = true

        //TODO: Set touch listener if required

    }

    private fun setupLayoutTransition() {
        // We want to show layout transitions when pages are deleted, to close the gap.
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
        layoutTransition.disableTransitionType(LayoutTransition.APPEARING)
        layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
    }

    fun enableLayoutTransitions() {
        setLayoutTransition(layoutTransition)
    }

    fun disableLayoutTransitions() {
        setLayoutTransition(null)
    }

    private fun setWallpaperDimension() {
        //TODO: Run it on a separate thread
        val size: Point = invariantDeviceProfile.defaultWallpaperSize
        if (size.x != wallpaperManager.desiredMinimumWidth ||
            size.y != wallpaperManager.desiredMinimumHeight
        ) {
            wallpaperManager.suggestDesiredDimensions(size.x, size.y)
        }
    }

    override fun setInsets(insets: Rect) {
        this.insets.set(insets)
        maxDistanceForFolderCreation = 0.55f * deviceProfile.iconSizePx

        val padding: Rect = deviceProfile.workspacePadding
        setPadding(padding.left, padding.top, padding.right, padding.bottom)
    }

    override fun onViewAdded(child: View?) {
        require(child is GridLayout) { "A Workspace can only have CellLayout children." }
        val gridlayout = child as GridLayout
        super.onViewAdded(child)
    }

    val isTouchActive: Boolean
        get() = touchState != TOUCH_STATE_REST

    fun removeAllWorkspaceScreens() {
        disableLayoutTransitions()
        //removeFolderListners()
        screenOrder.clear()
        workspaceScreens.clear()
        enableLayoutTransitions()
    }

    fun insertNewWorkspaceScreenBeforeEmptyScreen(screenId: Long) {
        var insertIndex = screenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID)
        if (insertIndex < 0) {
            insertIndex = screenOrder.size
        }

        insertNewWorkspaceScreen(screenId, insertIndex)
    }

    fun insertNewWorkspaceScreen(screenId: Long) {
        insertNewWorkspaceScreen(screenId, childCount)
    }

    fun insertNewWorkspaceScreen(screenId: Long, insertIndex: Int): GridLayout {
        if (workspaceScreens.containsKey(screenId)) {
            throw RuntimeException("Screen id $screenId already exists!")
        }

        val newScreen = LayoutInflater.from(context)
            .inflate(R.layout.layout_workspace_screen, this, false) as GridLayout
        // TODO: Set padding if needed
        newScreen.columnCount = invariantDeviceProfile.numColumns
        newScreen.rowCount = invariantDeviceProfile.numRows
        workspaceScreens.put(screenId, newScreen)
        screenOrder.add(insertIndex, screenId)
        addView(newScreen, insertIndex)

        // TODO: Apply state transition animation if needed
        // TODO: Enable accessibilty
        return newScreen
    }

    fun addExtraEmptyScreenOnDrag() {
        //TODO: Add once drag layer is done
    }

    fun addExtraEmptyScreen(): Boolean {
        if (!workspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID)
            return true
        }
        return false
    }

    fun convertFinalScreenToEmptyScreenIfNecessary() {
        //TODO: Return if early if workspace is loading

        if (hasExtraEmptyScreen || screenOrder.size == 0) return
        val finalScreenId = screenOrder[screenOrder.size - 1]
        val finalScreen = workspaceScreens[finalScreenId]
        // TODO: If the final screen is empty, convert it to extra empty screen
    }

    fun removeExtraEmptyScreen(
        animate: Boolean,
        stripEmptyScreens: Boolean
    ) {
        removeExtraEmptyScreenDelayed(animate, null, 0, stripEmptyScreens)
    }

    fun removeExtraEmptyScreenDelayed(
        animate: Boolean,
        onComplete: Runnable?,
        delay: Int,
        stripEmptyScreens: Boolean
    ) {
        //TODO: Return if early if workspace is loading

        if (delay > 0) {
            postDelayed({
                removeExtraEmptyScreenDelayed(animate, onComplete, 0, stripEmptyScreens)
            }, delay.toLong())
            return
        }

        convertFinalScreenToEmptyScreenIfNecessary()

        if (hasExtraEmptyScreen) {
            val emptyIndex: Int =
                screenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID)
            if (nextPage == emptyIndex) {
                snapToPage(
                    nextPage - 1,
                    SNAP_OFF_EMPTY_SCREEN_DURATION
                )
                fadeAndRemoveEmptyScreen(
                    SNAP_OFF_EMPTY_SCREEN_DURATION,
                    FADE_EMPTY_SCREEN_DURATION,
                    onComplete,
                    stripEmptyScreens
                )
            } else {
                snapToPage(nextPage, 0)
                fadeAndRemoveEmptyScreen(
                    0, FADE_EMPTY_SCREEN_DURATION,
                    onComplete, stripEmptyScreens
                )
            }
            return
        } else if (stripEmptyScreens) {
            // If we're not going to strip the empty screens after removing
            // the extra empty screen, do it right away.
            stripEmptyScreens()
        }
        onComplete?.run()
    }

    private fun fadeAndRemoveEmptyScreen(
        delay: Int,
        duration: Int,
        onComplete: Runnable?,
        stripEmptyScreens: Boolean
    ) {
        // XXX: Do we need to update LM workspace screens below?
        val alpha = PropertyValuesHolder.ofFloat("alpha", 0f)
        val bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0f)
        val gl: GridLayout =
            workspaceScreens.get(EXTRA_EMPTY_SCREEN_ID)
        val oa: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(gl, alpha, bgAlpha)
        oa.duration = duration.toLong()
        oa.startDelay = delay.toLong()
        oa.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Runnable {
                    if (hasExtraEmptyScreen) {
                        workspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID)
                        screenOrder.remove(EXTRA_EMPTY_SCREEN_ID)
                        removeView(gl)
                        if (stripEmptyScreens) {
                            stripEmptyScreens()
                        }
                        // Update the page indicator to reflect the removed page.
                        //showPageIndicatorAtCurrentScroll()
                    }
                }.run()
                onComplete?.run()
            }
        })
        oa.start()
    }

    val hasExtraEmptyScreen: Boolean
        get() = workspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID) && childCount > 1

    fun commitExtraEmptyScreen(): Long {
        // TODO: Return -1 if launcher is loading

        // TODO: Update the model here
        TODO()
    }

    fun getScreenWithId(screenId: Long) = workspaceScreens[screenId]

    fun getIdForScreen(screen: GridLayout): Long {
        val index = workspaceScreens.indexOfValue(screen)
        return if (index != -1) workspaceScreens.keyAt(index) else -1
    }

    fun getPageIndexForScreen(screenId: Long) = indexOfChild(workspaceScreens[screenId])

    fun getScreenIdForPageIndex(index: Int): Long {
        if (0 <= index && index < screenOrder.size) {
            screenOrder[index]
        }
        return -1
    }

    fun getScreenOrder() = screenOrder

    fun stripEmptyScreens() {
        // TODO: Return early if launcher is loading
    }

    fun addInScreenFromBind(child: View, item: LauncherItem) {
        val x = item.cellX
        val y = item.cellY
        addInScreen(child, item.container, item.screenId, x, y)
    }

    fun addInScreen(child: View, item: LauncherItem) {
        addInScreen(child, item.container, item.screenId, item.cellX, item.cellY)
    }

    private fun addInScreen(child: View, container: Long, screenId: Long, x: Int, y: Int) {
        if (container == LauncherConstants.ContainerType.CONTAINER_DESKTOP) {
            if (getScreenWithId(screenId) == null) {
                Timber.e("Skipping child, screenId $screenId not found")
                // DEBUGGING - Print out the stack trace to see where we are adding from
                Throwable().printStackTrace()
                return
            }
        }

        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            throw RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID")
        }

        if (container == LauncherConstants.ContainerType.CONTAINER_HOTSEAT) {
            //TODO: Hide folder title in hotseat
        } else {
        }
        // TODO: Add view to gridlayout here

        child.isHapticFeedbackEnabled = false
        child.setOnLongClickListener(null)
    }

    private fun shouldConsumeTouch(v: View): Boolean {
        return (!workspaceIconsCanBeDragged ||
            !workspaceInModalState && indexOfChild(v) != currentPage)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            mXDown = ev.x
            mYDown = ev.y
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wallpaperReceiver.setWindowToken(windowToken)
        //TODO: Set window token to drag layer
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wallpaperReceiver.setWindowToken(null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        //TODO: Update page alpha values
    }

    override fun getDescendantFocusability(): Int {
        if (workspaceInModalState) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        return super.getDescendantFocusability()
    }

    val workspaceInModalState = false //TODO: Change it with the state of launcher

    val workspaceIconsCanBeDragged = true // TODO: Change with the launcher State

    private fun updateChildrenLayersEnabled() {
        /*val enableChildrenLayers = mIsSwitchingState || isPageInTransition()

        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages()
            } else {
                for (i in 0 until getPageCount()) {
                    val cl: CellLayout = getChildAt(i) as CellLayout
                    cl.enableHardwareLayer(false)
                }
            }
        }*/
    }

    private fun enableHwLayersOnVisiblePages() {
    }

    fun onWallpaperTap(ev: MotionEvent) {
        val position: IntArray = mTempXY
        getLocationOnScreen(position)
        val pointerIndex = ev.actionIndex
        position[0] += ev.getX(pointerIndex).toInt()
        position[1] += ev.getY(pointerIndex).toInt()
        wallpaperManager.sendWallpaperCommand(
            windowToken,
            if (ev.action == MotionEvent.ACTION_UP) WallpaperManager.COMMAND_TAP
            else WallpaperManager.COMMAND_SECONDARY_TAP,
            position[0], position[1], 0, null
        )
    }

    companion object {
        const val DEFAULT_PAGE = 0
        const val EXTRA_EMPTY_SCREEN_ID: Long = -201
    }
}