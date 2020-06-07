package foundation.e.blisslauncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils.TruncateAt
import android.util.TypedValue
import android.widget.TextView
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import foundation.e.blisslauncher.domain.entity.ShortcutItem
import foundation.e.blisslauncher.features.launcher.LauncherActivity
import foundation.e.blisslauncher.graphics.DrawableFactory
import kotlin.math.ceil

/**
 * A text view which displays an icon on top side of it.
 */
@SuppressLint("AppCompatCustomView")
class IconTextView @JvmOverloads constructor(context: Context) : TextView(context) {

    private val launcher: LauncherActivity = LauncherActivity.getLauncher(context)
    private val dp = launcher.deviceProfile
    val defaultIconSize = dp.iconSizePx

    private var disableRelayout = false
    private var mIcon: Drawable? = null

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, dp.iconTextSizePx.toFloat())
        compoundDrawablePadding = dp.iconDrawablePaddingPx
        ellipsize = TruncateAt.END
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        // Disable marques when not focused to that, so that updating text does not cause relayout.
        ellipsize = if (focused) TruncateAt.MARQUEE else TruncateAt.END
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    fun reset() {}

    fun applyFromShortcutItem(item: ShortcutItem) {
        applyIconAndLabel(item)
        tag = item
        applyBadgeState(item, false)
    }

    private fun applyIconAndLabel(item: LauncherItemWithIcon) {
        val icon = DrawableFactory(context).newIcon(item)
        disableRelayout = mIcon != null
        icon.setBounds(0, 0, defaultIconSize, defaultIconSize)
        setCompoundDrawables(null, icon, null, null)
        disableRelayout = false
        mIcon = icon
        text = item.title
    }

    private fun applyBadgeState(item: ShortcutItem, animate: Boolean) {
    }

    override fun setTag(tag: Any?) {
        if (tag != null) {
            //TODO: Check Item info locked
        }
        super.setTag(tag)
    }

    override fun requestLayout() {
        if (!disableRelayout) {
            super.requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val fm = paint.fontMetrics
        val cellHeightPx: Int = defaultIconSize + compoundDrawablePadding +
            ceil(fm.bottom - fm.top.toDouble()).toInt()
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setPadding(
            paddingLeft, (height - cellHeightPx) / 2, paddingRight,
            paddingBottom
        )
    }
}