package foundation.e.blisslauncher.features.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider
import foundation.e.blisslauncher.core.blur.ShaderBlurDrawable
import foundation.e.blisslauncher.core.customviews.Insettable
import foundation.e.blisslauncher.core.customviews.InsettableFrameLayout
import foundation.e.blisslauncher.core.runOnMainThread

class WidgetPageLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : InsettableFrameLayout(context, attrs), Insettable,
    BlurWallpaperProvider.Listener {
    private val blurWallpaperProvider: BlurWallpaperProvider
    private var fullBlurDrawable: ShaderBlurDrawable? = null
    private val blurAlpha = 255
    private val blurDrawableCallback: Drawable.Callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            runOnMainThread {
                invalidate()
            }
        }

        override fun scheduleDrawable(
            who: Drawable,
            what: Runnable,
            `when`: Long
        ) {
        }

        override fun unscheduleDrawable(
            who: Drawable,
            what: Runnable
        ) {
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BlurWallpaperProvider.getInstance(context).addListener(this)
        fullBlurDrawable?.startListening()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        BlurWallpaperProvider.getInstance(context).removeListener(this)
        fullBlurDrawable?.stopListening()
    }

    override fun onDraw(canvas: Canvas) {
        fullBlurDrawable?.alpha = blurAlpha
        // fullBlurDrawable?.draw(canvas)
        super.onDraw(canvas)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            fullBlurDrawable?.setBounds(left, top, right, bottom)
        }
    }

    /**
     * We only need to change right bound for widget page blur layer.
     */
    fun changeBlurBounds(factor: Float, isLeftToRight: Boolean) {
        fullBlurDrawable?.setBounds(left, top, (right * factor).toInt(), bottom)
        if (isLeftToRight) {
            fullBlurDrawable?.canvasOffset =
                (right - left) * (1 - factor)
        }
        fullBlurDrawable?.invalidateSelf()
    }

    private fun createBlurDrawable() {
        if (isAttachedToWindow) {
            fullBlurDrawable?.stopListening()
        }
        fullBlurDrawable = blurWallpaperProvider.createDrawable().apply {
            callback = blurDrawableCallback
            setBounds(left, top, right, bottom)
        }
        if (isAttachedToWindow) fullBlurDrawable?.startListening()
    }

    override fun onEnabledChanged() {
        createBlurDrawable()
    }

    override fun onWallpaperChanged() {}

    init {
        setWillNotDraw(false)
        blurWallpaperProvider = BlurWallpaperProvider.getInstance(context)
        createBlurDrawable()
    }
}
