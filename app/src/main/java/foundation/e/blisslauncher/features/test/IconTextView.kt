package foundation.e.blisslauncher.features.test

import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils.TruncateAt
import android.util.AttributeSet
import android.util.Property
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import foundation.e.blisslauncher.core.Utilities
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.features.notification.DotInfo
import foundation.e.blisslauncher.features.notification.DotRenderer
import java.lang.IllegalArgumentException
import kotlin.math.roundToInt

/**
 * A text view which displays an icon on top side of it.
 */
@SuppressLint("AppCompatCustomView")
class IconTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int
) : TextView(context, attrs, defStyle) {

    companion object {
        private const val DISPLAY_WORKSPACE = 1
        private const val DISPLAY_FOLDER = 2
        private val STATE_PRESSED = intArrayOf(R.attr.state_pressed)

        private val DOT_SCALE_PROPERTY: Property<IconTextView, Float> =
            object : Property<IconTextView, Float>(
                java.lang.Float.TYPE,
                "dotScale"
            ) {
                override fun get(iconTextView: IconTextView): Float {
                    return iconTextView.dotScale
                }

                override fun set(
                    iconTextView: IconTextView,
                    value: Float
                ) {
                    iconTextView.dotScale = value
                    iconTextView.invalidate()
                }
            }
    }

    private lateinit var mDotRenderer: DotRenderer
    private val mActivity: TestActivity = if (context is TestActivity) context
    else throw IllegalArgumentException("Cannot find TestActivity in context tree")
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

    private var dotScale: Float = 0f

    private var mTextAlpha = 1f
    private var mTextColor = Color.WHITE

    private var disableRelayout = false
    private var mIcon: Drawable? = null
    private val slop =
        ViewConfiguration.get(getContext()).scaledTouchSlop.toFloat()

    private var longPressHelper: CheckLongPressHelper

    private var mDotInfo: DotInfo? = null
    private var mDotScaleAnim: Animator? = null
    private var mForceHideDot = false

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

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
        applyDotState(item, false)
    }

    private fun applyIconAndLabel(item: LauncherItem) {
        setIcon(item.icon)
        text = item.title
    }

    override fun setTag(tag: Any?) {
        if (tag != null) {
            // TODO: Check Item info locked
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

    fun applyDotState(itemInfo: LauncherItem, animate: Boolean) {
        val wasDotted = mDotInfo != null
        mDotInfo = mActivity.getDotInfoForItem(itemInfo)
        val isDotted = mDotInfo != null
        val newDotScale: Float = if (isDotted) 1f else 0f
        mDotRenderer = mActivity.deviceProfile.mDotRenderer
        if (wasDotted || isDotted) {
            // Animate when a dot is first added or when it is removed.
            if (animate && wasDotted xor isDotted && isShown) {
                animateDotScale(newDotScale)
            } else {
                cancelDotScaleAnim()
                dotScale = newDotScale
                invalidate()
            }
        }
    }

    /**
     * Resets the view so it can be recycled.
     */
    fun reset() {
        mDotInfo = null
        cancelDotScaleAnim()
        dotScale = 0f
        mForceHideDot = false
    }

    private fun cancelDotScaleAnim() {
        mDotScaleAnim?.cancel()
    }

    private fun animateDotScale(vararg dotScales: Float) {
        cancelDotScaleAnim()
        mDotScaleAnim = ObjectAnimator.ofFloat(
            this,
            DOT_SCALE_PROPERTY,
            *dotScales
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mDotScaleAnim = null
                }
            })
        }
        mDotScaleAnim?.start()
    }

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
        setTextAlpha(if (visible) 1f else 0f)
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
            mTextColor, (Color.alpha(mTextColor) * mTextAlpha).roundToInt()
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawDotIfNecessary(canvas)
    }

    /**
     * Draws the notification dot in the top right corner of the icon bounds.
     * @param canvas The canvas to draw to.
     */
    private fun drawDotIfNecessary(canvas: Canvas?) {
        if (!mForceHideDot && (hasDot() || dotScale > 0)) {
            val tempBounds = Rect()
            getIconBounds(tempBounds)
            val scrollX = scrollX
            val scrollY = scrollY
            canvas?.translate(scrollX.toFloat(), scrollY.toFloat())
            mDotRenderer.drawDot(canvas, tempBounds)
            canvas?.translate(-scrollX.toFloat(), -scrollY.toFloat())
        }
    }

    private fun hasDot(): Boolean {
        return mDotInfo != null
    }

    fun getIconBounds(outBounds: Rect) {
        getIconBounds(this, outBounds, defaultIconSize)
    }

    fun getIconBounds(iconView: View, outBounds: Rect, iconSize: Int) {
        val top = iconView.paddingTop
        val left = (iconView.width - iconSize) / 2
        val right = left + iconSize
        val bottom = top + iconSize
        outBounds[left, top, right] = bottom
    }
}
