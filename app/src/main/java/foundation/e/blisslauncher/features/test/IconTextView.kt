package foundation.e.blisslauncher.features.test

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils.TruncateAt
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import foundation.e.blisslauncher.core.Utilities
import foundation.e.blisslauncher.core.database.model.LauncherItem
import kotlin.math.ceil

/**
 * A text view which displays an icon on top side of it.
 */
@SuppressLint("AppCompatCustomView")
class IconTextView @JvmOverloads constructor(context: Context) : TextView(context) {

    companion object {
        private const val DISPLAY_WORKSPACE = 1
        private const val DISPLAY_FOLDER = 2
        private val STATE_PRESSED = intArrayOf(R.attr.state_pressed)
    }

    private var mStayPressed: Boolean = false
    val iconBounds: Rect
        get() {
            val top = paddingTop
            val left = (width - defaultIconSize) / 2
            val right = left + defaultIconSize
            val bottom = top + defaultIconSize
            return Rect(left, top, right, bottom)
        }
    private val launcher: TestActivity = TestActivity.getLauncher(context)
    private val dp = launcher.deviceProfile
    private val defaultIconSize = dp.iconSizePx

    private var mTextAlpha = 1f
    private var mTextColor = Color.WHITE

    private var disableRelayout = false
    private var mIcon: Drawable? = null
    private val slop =
        ViewConfiguration.get(getContext()).scaledTouchSlop.toFloat()

    private var longPressHelper: CheckLongPressHelper

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, dp.iconTextSizePx.toFloat())
        compoundDrawablePadding = dp.iconDrawablePaddingPx
        ellipsize = TruncateAt.END
        longPressHelper = CheckLongPressHelper(this)
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

    override fun setTextColor(color: Int) {
        mTextColor = color
        super.setTextColor(getModifiedColor())
    }

    override fun setTextColor(colors: ColorStateList) {
        mTextColor = colors.defaultColor
        if (java.lang.Float.compare(mTextAlpha, 1f) == 0) {
            super.setTextColor(colors)
        } else {
            super.setTextColor(getModifiedColor())
        }
    }

    fun applyFromShortcutItem(item: LauncherItem) {
        applyIconAndLabel(item)
        tag = item
        applyBadgeState(item, false)
    }

    private fun applyIconAndLabel(item: LauncherItem) {
        setIcon(item.icon)
        text = item.title
    }

    private fun applyBadgeState(item: LauncherItem, animate: Boolean) {
    }

    override fun setTag(tag: Any?) {
        if (tag != null) {
            //TODO: Check Item info locked
        }
        super.setTag(tag)
    }

    override fun refreshDrawableState() {
        super.refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (mStayPressed) {
            mergeDrawableStates(drawableState, STATE_PRESSED)
        }
        return drawableState
    }

    fun getIcon() = mIcon

    /**
     * Sets the icon for this view based on the layout direction.
     */
    private fun setIcon(icon: Drawable) {
        applyCompoundDrawables(icon)
        mIcon = icon
    }

    protected fun applyCompoundDrawables(icon: Drawable) {
        // If we had already set an icon before, disable relayout as the icon size is the
        // same as before.
        disableRelayout = mIcon != null
        icon.setBounds(0, 0, defaultIconSize, defaultIconSize)
        setCompoundDrawables(null, icon, null, null)
        disableRelayout = false
    }

    override fun requestLayout() {
        if (!disableRelayout) {
            super.requestLayout()
        }
    }

    fun setTextVisibility(visible: Boolean) {
        setTextAlpha(if (visible) 1f else 0.toFloat())
    }

    private fun setTextAlpha(alpha: Float) {
        mTextAlpha = alpha
        super.setTextColor(getModifiedColor())
    }

    private fun getModifiedColor(): Int {
        return if (mTextAlpha == 0f) {
            // Special case to prevent text shadows in high contrast mode
            Color.TRANSPARENT
        } else ColorUtils.setAlphaComponent(
            mTextColor, Math.round(Color.alpha(mTextColor) * mTextAlpha)
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        var result = super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> longPressHelper.cancelLongPress()
            MotionEvent.ACTION_MOVE -> if (!Utilities.pointInView(this, event.x, event.y, slop)) {
                longPressHelper.cancelLongPress()
            }
        }
        return result
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

    override fun cancelLongPress() {
        super.cancelLongPress()
        longPressHelper.cancelLongPress()
    }

    fun clearPressedBackground() {
        isPressed = false
        setStayPressed(false)
    }

    fun setStayPressed(stayPressed: Boolean) {
        mStayPressed = stayPressed
        refreshDrawableState()
    }
}
