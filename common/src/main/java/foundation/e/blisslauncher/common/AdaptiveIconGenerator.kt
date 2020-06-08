package foundation.e.blisslauncher.common

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseIntArray
import androidx.core.graphics.ColorUtils
import foundation.e.blisslauncher.common.compat.AdaptiveIconCompat
import foundation.e.blisslauncher.common.graphics.ColorExtractor

class AdaptiveIconGenerator(private val context: Context, private val icon: Drawable) {
    private var ranLoop = false
    private val shouldWrap = false
    private var backgroundColor = Color.WHITE
    private val useWhiteBackground = true
    private var isFullBleed = false
    private var noMixinNeeded = false
    private var fullBleedChecked = false
    private val matchesMaskShape = false
    private val isBackgroundWhite = false
    private var scale = 0f
    private var height = 0
    private var aHeight = 0f
    private var width = 0
    private var aWidth = 0f
    private var result: Drawable? = null
    private fun loop() {
        val extractee = icon
        if (extractee == null) {
            Log.e("AdaptiveIconGenerator", "extractee is null, skipping.")
            onExitLoop()
            return
        }
        val bounds = RectF()
        scale = 1.0f
        if (extractee is ColorDrawable) {
            isFullBleed = true
            fullBleedChecked = true
        }
        width = extractee.intrinsicWidth
        height = extractee.intrinsicHeight
        aWidth = width * (1 - (bounds.left + bounds.right))
        aHeight = height * (1 - (bounds.top + bounds.bottom))

        // Check if the icon is squarish
        val ratio = aHeight / aWidth
        val isSquarish = 0.999 < ratio && ratio < 1.0001
        val almostSquarish = isSquarish || 0.97 < ratio && ratio < 1.005
        if (!isSquarish) {
            isFullBleed = false
            fullBleedChecked = true
        }
        val bitmap =
            Utilities.drawableToBitmap(extractee)
        if (bitmap == null) {
            onExitLoop()
            return
        }
        if (!bitmap.hasAlpha()) {
            isFullBleed = true
            fullBleedChecked = true
        }
        val size = height * width
        val rgbScoreHistogram =
            SparseIntArray(NUMBER_OF_COLORS_GUESSTIMATE)
        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        /*
         *   Calculate the number of padding pixels around the actual icon (i)
         *   +----------------+
         *   |      top       |
         *   +---+--------+---+
         *   |   |        |   |
         *   | l |    i   | r |
         *   |   |        |   |
         *   +---+--------+---+
         *   |     bottom     |
         *   +----------------+
         */
        val adjHeight = height - bounds.top - bounds.bottom
        val l = bounds.left * width * adjHeight
        val top = bounds.top * height * width
        val r = bounds.right * width * adjHeight
        val bottom = bounds.bottom * height * width
        val addPixels = Math.round(l + top + r + bottom)

        // Any icon with less than 10% transparent pixels (padding excluded) is considered "full-bleed-ish"
        val maxTransparent = (Math.round(size * .10) + addPixels).toInt()
        // Any icon with less than 27% transparent pixels (padding excluded) doesn't need a color mix-in
        val noMixinScore = (Math.round(size * .27) + addPixels).toInt()
        var highScore = 0
        var bestRGB = 0
        var transparentScore = 0
        for (pixel in pixels) {
            val alpha = 0xFF and (pixel shr 24)
            if (alpha < MIN_VISIBLE_ALPHA) {
                // Drop mostly-transparent pixels.
                transparentScore++
                if (transparentScore > maxTransparent) {
                    isFullBleed = false
                    fullBleedChecked = true
                }
                continue
            }
            // Reduce color complexity.
            val rgb: Int = ColorExtractor.posterize(pixel)
            if (rgb < 0) {
                // Defensively avoid array bounds violations.
                continue
            }
            val currentScore = rgbScoreHistogram[rgb] + 1
            rgbScoreHistogram.append(rgb, currentScore)
            if (currentScore > highScore) {
                highScore = currentScore
                bestRGB = rgb
            }
        }
        // add back the alpha channel
        bestRGB = bestRGB or (0xff shl 24)

        // not yet checked = not set to false = has to be full bleed, isBackgroundWhite = true = is adaptive
        isFullBleed = isFullBleed or (!fullBleedChecked && !isBackgroundWhite)

        // return early if a mix-in isnt needed
        noMixinNeeded =
            !isFullBleed && !isBackgroundWhite && almostSquarish && transparentScore <= noMixinScore

        // Currently, it's set to true so a white background is used for all the icons.
        if (useWhiteBackground) {
            //backgroundColor = Color.WHITE;
            backgroundColor = Color.WHITE and -0x7f000001
            onExitLoop()
            return
        }
        if (isFullBleed || noMixinNeeded) {
            backgroundColor = bestRGB
            onExitLoop()
            return
        }

        // "single color"
        val numColors = rgbScoreHistogram.size()
        val singleColor =
            numColors <= SINGLE_COLOR_LIMIT

        // Convert to HSL to get the lightness and adjust the color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(bestRGB, hsl)
        val lightness = hsl[2]
        val light = lightness > .5
        // Apply dark background to mostly white icons
        val veryLight = lightness > .75 && singleColor
        // Apply light background to mostly dark icons
        val veryDark = lightness < .35 && singleColor

        // Adjust color to reach suitable contrast depending on the relationship between the colors
        val opaqueSize = size - transparentScore
        val pxPerColor = opaqueSize / numColors.toFloat()
        val mixRatio =
            Math.min(Math.max(pxPerColor / highScore, .15f), .7f)

        // Vary color mix-in based on lightness and amount of colors
        val fill = if (light && !veryLight || veryDark) -0x1 else -0xcccccd
        backgroundColor = ColorUtils.blendARGB(bestRGB, fill, mixRatio)
        onExitLoop()
    }

    private fun onExitLoop() {
        ranLoop = true
        result = genResult()
    }

    private fun genResult(): Drawable {
        val tmp = AdaptiveIconCompat(
            ColorDrawable(),
            FixedScaleDrawable()
        )
        (tmp.getForeground() as FixedScaleDrawable).setDrawable(icon)
        if (isFullBleed || noMixinNeeded) {
            val scale: Float
            scale = if (noMixinNeeded) {
                val upScale = Math.min(width / aWidth, height / aHeight)
                NO_MIXIN_ICON_SCALE * upScale
            } else {
                val upScale = Math.max(width / aWidth, height / aHeight)
                FULL_BLEED_ICON_SCALE * upScale
            }
            (tmp.getForeground() as FixedScaleDrawable).setScale(scale)
        } else {
            (tmp.getForeground() as FixedScaleDrawable).setScale(scale)
        }
        (tmp.getBackground() as ColorDrawable).color = backgroundColor
        return tmp
    }

    fun getResult(): Drawable? {
        if (!ranLoop) {
            loop()
        }
        return result
    }

    companion object {
        // Average number of derived colors (based on averages with ~100 icons and performance testing)
        private const val NUMBER_OF_COLORS_GUESSTIMATE = 45

        // Found after some experimenting, might be improved with some more testing
        private const val FULL_BLEED_ICON_SCALE = 1.44f

        // Found after some experimenting, might be improved with some more testing
        private const val NO_MIXIN_ICON_SCALE = 1.40f

        // Icons with less than 5 colors are considered as "single color"
        private const val SINGLE_COLOR_LIMIT = 5

        // Minimal alpha to be considered opaque
        private const val MIN_VISIBLE_ALPHA = 0xEF
    }
}
