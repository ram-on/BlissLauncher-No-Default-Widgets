package foundation.e.blisslauncher.features.test

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
