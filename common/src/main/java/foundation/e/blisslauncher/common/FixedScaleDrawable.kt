package foundation.e.blisslauncher.common

import android.annotation.TargetApi
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.util.AttributeSet
import org.xmlpull.v1.XmlPullParser

/**
 * Extension of [DrawableWrapper] which scales the child drawables by a fixed amount.
 */
@TargetApi(Build.VERSION_CODES.N)
class FixedScaleDrawable :
    DrawableWrapper(ColorDrawable()) {
    private var mScaleX: Float
    private var mScaleY: Float
    override fun draw(canvas: Canvas) {
        val saveCount = canvas.save()
        canvas.scale(
            mScaleX, mScaleY,
            bounds.exactCenterX(), bounds.exactCenterY()
        )
        super.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet
    ) {
    }

    override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme
    ) {
    }

    fun setScale(scale: Float) {
        val h = intrinsicHeight.toFloat()
        val w = intrinsicWidth.toFloat()
        mScaleX = scale * LEGACY_ICON_SCALE
        mScaleY = scale * LEGACY_ICON_SCALE
        if (h > w && w > 0) {
            mScaleX *= w / h
        } else if (w > h && h > 0) {
            mScaleY *= h / w
        }
    }

    companion object {
        // TODO b/33553066 use the constant defined in MaskableIconDrawable
        const val LEGACY_ICON_SCALE = .7f * .6667f
    }

    init {
        mScaleX = LEGACY_ICON_SCALE
        mScaleY = LEGACY_ICON_SCALE
    }
}
