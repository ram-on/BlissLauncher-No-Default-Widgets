package foundation.e.blisslauncher.data.graphics

import android.graphics.Bitmap
import foundation.e.blisslauncher.domain.entity.LauncherItemWithIcon

open class BitmapInfo {

    var icon: Bitmap? = null
    var color = 0
    fun applyTo(info: LauncherItemWithIcon) {
        info.iconBitmap = icon
        info.iconColor = color
    }

    fun applyTo(info: BitmapInfo) {
        info.icon = icon
        info.color = color
    }

    companion object {
        fun fromBitmap(bitmap: Bitmap?): BitmapInfo {
            val info = BitmapInfo()
            info.icon = bitmap
            //info.color = ColorExtractor.findDominantColorByHue(bitmap)
            return info
        }
    }
}