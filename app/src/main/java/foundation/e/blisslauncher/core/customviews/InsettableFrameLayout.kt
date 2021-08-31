package foundation.e.blisslauncher.core.customviews

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import foundation.e.blisslauncher.R

open class InsettableFrameLayout(mContext: Context, attrs: AttributeSet?) : FrameLayout(
    mContext, attrs
), Insettable {

    /*val insets: Rect
        get() {
            var tempInsets = Rect()
            if (this.windowInsets != null) {
                tempInsets.left = this.windowInsets!!.systemWindowInsetLeft
                tempInsets.top = this.windowInsets!!.systemWindowInsetTop
                tempInsets.right = this.windowInsets!!.systemWindowInsetRight
                tempInsets.bottom = this.windowInsets!!.systemWindowInsetBottom
            }
            return tempInsets
        }*/

    @JvmField
    val mInsets = Rect()

    private fun setFrameLayoutChildInsets(child: View, newInsets: Rect, oldInsets: Rect) {
        val lp: FrameLayout.LayoutParams =
            child.layoutParams as FrameLayout.LayoutParams
        if (child is Insettable) {
            (child as Insettable).setInsets(newInsets)
        } else {
            lp.topMargin += newInsets.top - oldInsets.top
            lp.leftMargin += newInsets.left - oldInsets.left
            lp.rightMargin += newInsets.right - oldInsets.right
            lp.bottomMargin += newInsets.bottom - oldInsets.bottom
        }
        child.layoutParams = lp
    }

    override fun setInsets(insets: Rect) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            setFrameLayoutChildInsets(child, insets, mInsets)
        }
        mInsets.set(insets)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        setFrameLayoutChildInsets(child, mInsets, Rect())
    }

    companion object {
        fun dispatchInsets(parent: ViewGroup, insets: Rect) {
            val n = parent.childCount
            for (i in 0 until n) {
                val child = parent.getChildAt(i)
                if (child is Insettable) {
                    (child as Insettable).setInsets(insets)
                }
            }
        }
    }
    open class LayoutParams : FrameLayout.LayoutParams {
        var ignoreInsets = false

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val a = c.obtainStyledAttributes(
                attrs,
                R.styleable.InsettableFrameLayout_Layout
            )
            ignoreInsets = a.getBoolean(
                R.styleable.InsettableFrameLayout_Layout_layout_ignoreInsets, false
            )
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)
        constructor(lp: ViewGroup.LayoutParams?) : super(lp!!)
    }
}
