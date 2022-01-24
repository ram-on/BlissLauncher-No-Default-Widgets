package foundation.e.blisslauncher.core.blur

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import foundation.e.blisslauncher.core.DeviceProfile

class ShaderBlurDrawable internal constructor(private val blurWallpaperProvider: BlurWallpaperProvider) :
    Drawable(), BlurWallpaperProvider.Listener {

    private var blurAlpha = 255
    private val blurPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var blurBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                blurPaint.shader =
                    value?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            }
        }

    private val blurBounds = RectF()
    var canvasOffset: Float = 0f
    private val blurPath = Path()
    private var blurPathValid = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    invalidateSelf()
                }
            }
        }

    var noRadius = true

    override fun draw(canvas: Canvas) = draw(canvas, noRadius)

    fun draw(canvas: Canvas, noRadius: Boolean = false) {
        if (blurAlpha == 0) return
        if (blurBounds.right.toInt() - blurBounds.left.toInt() == 0) return
        blurBitmap = blurWallpaperProvider.wallpaper

        if (blurBitmap == null) {
            blurBitmap = blurWallpaperProvider.placeholder
        }
        blurBitmap =
            if (blurBitmap!!.height >= blurBounds.bottom &&
                blurBitmap!!.width >= blurBounds.right
            ) {
                Bitmap.createBitmap(
                    blurBitmap!!,
                    blurBounds.left.toInt(), blurBounds.top.toInt(),
                    blurBounds.right.toInt() - blurBounds.left.toInt(),
                    blurBounds.bottom.toInt() - blurBounds.top.toInt()
                )
            } else {
                blurBitmap
            }

        // setupBlurPath()

        // We check the offset just to make sure we don't translate it incorrectly
        // when moving back to home screen from widget page.
        if (canvasOffset > 0) {
            canvas.translate(canvasOffset, 0f)
        }
        if (noRadius) {
            canvas.drawRect(
                0f, 0f,
                blurBounds.right - blurBounds.left, blurBounds.bottom - blurBounds.top,
                blurPaint
            )
        } else {
            canvas.drawPath(DeviceProfile.path, blurPaint)
        }

        if (canvasOffset > 0)
            canvas.translate(-canvasOffset, 0f)
    }

    override fun setAlpha(alpha: Int) {
        blurAlpha = alpha
        blurPaint.alpha = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int {
        return blurAlpha
    }

    private fun setupBlurPath() {
        if (blurPathValid) return

        blurPath.reset()
        blurPath.addRect(
            0f,
            0f,
            blurBounds.right - blurBounds.left,
            blurBounds.bottom - blurBounds.top,
            Path.Direction.CW
        )
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) =
        setBlurBounds(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    fun setBlurBounds(left: Float, top: Float, right: Float, bottom: Float) {
        if (blurBounds.left != left ||
            blurBounds.top != top ||
            blurBounds.right != right ||
            blurBounds.bottom != bottom
        ) {
            blurBounds.set(left, top, right, bottom)
            blurPathValid = false
        }
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onWallpaperChanged() {
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    fun startListening() = blurWallpaperProvider.addListener(this)

    fun stopListening() = blurWallpaperProvider.removeListener(this)
}
