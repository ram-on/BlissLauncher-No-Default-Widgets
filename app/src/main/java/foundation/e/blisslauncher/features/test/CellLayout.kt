package foundation.e.blisslauncher.features.test

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.GridLayout
import foundation.e.blisslauncher.core.customviews.Insettable
import foundation.e.blisslauncher.core.utils.Constants

open class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr), Insettable {

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

    override fun setInsets(insets: WindowInsets) {
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

    private fun measureChild(child: View) {
        val lp = child.layoutParams as LayoutParams
        lp.rowSpec = spec(UNDEFINED)
        lp.columnSpec = spec(UNDEFINED)
        lp.width = cellWidth
        lp.height = cellHeight
        // Center the icon/folder
        val cHeight: Int = dp.cellHeightPx
        val cellPaddingY = 0f.coerceAtLeast((lp.height - cHeight) / 2f).toInt()
        var cellPaddingX: Int
        if (containerType == Constants.CONTAINER_DESKTOP) {
            cellPaddingX = dp.workspaceCellPaddingXPx
        } else {
            cellPaddingX = (dp.edgeMarginPx / 2f).toInt()
        }
        child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, cellPaddingY)
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
}
