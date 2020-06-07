/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package foundation.e.blisslauncher.graphics

import android.R
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Property
import android.util.SparseArray
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon
import kotlin.math.floor

open class FastBitmapDrawable constructor(
    protected var bitmap: Bitmap?
) :
    Drawable() {
    protected val mPaint =
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var mIsPressed = false
    private var mIsDisabled = false
    private var mScaleAnimation: ObjectAnimator? = null
    private var mScale = 1f

    // The saturation and brightness are values that are mapped to REDUCED_FILTER_VALUE_SPACE and
    // as a result, can be used to compose the key for the cached ColorMatrixColorFilters
    private var mDesaturation = 0
    private var mBrightness = 0
    private var mAlpha = 255
    private var mPrevUpdateKey = Int.MAX_VALUE

    constructor(info: LauncherItemWithIcon) : this(info.iconBitmap)

    override fun draw(canvas: Canvas) {
        if (mScaleAnimation != null) {
            val count = canvas.save()
            val bounds = bounds
            canvas.scale(mScale, mScale, bounds.exactCenterX(), bounds.exactCenterY())
            drawInternal(canvas, bounds)
            canvas.restoreToCount(count)
        } else {
            drawInternal(canvas, bounds)
        }
    }

    protected fun drawInternal(
        canvas: Canvas,
        bounds: Rect?
    ) {
        canvas.drawBitmap(bitmap, null, bounds, mPaint)
    }

    override fun setColorFilter(cf: ColorFilter) {
        // No op
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {
        mAlpha = alpha
        mPaint.alpha = alpha
    }

    override fun setFilterBitmap(filterBitmap: Boolean) {
        mPaint.isFilterBitmap = filterBitmap
        mPaint.isAntiAlias = filterBitmap
    }

    override fun getAlpha(): Int {
        return mAlpha
    }

    val animatedScale: Float
        get() = if (mScaleAnimation == null) 1f else mScale

    override fun getIntrinsicWidth(): Int {
        return bitmap!!.width
    }

    override fun getIntrinsicHeight(): Int {
        return bitmap!!.height
    }

    override fun getMinimumWidth(): Int {
        return bounds.width()
    }

    override fun getMinimumHeight(): Int {
        return bounds.height()
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun getColorFilter(): ColorFilter {
        return mPaint.colorFilter
    }

    override fun onStateChange(state: IntArray): Boolean {
        var isPressed = false
        for (s in state) {
            if (s == R.attr.state_pressed) {
                isPressed = true
                break
            }
        }
        if (mIsPressed != isPressed) {
            mIsPressed = isPressed
            if (mScaleAnimation != null) {
                mScaleAnimation!!.cancel()
                mScaleAnimation = null
            }
            if (mIsPressed) {
                // Animate when going to pressed state
                mScaleAnimation = ObjectAnimator.ofFloat(
                    this,
                    SCALE,
                    PRESSED_SCALE
                )
                mScaleAnimation!!.duration = CLICK_FEEDBACK_DURATION.toLong()
                mScaleAnimation!!.start()
            } else {
                mScale = 1f
                invalidateSelf()
            }
            return true
        }
        return false
    }

    private fun invalidateDesaturationAndBrightness() {
        desaturation = if (mIsDisabled) DISABLED_DESATURATION else 0f
        brightness = if (mIsDisabled) DISABLED_BRIGHTNESS else 0f
    }

    fun setIsDisabled(isDisabled: Boolean) {
        if (mIsDisabled != isDisabled) {
            mIsDisabled = isDisabled
            invalidateDesaturationAndBrightness()
        }
    }

    /**
     * Sets the saturation of this icon, 0 [full color] -> 1 [desaturated]
     */
    var desaturation: Float
        get() = mDesaturation.toFloat() / REDUCED_FILTER_VALUE_SPACE
        private set(desaturation) {
            val newDesaturation =
                Math.floor(desaturation * REDUCED_FILTER_VALUE_SPACE.toDouble()).toInt()
            if (mDesaturation != newDesaturation) {
                mDesaturation = newDesaturation
                updateFilter()
            }
        }

    /**
     * Sets the brightness of this icon, 0 [no add. brightness] -> 1 [2bright2furious]
     */
    private var brightness: Float
        private get() = mBrightness.toFloat() / REDUCED_FILTER_VALUE_SPACE
        private set(brightness) {
            val newBrightness =
                floor(brightness * REDUCED_FILTER_VALUE_SPACE.toDouble()).toInt()
            if (mBrightness != newBrightness) {
                mBrightness = newBrightness
                updateFilter()
            }
        }

    /**
     * Updates the paint to reflect the current brightness and saturation.
     */
    private fun updateFilter() {
        var usePorterDuffFilter = false
        var key = -1
        if (mDesaturation > 0) {
            key = mDesaturation shl 16 or mBrightness
        } else if (mBrightness > 0) {
            // Compose a key with a fully saturated icon if we are just animating brightness
            key = 1 shl 16 or mBrightness

            // We found that in L, ColorFilters cause drawing artifacts with shadows baked into
            // icons, so just use a PorterDuff filter when we aren't animating saturation
            usePorterDuffFilter = true
        }

        // Debounce multiple updates on the same frame
        if (key == mPrevUpdateKey) {
            return
        }
        mPrevUpdateKey = key
        if (key != -1) {
            var filter = sCachedFilter[key]
            if (filter == null) {
                val brightnessF = brightness
                val brightnessI = (255 * brightnessF).toInt()
                if (usePorterDuffFilter) {
                    filter = PorterDuffColorFilter(
                        Color.argb(brightnessI, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP
                    )
                } else {
                    val saturationF = 1f - desaturation
                    sTempFilterMatrix.setSaturation(saturationF)
                    if (mBrightness > 0) {
                        // Brightness: C-new = C-old*(1-amount) + amount
                        val scale = 1f - brightnessF
                        val mat =
                            sTempBrightnessMatrix.array
                        mat[0] = scale
                        mat[6] = scale
                        mat[12] = scale
                        mat[4] = brightnessI.toFloat()
                        mat[9] = brightnessI.toFloat()
                        mat[14] = brightnessI.toFloat()
                        sTempFilterMatrix.preConcat(sTempBrightnessMatrix)
                    }
                    filter = ColorMatrixColorFilter(sTempFilterMatrix)
                }
                sCachedFilter.append(key, filter)
            }
            mPaint.colorFilter = filter
        } else {
            mPaint.colorFilter = null
        }
        invalidateSelf()
    }

    override fun getConstantState(): ConstantState {
        return MyConstantState(bitmap)
    }

    protected class MyConstantState(protected val mBitmap: Bitmap?) :
        ConstantState() {
        override fun newDrawable(): Drawable {
            return FastBitmapDrawable(mBitmap)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }
    }

    companion object {
        private const val PRESSED_SCALE = 1.1f
        private const val DISABLED_DESATURATION = 1f
        private const val DISABLED_BRIGHTNESS = 0.5f
        const val CLICK_FEEDBACK_DURATION = 200

        // Since we don't need 256^2 values for combinations of both the brightness and saturation, we
        // reduce the value space to a smaller value V, which reduces the number of cached
        // ColorMatrixColorFilters that we need to keep to V^2
        private const val REDUCED_FILTER_VALUE_SPACE = 48

        // A cache of ColorFilters for optimizing brightness and saturation animations
        private val sCachedFilter = SparseArray<ColorFilter>()

        // Temporary matrices used for calculation
        private val sTempBrightnessMatrix = ColorMatrix()
        private val sTempFilterMatrix = ColorMatrix()

        // Animator and properties for the fast bitmap drawable's scale
        private val SCALE: Property<FastBitmapDrawable, Float> = object :
            Property<FastBitmapDrawable, Float>(java.lang.Float.TYPE, "scale") {
            override fun get(fastBitmapDrawable: FastBitmapDrawable): Float {
                return fastBitmapDrawable.mScale
            }

            override fun set(
                fastBitmapDrawable: FastBitmapDrawable,
                value: Float
            ) {
                fastBitmapDrawable.mScale = value
                fastBitmapDrawable.invalidateSelf()
            }
        }
    }

    init {
        isFilterBitmap = true
    }
}