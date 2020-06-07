package foundation.e.blisslauncher.views.pageindicators

/**
 * Interface for a page indicator.
 */
interface PageIndicator {
    fun setScroll(currentScroll: Int, totalScroll: Int)
    fun setActiveMarker(activePage: Int)
    fun setMarkersCount(numMarkers: Int)
}