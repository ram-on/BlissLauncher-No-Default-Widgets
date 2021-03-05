package foundation.e.blisslauncher.features.test

import android.R
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.GridLayout
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.launcher.Hotseat

open class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    private var mIsDragOverlapping: Boolean = false

    // When a drag operation is in progress, holds the nearest cell to the touch point
    private val mDragCell = IntArray(2)
    private val BACKGROUND_STATE_ACTIVE = intArrayOf(R.attr.state_active)
    private val BACKGROUND_STATE_DEFAULT = EMPTY_STATE_SET
    private var mDragging: Boolean = false

    private var mDropPending = false

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    val mTmpPoint = IntArray(2)
    val mTempLocation = IntArray(2)

    private val TAG = "CellLayout"

    private val launcher: TestActivity = TestActivity.getLauncher(context)
    private val dp = launcher.deviceProfile
    val countX = dp.inv.numColumns
    val countY = dp.inv.numRows

    open var containerType = Constants.CONTAINER_DESKTOP

    private var cellWidth: Int = 0
    private var cellHeight: Int = 0

    init {
        setWillNotDraw(false)
        clipToPadding = false
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        Log.d(
            TAG,
            "$this onMeasure() called with: widthSpec = $widthSpec, heightSpec = $heightSpec"
        )
        val widthSpecMode = MeasureSpec.getMode(widthSpec)
        val heightSpecMode = MeasureSpec.getMode(heightSpec)
        val widthSize = MeasureSpec.getSize(widthSpec)
        val heightSize = MeasureSpec.getSize(heightSpec)
        val childWidthSize = widthSize - (paddingLeft + paddingRight)
        val childHeightSize = heightSize - (paddingTop + paddingBottom)
        val hotseat = parent is Hotseat
        Log.i("CellLayout", "$hotseat $heightSize $paddingTop $paddingBottom")
        cellWidth = VariantDeviceProfile.calculateCellWidth(childWidthSize, countX)
        cellHeight = VariantDeviceProfile.calculateCellHeight(childHeightSize, countY)
        Log.d(TAG, "cellWidth: $cellWidth")
        setMeasuredDimension(widthSize, heightSize)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChild(child)
            }
        }
    }

    open fun getCellContentHeight(): Int {
        return Math.min(
            measuredHeight,
            launcher.deviceProfile
                .getCellHeight(if (parent is Hotseat) Constants.CONTAINER_HOTSEAT else Constants.CONTAINER_DESKTOP)
        )
    }

    private fun measureChild(child: View) {
        val lp = child.layoutParams as LayoutParams
        lp.rowSpec = spec(UNDEFINED)
        lp.columnSpec = spec(UNDEFINED)
        lp.width = cellWidth
        lp.height = getCellContentHeight()
        // Center the icon/folder
        val cHeight: Int = dp.cellHeightPx
        Log.i("CellLayout", "Height: ${lp.height}, cellHeight: $cHeight")
        val cellPaddingY = 0f.coerceAtLeast((lp.height - cHeight) / 2f).toInt()
        var cellPaddingX: Int
        if (containerType == Constants.CONTAINER_DESKTOP) {
            cellPaddingX = dp.workspaceCellPaddingXPx
        } else {
            cellPaddingX = (dp.edgeMarginPx / 2f).toInt()
        }
        child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0)
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        Log.d(TAG, "onViewAdded() called with: child = $child")
    }

    fun addViewToCellLayout(child: View, index: Int, childId: Int, params: LayoutParams) {
    }

    open fun setDropPending(pending: Boolean) {
        mDropPending = pending
    }

    open fun isDropPending(): Boolean {
        return mDropPending
    }

    open fun setIsDragOverlapping(isDragOverlapping: Boolean) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping
            //mBackground.setState(if (mIsDragOverlapping) BACKGROUND_STATE_ACTIVE else BACKGROUND_STATE_DEFAULT)
            invalidate()
        }
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    open fun onDragEnter() {
        mDragging = true
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    open fun onDragExit() {
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false
        }

        // Invalidate the drag data
        mDragCell[1] = -1
        mDragCell[0] = -1
        /* mDragOutlineAnims.get(mDragOutlineCurrent).animateOut()
         mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.size
         revertTempState()*/
        setIsDragOverlapping(false)
    }

    fun revertTempState() {
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    fun onDropChild(child: View) {
        if (child != null) {
            val lp: LayoutParams =
                child.layoutParams as LayoutParams
            child.requestLayout()
            //markCellsAsOccupiedForView(child)
        }
    }

    // This class stores info for two purposes:
    // 1. When dragging items (mDragInfo in Workspace), we store the View, its cellX & cellY,
    //    its spanX, spanY, and the screen it is on
    // 2. When long clicking on an empty cell in a CellLayout, we save information about the
    //    cellX and cellY coordinates and which page was clicked. We then set this as a tag on
    //    the CellLayout that was long clicked
    class CellInfo(v: View, info: LauncherItem) {
        val cell: View
        val screenId: Long
        val container: Long
        val rank: Int
        override fun toString(): String {
            return ("Cell[view=${cell.javaClass}, rank=$rank")
        }

        init {
            cell = v
            rank = info.cell
            screenId = info.screenId
            container = info.container
        }
    }
}
