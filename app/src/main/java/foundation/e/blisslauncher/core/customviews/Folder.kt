package foundation.e.blisslauncher.core.customviews

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.text.InputType
import android.text.Selection
import android.util.AttributeSet
import android.util.Log
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewDebug.ExportedProperty
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.features.folder.FolderAnimationManager
import foundation.e.blisslauncher.features.folder.FolderIcon
import foundation.e.blisslauncher.features.folder.FolderPagerAdapter
import foundation.e.blisslauncher.features.folder.FolderViewPager
import foundation.e.blisslauncher.features.test.Alarm
import foundation.e.blisslauncher.features.test.BaseDragLayer
import foundation.e.blisslauncher.features.test.CellLayout
import foundation.e.blisslauncher.features.test.OnAlarmListener
import foundation.e.blisslauncher.features.test.TestActivity
import foundation.e.blisslauncher.features.test.VariantDeviceProfile
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions
import foundation.e.blisslauncher.features.test.dragndrop.DragSource
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget
import java.util.ArrayList
import kotlinx.android.synthetic.main.activity_test.*
import me.relex.circleindicator.CircleIndicator

class Folder @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractFloatingView(context, attrs), DragController.DragListener,
    FolderTitleInput.OnBackKeyListener, FolderItem.FolderListener, View.OnFocusChangeListener,
    OnEditorActionListener, DragSource {

    /**
     * Fraction of icon width which behave as scroll region.
     */
    private val ICON_OVERSCROLL_WIDTH_FACTOR = 0.45f

    private val FOLDER_NAME_ANIMATION_DURATION = 633L

    private val REORDER_DELAY = 250L
    private val ON_EXIT_CLOSE_DELAY = 400L
    private val sTempRect = Rect()
    private val MIN_FOLDERS_FOR_HARDWARE_OPTIMIZATION = 10

    private val mReorderAlarm: Alarm = Alarm()
    private val mOnExitAlarm: Alarm = Alarm()
    val mItemsInReadingOrder = ArrayList<View>()

    val launcher: TestActivity
    var dragController: DragController? = null
    lateinit var mInfo: FolderItem
    private var mCurrentAnimator: AnimatorSet? = null

    var folderIcon: FolderIcon? = null

    lateinit var mContent: FolderViewPager
    lateinit var mFolderTitleInput: FolderTitleInput
    private lateinit var mPageIndicator: CircleIndicator

    // Cell ranks used for drag and drop
    var mTargetRank = 0
    var mPrevTargetRank = 0
    var mEmptyCellRank = 0

    var mState: Int = STATE_NONE

    private var mRearrangeOnClose = false
    var mItemsInvalidated = false
    private var mCurrentDragView: View? = null
    private var mIsExternalDrag = false
    private var mDragInProgress = false
    private var mDeleteFolderOnDropCompleted = false
    private var mSuppressFolderDeletion = false
    private var mItemAddedBackToSelfViaIcon = false

    var mFolderIconPivotX = 0f

    private var mIsEditingName = false

    @ExportedProperty(category = "launcher")
    private var mDestroyed = false

    init {
        setLocaleDependentFields(resources, false /* force */)
        launcher = TestActivity.getLauncher(context)
        isFocusableInTouchMode = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mContent = findViewById(R.id.folder_apps)

        mPageIndicator = findViewById(R.id.indicator)
        mFolderTitleInput = findViewById(R.id.folder_title)
        mFolderTitleInput.setOnBackKeyListener(this)
        mFolderTitleInput.onFocusChangeListener = this
        mFolderTitleInput.setOnEditorActionListener(this)
        mFolderTitleInput.setSelectAllOnFocus(true)
        mFolderTitleInput.inputType = mFolderTitleInput.inputType and
            InputType.TYPE_TEXT_FLAG_AUTO_CORRECT.inv() and
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS.inv() or
            InputType.TYPE_TEXT_FLAG_CAP_WORDS
        mFolderTitleInput.forceDisableSuggestions(true)
    }

    fun startDrag(v: View, options: DragOptions): Boolean {
        val tag = v.tag
        if (tag is LauncherItem) {
            val item: LauncherItem = tag as LauncherItem
            mEmptyCellRank = item.cell
            mCurrentDragView = v
            dragController!!.addDragListener(this)
            launcher.getLauncherPagedView().beginDragShared(v, this, options)
        }
        return true
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                val dl: DragLayer = launcher.dragLayer
                if (isEditingName()) {
                    if (!dl.isEventOverView(mFolderTitleInput, ev)) {
                        mFolderTitleInput.dispatchBackKey()
                        return true
                    }
                    return false
                } else if (!dl.isEventOverView(this, ev)) {
                    close(true)
                    return true
                }
            }
        }
        return false
    }

    override fun handleClose(animate: Boolean) {
        mIsOpen = false

        if (!animate && mCurrentAnimator != null && mCurrentAnimator!!.isRunning) {
            mCurrentAnimator?.cancel()
        }

        if (isEditingName()) {
            mFolderTitleInput.dispatchBackKey()
        }

        if (folderIcon != null) {
            // mFolderIcon.clearLeaveBehindIfExists()
        }

        if (animate) {
            animateClosed()
        } else {
            closeComplete(false)
        }
    }

    private fun animateClosed() {
        val a = FolderAnimationManager(this, false /* isOpening */).animator
        a.play(ObjectAnimator.ofFloat(launcher.getLauncherPagedView(), View.ALPHA, 1f))
            .with(ObjectAnimator.ofFloat(launcher.hotseat, View.ALPHA, 1f))
            .with(
                ObjectAnimator.ofFloat(
                    launcher.getLauncherPagedView().pageIndicator,
                    View.ALPHA,
                    1f
                )
            )
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                closeComplete(true)
            }
        })
        startAnimation(a)
    }

    private fun closeComplete(wasAnimated: Boolean) {
        // TODO: Clear all active animations.
        (this.parent as DragLayer?)?.removeView(this)
        clearFocus()
        folderIcon?.apply {
            launcher.getLauncherPagedView().alpha = 1f
            launcher.getLauncherPagedView().pageIndicator.alpha = 1f
            launcher.hotseat.alpha = 1f
            if (wasAnimated) {
                if (this.hasDot()) {
                    this.animateDotScale(0f, 1f)
                }
            }
        }

        if (mRearrangeOnClose) {
            rearrangeChildren()
            mRearrangeOnClose = false
        }
        if (getItemCount() <= 1) {
            if (!mDragInProgress && !mSuppressFolderDeletion) {
                replaceFolderWithFinalItem()
            } else if (mDragInProgress) {
                mDeleteFolderOnDropCompleted = true
            }
        }
        mSuppressFolderDeletion = false
        clearDragInfo()
        mState = STATE_NONE
        mContent.currentItem = 0
    }

    override fun isOfType(type: Int): Boolean = type and TYPE_FOLDER != 0

    override fun onBackKey(): Boolean {
        // Convert to a string here to ensure that no other state associated with the text field
        // gets saved.
        val newTitle: String = mFolderTitleInput.text.toString()
        mInfo?.setTitle(newTitle)

        // Update database
        launcher.getLauncherPagedView().updateDatabase()

        // This ensures that focus is gained every time the field is clicked, which selects all
        // the text and brings up the soft keyboard if necessary.
        mFolderTitleInput.clearFocus()

        Selection.setSelection(mFolderTitleInput.text, 0, 0)
        mIsEditingName = false
        return true
    }

    // This is used so the item doesn't immediately appear in the folder when added. In one case
    // we need to create the illusion that the item isn't added back to the folder yet, to
    // to correspond to the animation of the icon back into the folder. This is
    fun hideItem(info: LauncherItem) {
        getViewForInfo(info).apply {
            visibility = INVISIBLE
        }
    }

    fun showItem(info: LauncherItem) {
        getViewForInfo(info)?.apply {
            visibility = VISIBLE
        }
    }

    override fun onAdd(item: LauncherItem) {
    }

    override fun onTitleChanged(title: CharSequence?) {}

    override fun onRemove(item: LauncherItem) {
        mItemsInvalidated = true
        val v: View? = getViewForInfo(item)
        mContent.adapter?.notifyDataSetChanged()
        if (mState == STATE_ANIMATING) {
            mRearrangeOnClose = true
        } else {
            rearrangeChildren()
        }
        if (getItemCount() <= 1) {
            if (mIsOpen) {
                close(true)
            } else {
                replaceFolderWithFinalItem()
            }
        }
    }

    private fun getViewForInfo(item: LauncherItem): View? {
        return mContent.iterateOverItems { info, _, _ -> info === item }
    }

    override fun onItemsChanged(animate: Boolean) {
        updateTextViewFocus()
    }

    override fun onDragStart(dragObject: DropTarget.DragObject, options: DragOptions) {
        if (dragObject.dragSource != this) {
            return
        }

        mContent.removeItem(mCurrentDragView)
        if (dragObject.dragInfo is LauncherItem) {
            mItemsInvalidated = true
            SuppressInfoChanges().use { _ ->
                mInfo.remove(
                    dragObject.dragInfo as LauncherItem,
                    true
                )
            }
        }
        mDragInProgress = true
        mItemAddedBackToSelfViaIcon = false
    }

    override fun onDragEnd() {
        if (mIsExternalDrag && mDragInProgress) {
            completeDragExit()
        }
        mDragInProgress = false
        dragController?.removeDragListener(this)
    }

    fun isEditingName(): Boolean {
        return mIsEditingName
    }

    private fun startEditingFolderName() {
        post {
            mFolderTitleInput.hint = ""
            mIsEditingName = true
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            mFolderTitleInput.dispatchBackKey()
            return true
        }
        return false
    }

    override fun onAttachedToWindow() {
        // requestFocus() causes the focus onto the folder itself, which doesn't cause visual
        // effect but the next arrow key can start the keyboard focus inside of the folder, not
        // the folder itself.
        requestFocus()
        super.onAttachedToWindow()
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean =
        // When the folder gets focus, we don't want to announce the list of items.
        true

    override fun focusSearch(direction: Int): View? =
        // When the folder is focused, further focus search should be within the folder contents.
        FocusFinder.getInstance().findNextFocus(this, null, direction)

    /**
     * @return the FolderInfo object associated with this folder
     */
    fun getInfo(): FolderItem? {
        return mInfo
    }

    fun bind(info: FolderItem) {
        mInfo = info
        val children: MutableList<LauncherItem> = info.items.toMutableList()
        children.sortWith { lhs, rhs ->
            lhs.cell - rhs.cell
        }

        mItemsInvalidated = true
        updateTextViewFocus()
        mInfo.addListener(this)

        mFolderTitleInput.setText(mInfo.title)
        mFolderTitleInput.isCursorVisible = false

        val mDeviceProfile: VariantDeviceProfile = launcher.deviceProfile
        mContent.adapter =
            FolderPagerAdapter(context, mInfo.items, mDeviceProfile)
        // We use same size for height and width as we want to look it like square
        val height =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding)
        mContent.layoutParams?.width =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding) * 2
        mContent.layoutParams?.height =
            (mDeviceProfile.cellHeightPx + mDeviceProfile.iconDrawablePaddingPx * 2) * 3 + resources.getDimensionPixelSize(
                R.dimen.folder_padding
            ) * 2
        mPageIndicator.setViewPager(mContent)

        // In case any children didn't come across during loading, clean up the folder accordingly
        folderIcon?.post {
            if (getItemCount() <= 1) {
                replaceFolderWithFinalItem()
            }
        }
    }

    private fun startAnimation(a: AnimatorSet) {
        if (mCurrentAnimator != null && mCurrentAnimator!!.isRunning) {
            mCurrentAnimator?.cancel()
        }
        val workspace: LauncherPagedView = launcher.getLauncherPagedView()
        val currentCellLayout: CellLayout =
            workspace.getChildAt(workspace.currentPage) as CellLayout
        val useHardware = shouldUseHardwareLayerForAnimation(currentCellLayout)
        val wasHardwareAccelerated: Boolean = currentCellLayout.isHardwareLayerEnabled()
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                if (useHardware) {
                    currentCellLayout.enableHardwareLayer(true)
                }
                mState = STATE_ANIMATING
                mCurrentAnimator = a
            }

            override fun onAnimationEnd(animation: Animator) {
                if (useHardware) {
                    currentCellLayout.enableHardwareLayer(wasHardwareAccelerated)
                }
                mCurrentAnimator = null
            }
        })
        a.start()
    }

    private fun shouldUseHardwareLayerForAnimation(currentCellLayout: CellLayout): Boolean {
        var folderCount = 0
        for (i in currentCellLayout.childCount - 1 downTo 0) {
            val child: View = currentCellLayout.getChildAt(i)
            if (child is FolderIcon) ++folderCount
        }
        return folderCount >= MIN_FOLDERS_FOR_HARDWARE_OPTIMIZATION
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     */
    fun animateOpen() {
        val openFolder = getOpen(launcher)
        if (openFolder != null && openFolder !== this) {
            // Close any open folder before opening a folder.
            openFolder.close(true)
        }
        mIsOpen = true
        val dragLayer = launcher.dragLayer
        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (parent == null) {
            dragLayer.addView(this, BaseDragLayer.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            Log.e(
                TAG,
                "Opening folder (" + this + ") which already has a parent:" +
                    parent
            )
        }
        // mContent.completePendingPageChanges()
        if (!mDragInProgress) {
            // Open on the first page.
            mContent.currentItem = 0
        }

        // This is set to true in close(), but isn't reset to false until onDropCompleted(). This
        // leads to an inconsistent state if you drag out of the folder and drag back in without
        // dropping. One resulting issue is that replaceFolderWithFinalItem() can be called twice.
        mDeleteFolderOnDropCompleted = false
        // centerAboutIcon()
        val anim: AnimatorSet = FolderAnimationManager(this, true /* isOpening */).animator
        anim.play(ObjectAnimator.ofFloat(launcher.getLauncherPagedView(), View.ALPHA, 0f))
            .with(ObjectAnimator.ofFloat(launcher.hotseat, View.ALPHA, 0f))
            .with(
                ObjectAnimator.ofFloat(
                    launcher.getLauncherPagedView().pageIndicator,
                    View.ALPHA,
                    0f
                )
            )
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                mState = STATE_OPEN
                launcher.getLauncherPagedView().alpha = 0f
                launcher.hotseat.alpha = 0f
                launcher.getLauncherPagedView().pageIndicator.alpha = 0f
            }

            override fun onAnimationCancel(animation: Animator?) {
                launcher.getLauncherPagedView().alpha = 1f
                launcher.hotseat.alpha = 1f
                launcher.getLauncherPagedView().pageIndicator.alpha = 1f
            }
        })
        startAnimation(anim)

        // Make sure the folder picks up the last drag move even if the finger doesn't move.
        if (dragController!!.isDragging) {
            dragController!!.forceTouchMove()
        }
    }

    fun completeDragExit() {
        if (mIsOpen) {
            close(true)
            mRearrangeOnClose = true
        } else if (mState == STATE_ANIMATING) {
            mRearrangeOnClose = true
        } else {
            rearrangeChildren()
            clearDragInfo()
        }
    }

    private fun clearDragInfo() {
        mCurrentDragView = null
        mIsExternalDrag = false
    }

    /**
     * Rearranges the children based on their rank.
     */
    fun rearrangeChildren() {
        rearrangeChildren(-1)
    }

    /**
     * Rearranges the children based on their rank.
     * @param itemCount if greater than the total children count, empty spaces are left at the end,
     * otherwise it is ignored.
     */
    fun rearrangeChildren(itemCount: Int) {
        mContent.adapter?.notifyDataSetChanged()
        mItemsInvalidated = true
    }

    fun isDestroyed(): Boolean {
        return mDestroyed
    }

    private fun replaceFolderWithFinalItem() {
        // Add the last remaining child to the workspace in place of the folder
        val onCompleteRunnable = Runnable {
            val itemCount: Int = mInfo.items.size
            if (itemCount <= 1) {
                var finalItem: LauncherItem? = null
                if (itemCount == 1) {
                    // Move the item from the folder to the workspace, in the position of the
                    // folder
                    finalItem = mInfo.items.removeAt(0)
                    finalItem?.apply {
                        cell = mInfo.cell
                        screenId = mInfo.screenId
                        container = mInfo.container
                    }
                }

                // Remove the folder
                launcher.getLauncherPagedView().removeItem(folderIcon, mInfo /* deleteFromDb */)
                if (finalItem != null) {
                    // We add the child after removing the folder to prevent both from existing
                    // at the same time in the CellLayout.  We need to add the new item with
                    // addInScreenFromBind() to ensure that hotseat items are placed correctly.
                    launcher.getLauncherPagedView().bindItems(listOf(finalItem), true)
                }
                launcher.getLauncherPagedView().updateDatabase()
            }
        }
        onCompleteRunnable.run()
        mDestroyed = true
    }

    fun getItemCount(): Int {
        Log.i(TAG, "getItemCount: " + mContent.getItemCount() + " " + mInfo.items.size)
        return mInfo.items.size
    }

    // This method keeps track of the first and last item in the folder for the purposes
    // of keyboard focus
    fun updateTextViewFocus() {
        val firstChild: View? = mContent.getFirstItem()
        val lastChild: View? = mContent.getLastItem()
        if (firstChild != null && lastChild != null) {
            mFolderTitleInput.nextFocusDownId = lastChild.id
            mFolderTitleInput.nextFocusRightId = lastChild.id
            mFolderTitleInput.nextFocusLeftId = lastChild.id
            mFolderTitleInput.nextFocusUpId = lastChild.id
            // Hitting TAB from the folder name wraps around to the first item on the current
            // folder page, and hitting SHIFT+TAB from that item wraps back to the folder name.
            mFolderTitleInput.nextFocusForwardId = firstChild.id
            // When clicking off the folder when editing the name, this Folder gains focus. When
            // pressing an arrow key from that state, give the focus to the first item.
            this.nextFocusDownId = firstChild.id
            this.nextFocusRightId = firstChild.id
            this.nextFocusLeftId = firstChild.id
            this.nextFocusUpId = firstChild.id
            // When pressing shift+tab in the above state, give the focus to the last item.
            setOnKeyListener { _, keyCode, event ->
                val isShiftPlusTab = keyCode == KeyEvent.KEYCODE_TAB &&
                    event.hasModifiers(KeyEvent.META_SHIFT_ON)
                if (isShiftPlusTab && this@Folder.isFocused) {
                    lastChild.requestFocus()
                } else false
            }
        }
    }

    var mOnExitAlarmListener: OnAlarmListener = OnAlarmListener { completeDragExit() }

    override fun onDropCompleted(target: View?, d: DropTarget.DragObject?, success: Boolean) {
    }

    override fun onBackPressed(): Boolean {
        if (isEditingName()) {
            mFolderTitleInput.dispatchBackKey()
        } else {
            super.onBackPressed()
        }
        return true
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v === mFolderTitleInput) {
            if (hasFocus) {
                startEditingFolderName()
            } else {
                mFolderTitleInput.dispatchBackKey()
            }
        }
    }

    fun getItemsInReadingOrder(): ArrayList<View> {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear()
            mContent.iterateOverItems { _, view, _ ->
                mItemsInReadingOrder.add(view)
                false
            }
            mItemsInvalidated = false
        }
        return mItemsInReadingOrder
    }

    companion object {
        const val STATE_NONE = -1
        const val STATE_ANIMATING = 1
        const val STATE_OPEN = 2

        const val TAG = "Folder"

        private var sDefaultFolderName: String? = null

        /**
         * Creates a new UserFolder, inflated from R.layout.user_folder.
         *
         * @param launcher The main activity.
         *
         * @return A new UserFolder.
         */
        @SuppressLint("InflateParams")
        fun fromXml(launcher: TestActivity): Folder {
            return launcher.layoutInflater.inflate(R.layout.layout_folder, null) as Folder
        }

        /**
         * Returns a folder which is already open or null
         */
        fun getOpen(launcher: TestActivity?): Folder? {
            return getOpenView(launcher, TYPE_FOLDER)
        }

        fun setLocaleDependentFields(res: Resources, force: Boolean) {
            if (sDefaultFolderName == null || force) {
                sDefaultFolderName = res.getString(R.string.untitled)
            }
        }
    }

    /**
     * Temporary resource held while we don't want to handle info changes
     */
    inner class SuppressInfoChanges internal constructor() : AutoCloseable {
        override fun close() {
            mInfo.addListener(this@Folder)
            updateTextViewFocus()
        }

        init {
            mInfo.removeListener(this@Folder)
        }
    }
}
