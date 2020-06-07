package foundation.e.blisslauncher.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import foundation.e.blisslauncher.R
import timber.log.Timber

class InsettableFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Insettable {

    private val insets = Rect()

    override fun setInsets(newInsets: Rect) {
        val n = childCount
        for (i in 0 until n) {
            val child = getChildAt(i)
            setFrameLayoutChildInsets(child, insets, newInsets)
        }
        insets.set(insets)
    }

    override fun onApplyWindowInsets(newInsets: WindowInsets): WindowInsets {
        Timber.d("this is called")
        insets.set(0, newInsets.systemWindowInsetTop, 0, newInsets.systemWindowInsetBottom)
        setInsets(insets)
        return newInsets
    }

    fun setFrameLayoutChildInsets(
        child: View,
        newInsets: Rect,
        oldInsets: Rect
    ) {
        val lp: LayoutParams =
            child.layoutParams as LayoutParams
        if (child is Insettable) {
            (child as Insettable).setInsets(newInsets)
        } else if (!lp.ignoreInsets) {
            lp.topMargin += newInsets.top - oldInsets.top
            lp.leftMargin += newInsets.left - oldInsets.left
            lp.rightMargin += newInsets.right - oldInsets.right
            lp.bottomMargin += newInsets.bottom - oldInsets.bottom
        }
        child.layoutParams = lp
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Override to allow type-checking of LayoutParams.
    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    class LayoutParams : FrameLayout.LayoutParams {
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

        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(lp: ViewGroup.LayoutParams?) : super(lp) {}
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        setFrameLayoutChildInsets(child!!, insets, Rect())
    }
}