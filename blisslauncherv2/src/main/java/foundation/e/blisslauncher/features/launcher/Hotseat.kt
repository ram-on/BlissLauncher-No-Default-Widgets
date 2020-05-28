package foundation.e.blisslauncher.features.launcher

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import foundation.e.blisslauncher.widget.Insettable

class Hotseat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr), Insettable {

    private val launcher: LauncherActivity = LauncherActivity.getLauncher(context)

    override fun setInsets(insets: Rect) {
        val lp = getLayoutParams() as FrameLayout.LayoutParams
        val idp = launcher.deviceProfile
        lp.gravity = Gravity.BOTTOM
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = idp.hotseatBarSizePx + insets!!.bottom

        val padding: Rect = idp.hotseatLayoutPadding
        setPadding(padding.left, padding.top, padding.right, padding.bottom)

        layoutParams = lp

    }
}