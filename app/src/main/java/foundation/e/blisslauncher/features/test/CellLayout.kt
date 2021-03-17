package foundation.e.blisslauncher.features.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.ArrayMap
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewDebug.ExportedProperty
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.IntDef
import foundation.e.blisslauncher.R
import foundation.e.blisslauncher.core.Utilities
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.launcher.Hotseat
import foundation.e.blisslauncher.features.test.anim.Interpolators
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget
import foundation.e.blisslauncher.features.test.graphics.DragPreviewProvider
import java.lang.Double.MAX_VALUE
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.ArrayList
import java.util.Arrays
import java.util.Stack
import kotlin.math.hypot

open class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    private var mItemPlacementDirty: Boolean = false
    private var mIsDragOverlapping: Boolean = false

    // When a drag operation is in progress, holds the nearest cell to the touch point
    private val mDragCell = IntArray(2)
    private val BACKGROUND_STATE_ACTIVE = intArrayOf(android.R.attr.state_active)
    private val BACKGROUND_STATE_DEFAULT = EMPTY_STATE_SET
    private var mDragging: Boolean = false

    private val DESTRUCTIVE_REORDER = false
    private val DEBUG_VISUALIZE_OCCUPIED = false

    private var mDropPending = false

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    val mTmpPoint = IntArray(2)
    val mTempLocation = IntArray(2)

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(WORKSPACE, HOTSEAT)
    annotation class ContainerType

    @ContainerType
    private var mContainerType = 0

    private val TAG = "CellLayout"

    private val launcher: TestActivity = TestActivity.getLauncher(context)
    private val dp = launcher.deviceProfile
    val mCountX = dp.inv.numColumns
    val mCountY = dp.inv.numRows
    val mOccupied = GridOccupancy(mCountX, mCountY)
    val mTmpOccupied = GridOccupancy(mCountX, mCountY)

    private val mIntersectingViews = ArrayList<View>()

    // These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    val mDragOutlines = arrayOfNulls<Rect>(4)

    val mDragOutlineAlphas = FloatArray(mDragOutlines.size)
    private val mDragOutlineAnims: Array<InterruptibleInOutAnimator?> =
        arrayOfNulls<InterruptibleInOutAnimator>(mDragOutlines.size)

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private var mDragOutlineCurrent = 0
    private val mDragOutlinePaint = Paint()
    private var mEaseOutInterpolator: TimeInterpolator? = null

    open var containerType = Constants.CONTAINER_DESKTOP

    private var cellWidth: Int = 0
    private var cellHeight: Int = 0

    companion object {
        const val MODE_SHOW_REORDER_HINT = 0
        const val MODE_DRAG_OVER = 1
        const val MODE_ON_DROP = 2
        const val MODE_ON_DROP_EXTERNAL = 3
        const val MODE_ACCEPT_DROP = 4

        const val WORKSPACE = 0
        const val HOTSEAT = 1
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyleAttr, 0)
        mContainerType = a.getInteger(R.styleable.CellLayout_containerType, CellLayout.WORKSPACE)
        a.recycle()

        setWillNotDraw(false)
        clipToPadding = false

        // Initialize the data structures used for the drag visualization.

        // Initialize the data structures used for the drag visualization.
        mEaseOutInterpolator = Interpolators.DEACCEL_2_5 // Quint ease out

        for (i in mDragOutlines.indices) {
            mDragOutlines[i] = Rect(-1, -1, -1, -1)
        }
        mDragOutlinePaint.color = Color.RED

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.
        val duration: Int = 900
        val fromAlphaValue = 0f
        val toAlphaValue = 128f as Float

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue)

        for (i in mDragOutlineAnims.indices) {
            val anim = InterruptibleInOutAnimator(
                this,
                duration.toLong(), fromAlphaValue, toAlphaValue
            )
            anim.animator.interpolator = mEaseOutInterpolator
            anim.animator.addUpdateListener { animation ->
                val outline = anim.tag as Bitmap?

                // If an animation is started and then stopped very quickly, we can still
                // get spurious updates we've cleared the tag. Guard against this.
                if (outline == null) {

                    val `val` = animation.animatedValue
                    Log.d(
                        TAG, "anim " + i + " update: " + `val` +
                            ", isStopped " + anim.isStopped
                    )
                    // Try to prevent it from continuing to run
                    animation.cancel()
                } else {
                    mDragOutlineAlphas[i] = animation.animatedValue as Float
                    this@CellLayout.invalidate(mDragOutlines[i])
                }
            }
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            anim.animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if ((animation as ValueAnimator).animatedValue as Float == 0f) {
                        anim.tag = null
                    }
                }
            })
            mDragOutlineAnims[i] = anim
        }
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
        cellWidth = VariantDeviceProfile.calculateCellWidth(childWidthSize, mCountX)
        cellHeight = VariantDeviceProfile.calculateCellHeight(childHeightSize, mCountY)
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

    fun measureChild(child: View) {
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

    fun addViewToCellLayout(
        child: View,
        index: Int,
        childId: Int,
        params: LayoutParams,
        markCells: Boolean
    ): Boolean {
        val lp: LayoutParams = params

        // Hotseat icons - remove text
        if (child is IconTextView) {
            val bubbleChild: IconTextView = child
            bubbleChild.setTextVisibility(mContainerType != HOTSEAT)
        }

        child.scaleX = 1f
        child.scaleY = 1f

        Log.d(TAG, "Adding view at index: ")

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (index >= 0 && index <= mCountX * mCountY - 1) {

            addView(child, index, lp)

            //if (markCells) markCellsAsOccupiedForView(child)
            return true
        }
        return false
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
        mDragOutlineAnims[mDragOutlineCurrent]?.animateOut()
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.size
        revertTempState()
        setIsDragOverlapping(false)
    }

    fun revertTempState() {
/*        completeAndClearReorderPreviewAnimations()
        if (isItemPlacementDirty()) {
            val count: Int = getChildCount()
            for (i in 0 until count) {
                val child: View = getChildAt(i)
                val lp: com.android.launcher3.CellLayout.LayoutParams =
                    child.layoutParams as com.android.launcher3.CellLayout.LayoutParams
                if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                    lp.tmpCellX = lp.cellX
                    lp.tmpCellY = lp.cellY
                    animateChildToPosition(
                        child, lp.cellX, lp.cellY, CellLayout.REORDER_ANIMATION_DURATION,
                        0, false, false
                    )
                }
            }
            setItemPlacementDirty(false)
        }*/
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
            lp.dropped = true
            child.requestLayout()
            markCellsAsOccupiedForView(child)
        }
    }

    open fun visualizeDropLocation(
        v: View?,
        outlineProvider: DragPreviewProvider?,
        cellX: Int,
        cellY: Int,
        resize: Boolean,
        dragObject: DropTarget.DragObject
    ) {
        val oldDragCellX = mDragCell[0]
        val oldDragCellY = mDragCell[1]
        if (outlineProvider?.generatedDragOutline == null) {
            return
        }
        val dragOutline: Bitmap = outlineProvider.generatedDragOutline
        if (cellX != oldDragCellX || cellY != oldDragCellY) {
            val dragOffset: Point = dragObject.dragView.dragVisualizeOffset
            val dragRegion: Rect = dragObject.dragView.dragRegion
            mDragCell[0] = cellX
            mDragCell[1] = cellY
            val oldIndex: Int = mDragOutlineCurrent
            mDragOutlineAnims[oldIndex]?.animateOut()
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.size
            val r: Rect = mDragOutlines[mDragOutlineCurrent]!!

            // Find the top left corner of the rect the object will occupy
            val topLeft = mTmpPoint
            cellToPoint(cellX, cellY, topLeft)
            var left = topLeft[0]
            var top = topLeft[1]
            if (v != null && dragOffset == null) {
                // When drawing the drag outline, it did not account for margin offsets
                // added by the view's parent.
                val lp = v.layoutParams as MarginLayoutParams
                left += lp.leftMargin
                top += lp.topMargin

                // Offsets due to the size difference between the View and the dragOutline.
                // There is a size difference to account for the outer blur, which may lie
                // outside the bounds of the view.
                top += (cellHeight * 1 - dragOutline.height) / 2
                // We center about the x axis
                left += (cellWidth * 1 - dragOutline.width) / 2
            } else {
                if (dragOffset != null && dragRegion != null) {
                    // Center the drag region *horizontally* in the cell and apply a drag
                    // outline offset
                    left += dragOffset.x + (cellWidth * 1 - dragRegion.width()) / 2
                    val cHeight: Int = getCellContentHeight()
                    val cellPaddingY =
                        Math.max(0f, (cellHeight - cHeight) / 2f).toInt()
                    top += dragOffset.y + cellPaddingY
                } else {
                    // Center the drag outline in the cell
                    left += (cellWidth * 1 - dragOutline.width) / 2
                    top += (cellHeight * 1 - dragOutline.height) / 2
                }
            }
            r[left, top, left + dragOutline.width] = top + dragOutline.height
            Utilities.scaleRectAboutCenter(r, 1f)
            mDragOutlineAnims[mDragOutlineCurrent]?.setTag(dragOutline)
            mDragOutlineAnims[mDragOutlineCurrent]?.animateIn()
        }
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    open fun cellToPoint(cellX: Int, cellY: Int, result: IntArray) {
        val hStartPadding = paddingLeft
        val vStartPadding = paddingTop
        result[0] = hStartPadding + cellX * cellWidth
        result[1] = vStartPadding + cellY * cellHeight
    }

    fun getDistanceFromCell(x: Float, y: Float, cell: IntArray): Float {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint)
        return Math.hypot((x - mTmpPoint[0]).toDouble(), (y - mTmpPoint[1]).toDouble())
            .toFloat()
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    private fun getViewsIntersectingRegion(
        cellX: Int,
        cellY: Int,
        dragView: View,
        boundingRect: Rect?,
        intersectingViews: ArrayList<View>
    ) {
        boundingRect?.set(cellX, cellY, cellX + 1, cellY + 1)
        intersectingViews.clear()
        val r0 = Rect(cellX, cellY, cellX + 1, cellY + 1)
        val r1 = Rect()
        val count: Int = childCount
        for (i in 0 until count) {
            val child: View = getChildAt(i)
            if (child === dragView) continue
            r1[i % mCountX, i % mCountY, i % mCountX + 1] = i % mCountY + 1
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child)
                boundingRect?.union(r1)
            }
        }
    }

    open fun isNearestDropLocationOccupied(
        pixelX: Int,
        pixelY: Int,
        dragView: View,
        result: IntArray?
    ): Boolean {
        var result = result
        result = findNearestArea(pixelX, pixelY, result)
        getViewsIntersectingRegion(
            result!![0], result!![1], dragView, null,
            mIntersectingViews
        )
        return !mIntersectingViews.isEmpty()
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
        result: IntArray?
    ): IntArray? {
        return findNearestArea(pixelX, pixelY, false, result, null)
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    private fun findNearestArea(
        pixelX: Int,
        pixelY: Int,
        ignoreOccupied: Boolean,
        result: IntArray?,
        resultSpan: IntArray?
    ): IntArray? {

        var pixelX = pixelX
        var pixelY = pixelY
        lazyInitTempRectStack()

        // Keep track of best-scoring drop area
        val bestXY = result ?: IntArray(2)
        var bestDistance: Double = MAX_VALUE
        val bestRect = Rect(-1, -1, -1, -1)
        val validRegions = Stack<Rect>()
        val countX: Int = mCountX
        val countY: Int = mCountY

        val minSpanX = 1
        val minSpanY = 1
        val spanX = 1
        val spanY = 1

        for (y in 0 until countY - (minSpanY - 1)) {
            inner@ for (x in 0 until countX - (minSpanX - 1)) {
                var ySize = -1
                var xSize = -1
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (i in 0 until minSpanX) {
                        for (j in 0 until minSpanY) {
                            if (mOccupied.cells[x + i][y + j]) {
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
                                if (x + xSize > countX - 1 || mOccupied.cells[x + xSize][y + j]
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
                                if (y + ySize > countY - 1 || mOccupied.cells[x + i][y + ySize]
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
                    hypot((cellXY[0] - pixelX).toDouble(), (cellXY[1] - pixelY).toDouble())
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

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    open fun cellToCenterPoint(cellX: Int, cellY: Int, result: IntArray) {
        regionToCenterPoint(cellX, cellY, 1, 1, result)
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    open fun regionToCenterPoint(cellX: Int, cellY: Int, spanX: Int, spanY: Int, result: IntArray) {
        val hStartPadding = paddingLeft
        val vStartPadding = paddingTop
        result[0] = hStartPadding + cellX * cellWidth + spanX * cellWidth / 2
        result[1] = vStartPadding + cellY * cellHeight + spanY * cellHeight / 2
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
     * @param result Array in which to place the result, or null (in which case a new array will
     * be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     * nearest the requested location.
     */
    open fun findNearestVacantArea(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        result: IntArray?,
        resultSpan: IntArray?
    ): IntArray? {
        return findNearestArea(
            pixelX, pixelY, true,
            result, resultSpan
        )
    }

    private val mTempRectStack = Stack<Rect>()
    private fun lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (i in 0 until mCountX * mCountY) {
                mTempRectStack.push(Rect())
            }
        }
    }

    private fun recycleTempRects(used: Stack<Rect>) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop())
        }
    }

    open fun performReorder(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        result: IntArray?,
        resultSpan: IntArray?,
        mode: Int
    ): IntArray? {

        // First we determine if things have moved enough to cause a different layout
        var result = result
        var resultSpan = resultSpan
        result = findNearestArea(pixelX, pixelY, result)
        Log.d(
            TAG,
            "performReorder() called with: resultX = ${result!![0]} ${result!![1]} mode = $mode"
        )
        if (resultSpan == null) {
            resultSpan = IntArray(2)
        }

        resultSpan[0] = 1
        resultSpan[1] = 1

        // We don't need to find for nearest area when the grid is already totally occupied.

        // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
        // committing anything or animating anything as we just want to determine if a solution
        // exists
        if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
            val parent = (dragView?.parent as ViewGroup?)
            parent?.removeView(dragView)
            if (childCount == mCountX * mCountY) {
                return intArrayOf(-1, -1)
            }

            val index = result[1] * mCountX + result[0]
            addView(dragView, index)

            /*copySolutionToTempState(finalSolution, dragView)
            setItemPlacementDirty(true)
            animateItemsToSolution(finalSolution, dragView!!, mode == MODE_ON_DROP)
            if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                commitTempPlacement()
                completeAndClearReorderPreviewAnimations()
                setItemPlacementDirty(false)
            } else {
                beginOrAdjustReorderPreviewAnimations(
                    finalSolution,
                    dragView!!,
                    CellLayout.REORDER_ANIMATION_DURATION,
                    ReorderPreviewAnimation.MODE_PREVIEW
                )
            }*/
            Log.d(TAG, "Add view")
            //(dragView!!.parent as ViewGroup).removeView(dragView)
        }

        // May consider this later.
        /*if ((mode == MODE_ON_DROP || !foundSolution)) {
            setUseTempCoords(false)
        }*/
        requestLayout()
        return result
    }

    private fun findConfigurationNoShuffle(
        pixelX: Int,
        pixelY: Int,
        minSpanX: Int,
        minSpanY: Int,
        spanX: Int,
        spanY: Int,
        dragView: View?,
        solution: ItemConfiguration
    ): ItemConfiguration {
        val result = IntArray(2)
        val resultSpan = IntArray(2)
        findNearestVacantArea(
            pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, result,
            resultSpan
        )
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false)
            solution.cellX = result[0]
            solution.cellY = result[1]
            solution.isSolution = true
        } else {
            solution.isSolution = false
        }
        return solution
    }

    private fun copyCurrentStateToSolution(solution: ItemConfiguration, temp: Boolean) {
        /*val childCount: Int = childCount
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            val lp: LayoutParams =
                child.layoutParams as GridLayout.LayoutParams
            var c: CellAndSpan?
            if (temp) {
                c = CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan)
            } else {
                c = CellAndSpan(, lp.cellY, lp.cellHSpan, lp.cellVSpan)
            }
            solution.add(child, c!!)
        }*/
    }

    open fun setItemPlacementDirty(dirty: Boolean) {
        mItemPlacementDirty = dirty
    }

    open fun isItemPlacementDirty(): Boolean {
        return mItemPlacementDirty
    }

    private class ItemConfiguration : CellAndSpan() {
        val map = ArrayMap<View, CellAndSpan>()
        private val savedMap = ArrayMap<View, CellAndSpan>()
        val sortedViews = ArrayList<View>()
        var intersectingViews: ArrayList<View>? = null
        var isSolution = false
        fun save() {
            // Copy current state into savedMap
            for (v in map.keys) {
                savedMap[v]!!.copyFrom(map[v])
            }
        }

        fun restore() {
            // Restore current state from savedMap
            for (v in savedMap.keys) {
                map[v]!!.copyFrom(savedMap[v])
            }
        }

        fun add(v: View, cs: CellAndSpan) {
            map[v] = cs
            savedMap[v] = CellAndSpan()
            sortedViews.add(v)
        }

        fun area(): Int {
            return 1
        }

        fun getBoundingRectForViews(views: ArrayList<View?>, outRect: Rect) {
            var first = true
            for (v in views) {
                val c = map[v]
                if (first) {
                    outRect[c!!.cellX, c.cellY, c.cellX + 1] = c.cellY + 1
                    first = false
                } else {
                    outRect.union(c!!.cellX, c.cellY, c.cellX + 1, c.cellY + 1)
                }
            }
        }
    }

    // TODO: Add animation once drag and drop is done.
/*    private fun animateItemsToSolution(
        solution: ItemConfiguration,
        dragView: View,
        commitDragView: Boolean
    ) {
        val occupied = mOccupied
        occupied.clear()
        val childCount: Int = childCount
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            if (child === dragView) continue
            val c = solution.map[child]
            if (c != null) {
                animateChildToPosition(
                    child, c.cellX, c.cellY, CellLayout.REORDER_ANIMATION_DURATION, 0,
                    CellLayout.DESTRUCTIVE_REORDER, false
                )
                occupied.markCells(c, true)
            }
        }
        if (commitDragView) {
            occupied.markCells(solution, true)
        }
    }*/

    // TODO: Add animation once drag and drop is done.
    /*open fun animateChildToPosition(
        child: View, cellX: Int, cellY: Int, duration: Int,
        delay: Int, permanent: Boolean, adjustOccupied: Boolean
    ): Boolean {
        val index = indexOfChild(child)
        if (index != -1) {
            val lp: LayoutParams =
                child.layoutParams as GridLayout.LayoutParams
            val info: LauncherItem = child.tag as LauncherItem

            val oldX: Int = lp.x
            val oldY: Int = lp.y
            lp.isLockedToGrid = true
            if (permanent) {
                info.cellX = cellX
                lp.cellX = info.cellX
                info.cellY = cellY
                lp.cellY = info.cellY
            } else {
                lp.tmpCellX = cellX
                lp.tmpCellY = cellY
            }
            clc.setupLp(child)
            lp.isLockedToGrid = false
            val newX: Int = lp.x
            val newY: Int = lp.y
            lp.x = oldX
            lp.y = oldY

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true
                return true
            }
            val va: ValueAnimator = LauncherAnimUtils.ofFloat(0f, 1f)
            va.duration = duration.toLong()
            va.addUpdateListener { animation ->
                val r = animation.animatedValue as Float
                lp.x = ((1 - r) * oldX + r * newX).toInt()
                lp.y = ((1 - r) * oldY + r * newY).toInt()
                child.requestLayout()
            }
            va.addListener(object : AnimatorListenerAdapter() {
                var cancelled = false
                override fun onAnimationEnd(animation: Animator) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true
                        child.requestLayout()
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
            })
            va.startDelay = delay.toLong()
            va.start()
            return true
        }
        return false
    }*/

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

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        LayoutParams(context, attrs)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean =
        p != null && p is LayoutParams

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): LayoutParams = LayoutParams(lp)

    class LayoutParams : GridLayout.LayoutParams {

        /**
         * Rank of item in the grid based on x and y cell index.
         */
        var rank = 0

        /**
         * Temporary rank of the item in the grid during reorder.
         */
        var tmpRank = 0


        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        var useTmpCoords = false

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        var isLockedToGrid = true

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button and QSB place holder.
         */
        var canReorder = true

        // X coordinate of the view in the layout.
        @ExportedProperty
        var x = 0

        // Y coordinate of the view in the layout.
        @ExportedProperty
        var y = 0
        var dropped = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: LayoutParams) : super(source) {
            rank = source.rank
        }

        constructor(rank: Int) : super(
            spec(UNDEFINED), spec(UNDEFINED)
        ) {
            this.rank = rank
        }

        override fun toString(): String {
            return "(${this.rank})"
        }
    }
}
