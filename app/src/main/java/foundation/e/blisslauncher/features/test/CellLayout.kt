package foundation.e.blisslauncher.features.test

import android.R
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.GridLayout
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.launcher.Hotseat
import java.lang.Double.MAX_VALUE
import java.util.Stack

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

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    open fun findNearestArea(
        pixelX: Int,
        pixelY: Int,
        spanX: Int,
        spanY: Int,
        result: IntArray?
    ): IntArray? {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, false, result, null)
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private fun findNearestArea(
        pixelX: Int, pixelY: Int, ignoreOccupied: Boolean, result: IntArray?, resultSpan: IntArray?
    ): IntArray? {
        var pixelX = pixelX
        var pixelY = pixelY
        lazyInitTempRectStack()

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= (mCellWidth * (spanX - 1) / 2f).toInt()
        pixelY -= (mCellHeight * (spanY - 1) / 2f).toInt()

        // Keep track of best-scoring drop area
        val bestXY = result ?: IntArray(2)
        var bestDistance: Double = MAX_VALUE
        val bestRect = Rect(-1, -1, -1, -1)
        val validRegions = Stack<Rect>()
        val countX: Int = mCountX
        val countY: Int = mCountY
        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 || spanX < minSpanX || spanY < minSpanY) {
            return bestXY
        }
        for (y in 0 until countY - (minSpanY - 1)) {
            inner@ for (x in 0 until countX - (minSpanX - 1)) {
                var ySize = -1
                var xSize = -1
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (i in 0 until minSpanX) {
                        for (j in 0 until minSpanY) {
                            if (mOccupied.cells.get(x + i).get(y + j)) {
                                continue@inner
                            }
                        }
                    }
                    xSize = minSpanX
                    ySize = minSpanY

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    var incX = true
                    var hitMaxX = xSize >= spanX
                    var hitMaxY = ySize >= spanY
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (j in 0 until ySize) {
                                if (x + xSize > countX - 1 || mOccupied.cells.get(x + xSize)
                                        .get(y + j)
                                ) {
                                    // We can't move out horizontally
                                    hitMaxX = true
                                }
                            }
                            if (!hitMaxX) {
                                xSize++
                            }
                        } else if (!hitMaxY) {
                            for (i in 0 until xSize) {
                                if (y + ySize > countY - 1 || mOccupied.cells.get(x + i)
                                        .get(y + ySize)
                                ) {
                                    // We can't move out vertically
                                    hitMaxY = true
                                }
                            }
                            if (!hitMaxY) {
                                ySize++
                            }
                        }
                        hitMaxX = hitMaxX or (xSize >= spanX)
                        hitMaxY = hitMaxY or (ySize >= spanY)
                        incX = !incX
                    }
                    incX = true
                    hitMaxX = xSize >= spanX
                    hitMaxY = ySize >= spanY
                }
                val cellXY = mTmpPoint
                cellToCenterPoint(x, y, cellXY)

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                val currentRect: Rect = mTempRectStack.pop()
                currentRect[x, y, x + xSize] = y + ySize
                var contained = false
                for (r in validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true
                        break
                    }
                }
                validRegions.push(currentRect)
                val distance =
                    Math.hypot((cellXY[0] - pixelX).toDouble(), (cellXY[1] - pixelY).toDouble())
                if (distance <= bestDistance && !contained ||
                    currentRect.contains(bestRect)
                ) {
                    bestDistance = distance
                    bestXY[0] = x
                    bestXY[1] = y
                    if (resultSpan != null) {
                        resultSpan[0] = xSize
                        resultSpan[1] = ySize
                    }
                    bestRect.set(currentRect)
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == MAX_VALUE) {
            bestXY[0] = -1
            bestXY[1] = -1
        }
        recycleTempRects(validRegions)
        return bestXY
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
