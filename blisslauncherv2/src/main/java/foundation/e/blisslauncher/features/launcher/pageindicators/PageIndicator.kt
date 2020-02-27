package foundation.e.blisslauncher.features.launcher.pageindicators

/**
 * Base class for a page indicator.
 */
interface PageIndicator {
    fun setScroll(currentScroll: Int, totalScroll: Int)
    fun setActiveMarker(activePage: Int)
    fun setMarkersCount(numMarkers: Int)
}