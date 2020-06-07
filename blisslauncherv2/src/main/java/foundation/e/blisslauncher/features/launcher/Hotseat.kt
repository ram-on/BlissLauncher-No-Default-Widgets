package foundation.e.blisslauncher.features.launcher

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import foundation.e.blisslauncher.common.LauncherConstants
import foundation.e.blisslauncher.views.CellLayout

class Hotseat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CellLayout(context) {

    private val launcher: LauncherActivity = LauncherActivity.getLauncher(context)

    override var containerType = LauncherConstants.ContainerType.CONTAINER_DESKTOP

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