package foundation.e.blisslauncher.views

import android.graphics.Rect
/**
 * Allows the implementing [View] to not draw underneath system bars.
 * e.g., notification bar on top and home key area on the bottom.
 */
interface Insettable {
    fun setInsets(insets: Rect)
}