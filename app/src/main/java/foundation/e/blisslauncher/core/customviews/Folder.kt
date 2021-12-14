package foundation.e.blisslauncher.core.customviews

import android.animation.AnimatorSet
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.text.InputType
import android.text.Selection
import android.util.AttributeSet
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewDebug.ExportedProperty
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.viewpager.widget.ViewPager
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.DeviceProfile
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.features.folder.FolderPagerAdapter
import foundation.e.blisslauncher.features.test.Alarm
import foundation.e.blisslauncher.features.test.IconTextView
import foundation.e.blisslauncher.features.test.OnAlarmListener
import foundation.e.blisslauncher.features.test.TestActivity
import foundation.e.blisslauncher.features.test.VariantDeviceProfile
import foundation.e.blisslauncher.features.test.dragndrop.DragController
import foundation.e.blisslauncher.features.test.dragndrop.DragLayer
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions
import foundation.e.blisslauncher.features.test.dragndrop.DragSource
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget
import me.relex.circleindicator.CircleIndicator
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

class Folder @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbstractFloatingView(context, attrs), DropTarget, DragController.DragListener,
    FolderTitleInput.OnBackKeyListener, FolderItem.FolderListener,
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

    private var sDefaultFolderName: String? = null
    private val mReorderAlarm: Alarm = Alarm()
    private val mOnExitAlarm: Alarm = Alarm()
    val mItemsInReadingOrder = ArrayList<View>()

    protected val mLauncher: TestActivity
    protected var mDragController: DragController? = null
    lateinit var mInfo: FolderItem
    private val mCurrentAnimator: AnimatorSet? = null

    var mFolderIcon: IconTextView? = null

    lateinit var mContent: ViewPager
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
    private val mDeleteFolderOnDropCompleted = false
    private val mSuppressFolderDeletion = false
    private var mItemAddedBackToSelfViaIcon = false

    var mFolderIconPivotX = 0f

    private var mIsEditingName = false

    @ExportedProperty(category = "launcher")
    private val mDestroyed = false

    init {
        setLocaleDependentFields(resources, false /* force */)
        mLauncher = TestActivity.getLauncher(context)
        isFocusableInTouchMode = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mContent = findViewById(R.id.folder_apps)
        mContent.setFolder(this)

        mPageIndicator = findViewById(R.id.indicator)
        mFolderTitleInput = findViewById(R.id.folder_title)
        mFolderTitleInput.setOnBackKeyListener(this)
        mFolderTitleInput.setOnFocusChangeListener(this)
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
            mDragController!!.addDragListener(this)
            mLauncher.getLauncherPagedView().beginDragShared(v, this, options)
        }
        return true
    }

    fun setLocaleDependentFields(res: Resources, force: Boolean) {
        if (sDefaultFolderName == null || force) {
            sDefaultFolderName = res.getString(R.string.untitled)
        }
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleClose(animate: Boolean) {
    }

    override fun isOfType(type: Int): Boolean = type and TYPE_FOLDER != 0

    override fun onBackKey(): Boolean {
        // Convert to a string here to ensure that no other state associated with the text field
        // gets saved.
        val newTitle: String = mFolderTitleInput.text.toString()
        mInfo?.setTitle(newTitle)

        // Update database
        mLauncher.getLauncherPagedView().updateDatabase()

        // This ensures that focus is gained every time the field is clicked, which selects all
        // the text and brings up the soft keyboard if necessary.
        mFolderTitleInput.clearFocus()

        Selection.setSelection(mFolderTitleInput.getText(), 0, 0)
        mIsEditingName = false
        return true
    }

    override fun onTitleChanged(title: CharSequence?) {
        TODO("Not yet implemented")
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
                    dragObject.dragInfo as WorkspaceItemInfo,
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
        mDragController?.removeDragListener(this)
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

    fun getFolderIcon() = mFolderIcon

    fun setFolderIcon(icon: IconTextView) {
        mFolderIcon = icon
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

        val mDeviceProfile: VariantDeviceProfile = mLauncher.deviceProfile
        mContent?.adapter =
            FolderPagerAdapter(context, mInfo.items, mDeviceProfile)
        // We use same size for height and width as we want to look it like sqaure
        val height =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding)
        mContent?.layoutParams?.width =
            mDeviceProfile.cellHeightPx * 3 + resources.getDimensionPixelSize(R.dimen.folder_padding) * 2
        mContent?.layoutParams?.height =
            (mDeviceProfile.cellHeightPx + mDeviceProfile.iconDrawablePaddingPx * 2) * 3 + resources.getDimensionPixelSize(
                R.dimen.folder_padding
            ) * 2
        mPageIndicator.setViewPager(mContent)

        // In case any children didn't come across during loading, clean up the folder accordingly
        mFolderIcon?.post {
            if (getItemCount() <= 1) {
                replaceFolderWithFinalItem()
            }
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
        val views: ArrayList<View> = getItemsInReadingOrder()
        mContent.arrangeChildren(views, Math.max(itemCount, views.size))
        mItemsInvalidated = true
    }

    fun getItemCount(): Int {
        return mContent.getItemCount()
    }

    override fun isDropEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDrop(dragObject: DropTarget.DragObject?, options: DragOptions?) {
        TODO("Not yet implemented")
    }

    override fun onDragEnter(dragObject: DropTarget.DragObject?) {
        TODO("Not yet implemented")
    }

    override fun onDragOver(dragObject: DropTarget.DragObject?) {
        TODO("Not yet implemented")
    }

    var mOnExitAlarmListener: OnAlarmListener = OnAlarmListener { completeDragExit() }

    override fun onDragExit(d: DropTarget.DragObject) {
        // We only close the folder if this is a true drag exit, ie. not because
        // a drop has occurred above the folder.
        if (!d.dragComplete) {
            mOnExitAlarm.setOnAlarmListener(mOnExitAlarmListener)
            mOnExitAlarm.setAlarm(ON_EXIT_CLOSE_DELAY)
        }
        mReorderAlarm.cancelAlarm()
    }

    override fun acceptDrop(dragObject: DropTarget.DragObject?): Boolean {
        TODO("Not yet implemented")
    }

    override fun prepareAccessibilityDrop() {
        TODO("Not yet implemented")
    }

    override fun onDropCompleted(target: View?, d: DropTarget.DragObject?, success: Boolean) {
    }

    override fun getHitRectRelativeToDragLayer(outRect: Rect?) {
        TODO("Not yet implemented")
    }

    companion object {
        const val STATE_NONE = -1
        const val STATE_SMALL = 0
        const val STATE_ANIMATING = 1
        const val STATE_OPEN = 2
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