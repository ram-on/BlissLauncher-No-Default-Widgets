package foundation.e.blisslauncher.features.test

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
import foundation.e.blisslauncher.core.database.model.ApplicationItem
import foundation.e.blisslauncher.core.database.model.FolderItem
import foundation.e.blisslauncher.core.database.model.LauncherItem
import foundation.e.blisslauncher.core.database.model.ShortcutItem
import foundation.e.blisslauncher.core.utils.Constants
import foundation.e.blisslauncher.features.notification.DotInfo
import foundation.e.blisslauncher.features.notification.DotRenderer
import foundation.e.blisslauncher.features.notification.FolderDotInfo
import foundation.e.blisslauncher.features.test.uninstall.UninstallButtonRenderer
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
        private val STATE_PRESSED = intArrayOf(android.R.attr.state_pressed)

        private const val TAG = "IconTextView"

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

        private val UNINSTALL_SCALE_PROPERTY: Property<IconTextView, Float> =
            object : Property<IconTextView, Float>(
                java.lang.Float.TYPE,
                "uninstallButtonScale"
            ) {
                override fun get(iconTextView: IconTextView): Float {
                    return iconTextView.uninstallButtonScale
                }

                override fun set(
                    iconTextView: IconTextView,
                    value: Float
                ) {
                    iconTextView.uninstallButtonScale = value
                    iconTextView.invalidate()
                }
            }
    }

    private lateinit var mDotRenderer: DotRenderer
    private lateinit var mUninstallRenderer: UninstallButtonRenderer
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
    private var uninstallButtonScale: Float = 0f

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

    private var isUninstallVisible: Boolean = false
    private var mUninstallIconScaleAnim: Animator? = null

    private var touchX = 0
    private var touchY = 0

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, dp.iconTextSizePx.toFloat())
        compoundDrawablePadding = dp.iconDrawablePaddingPx
        ellipsize = TruncateAt.END
        longPressHelper = CheckLongPressHelper(this)
        setTextAlpha(1f)
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
        applyUninstallIconState(false)
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

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (mStayPressed) {
            mergeDrawableStates(drawableState, STATE_PRESSED)
        }
        return drawableState
    }

    fun getIcon() = mIcon

    fun applyDotState(itemInfo: LauncherItem, animate: Boolean) {
        if (tag !is FolderItem) {
            val wasDotted = mDotInfo != null
            mDotInfo = mActivity.getDotInfoForItem(itemInfo)
            val isDotted = mDotInfo != null
            updateDotScale(wasDotted, isDotted, animate)
        }
    }

    fun setDotInfo(item: FolderItem, dotInfo: FolderDotInfo) {
        val wasDotted = mDotInfo is FolderDotInfo && (mDotInfo as FolderDotInfo).hasDot()
        updateDotScale(wasDotted, dotInfo.hasDot(), true)
        mDotInfo = dotInfo
    }

    private fun updateDotScale(wasDotted: Boolean, isDotted: Boolean, animate: Boolean) {
        val newDotScale: Float = if (isDotted) 1f else 0f
        mDotRenderer = mActivity.deviceProfile.mDotRenderer
        if (wasDotted || isDotted) {
            // Animate when a dot is first added or when it is removed.
            if (animate && (wasDotted xor isDotted) && isShown) {
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

        isUninstallVisible = false
        cancelUninstallScaleAnim()
        uninstallButtonScale = 0f
    }

    private fun cancelUninstallScaleAnim() {
        mUninstallIconScaleAnim?.cancel()
    }

    private fun cancelDotScaleAnim() {
        mDotScaleAnim?.cancel()
    }

    private fun animateDotScale(dotScales: Float) {
        cancelDotScaleAnim()
        mDotScaleAnim = ObjectAnimator.ofFloat(
            this,
            DOT_SCALE_PROPERTY,
            dotScales
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
            MotionEvent.ACTION_DOWN -> {
                // We store these value to check if the click has been made on uninstallIcon or not.
                touchX = event.x.toInt()
                touchY = event.y.toInt()
            }
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
        drawUninstallIcon(canvas)
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
        if (mDotInfo != null && mDotInfo is FolderDotInfo) {
            return (mDotInfo as FolderDotInfo).hasDot()
        }
        return mDotInfo != null
    }

    private fun getIconBounds(outBounds: Rect) {
        getIconBounds(this, outBounds, defaultIconSize)
    }

    private fun getIconBounds(iconView: View, outBounds: Rect, iconSize: Int) {
        val top = iconView.paddingTop
        val left = (iconView.width - iconSize) / 2
        val right = left + iconSize
        val bottom = top + iconSize
        outBounds[left, top, right] = bottom
    }

    /**
     * Draws the uninstall button in the top right corner of the icon bounds.
     * @param canvas The canvas to draw to.
     */
    private fun drawUninstallIcon(canvas: Canvas?) {
        if (isUninstallVisible || uninstallButtonScale > 0) {
            val tempBounds = Rect()
            getIconBounds(tempBounds)
            val scrollX = scrollX
            val scrollY = scrollY
            canvas?.translate(scrollX.toFloat(), scrollY.toFloat())
            mUninstallRenderer.draw(canvas, tempBounds)
            canvas?.translate(-scrollX.toFloat(), -scrollY.toFloat())
        }
    }

    fun applyUninstallIconState(showUninstallIcon: Boolean) {
        val wasUninstallVisible = isUninstallVisible
        isUninstallVisible = showUninstallIcon
        val newScale: Float = if (isUninstallVisible) 1f else 0f
        mUninstallRenderer = mActivity.deviceProfile.uninstallRenderer
        if (wasUninstallVisible || isUninstallVisible) {
            // Animate when a dot is first added or when it is removed.
            if (wasUninstallVisible xor isUninstallVisible && isShown) {
                animateUninstallScale(newScale)
            } else {
                cancelUninstallScaleAnim()
                uninstallButtonScale = newScale
                invalidate()
            }
        }
    }

    private fun animateUninstallScale(vararg scales: Float) {
        cancelUninstallScaleAnim()
        mUninstallIconScaleAnim = ObjectAnimator.ofFloat(
            this,
            UNINSTALL_SCALE_PROPERTY,
            *scales
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mUninstallIconScaleAnim = null
                }
            })
        }
        mUninstallIconScaleAnim?.start()
    }

    fun tryToHandleUninstallClick(launcher: TestActivity): Boolean {
        if (!isUninstallVisible) {
            return false
        }
        val iconBounds = Rect()
        getIconBounds(iconBounds)
        val uninstallIconBounds = mUninstallRenderer.getBoundsScaled(iconBounds)
        if (uninstallIconBounds.contains(touchX, touchY)) {
            val tag = tag as LauncherItem
            if (tag.itemType == Constants.ITEM_TYPE_APPLICATION) {
                launcher.uninstallApplication(tag as ApplicationItem)
            } else if (tag.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                launcher.removeShortcut(tag as ShortcutItem)
            }

            // Reset touch coordinates
            touchX = 0
            touchY = 0
            return true
        }
        return false
    }
}
