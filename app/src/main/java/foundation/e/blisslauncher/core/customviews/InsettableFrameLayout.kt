package foundation.e.blisslauncher.core.customviews

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout

open class InsettableFrameLayout(private val mContext: Context, attrs: AttributeSet?) : FrameLayout(
    mContext, attrs
), Insettable {

    var windowInsets: WindowInsets? = null

    private fun setFrameLayoutChildInsets(child: View, newInsets: WindowInsets?, oldInsets: Rect) {
        if (newInsets == null) return
        val lp: LayoutParams =
            child.layoutParams as LayoutParams
        if (child is Insettable) {
            (child as Insettable).setInsets(newInsets)
        } else {
            lp.topMargin += newInsets.systemWindowInsetTop - oldInsets.top
            lp.leftMargin += newInsets.systemWindowInsetLeft - oldInsets.left
            lp.rightMargin += newInsets.systemWindowInsetRight - oldInsets.right
            lp.bottomMargin += newInsets.systemWindowInsetBottom - oldInsets.bottom
        }
        child.layoutParams = lp
    }

    override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets? {
        Log.d("InsettableFrameLayout", "setInsets() called with: insets = $insets")

        //BlissLauncher.getApplication(mContext).resetDeviceProfile()
        setInsets(insets)
        return insets
    }

    override fun setInsets(insets: WindowInsets?) {
        if (insets == null) return
        setPadding(
            paddingLeft, paddingTop,
            paddingRight, paddingBottom + insets.systemWindowInsetBottom
        )
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            var oldInsets = Rect()
            if (this.windowInsets != null) {
                oldInsets.left = this.windowInsets!!.systemWindowInsetLeft
                oldInsets.top = this.windowInsets!!.systemWindowInsetTop
                oldInsets.right = this.windowInsets!!.systemWindowInsetRight
                oldInsets.bottom = this.windowInsets!!.systemWindowInsetBottom
            }
            setFrameLayoutChildInsets(child, insets, oldInsets)
        }
        this.windowInsets = insets
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        setFrameLayoutChildInsets(child, windowInsets, Rect())
    }

    companion object {
        fun dispatchInsets(parent: ViewGroup, insets: WindowInsets) {
            val n = parent.childCount
            for (i in 0 until n) {
                val child = parent.getChildAt(i)
                if (child is Insettable) {
                    (child as Insettable).setInsets(insets)
                }
            }
        }
    }
}