package foundation.e.blisslauncher.core.customviews

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import foundation.e.blisslauncher.BlissLauncher

open class InsettableFrameLayout(private val mContext: Context, attrs: AttributeSet?) : FrameLayout(
    mContext, attrs
), Insettable {

    private var mInsets: WindowInsets? = null

    private fun setFrameLayoutChildInsets(child: View, newInsets: WindowInsets?) {
        if (newInsets == null) return
        val lp: LayoutParams =
            child.layoutParams as LayoutParams
        if (child is Insettable) {
            (child as Insettable).setInsets(newInsets)
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
            setFrameLayoutChildInsets(child, insets)
        }
        mInsets = insets
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        setFrameLayoutChildInsets(child, mInsets)
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