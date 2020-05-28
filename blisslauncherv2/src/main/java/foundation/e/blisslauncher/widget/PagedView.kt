package foundation.e.blisslauncher.widget

import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.touch.OverScroll
import foundation.e.blisslauncher.widget.pageindicators.PageIndicatorDots
import java.util.ArrayList
import kotlin.math.sin

typealias ComputePageScrollsLogic = (View) -> Boolean

open class PagedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var freeScroll = false
    private var settleOnPageInFreeScroll = false

    private val flingThresholdVelocity: Int
    private val minFlingVelocity: Int
    private val minSnapVelocity: Int

    protected var firstLayout = true

     var currentPage = 0
        set(value) {
            if (!scroller.isFinished) {
                abortScrollerAnimation(true)
            }
            // don't introduce any checks like currentPage == currentPage here-- if we change the
            // the default
            if (childCount == 0) {
                return
            }
            val prevPage: Int = currentPage
            field = validateNewPage(value)
            updateCurrentPageScroll()
            notifyPageSwitchListener(prevPage)
            invalidate()
        }

    var nextPage: Int = INVALID_PAGE
        get() {
            return if (field != INVALID_PAGE) field else currentPage
        }

    protected var maxScrollX = 0
    protected val scroller: Scroller = Scroller(context)
    private var velocityTracker: VelocityTracker? = null
    protected var pageSpacing = 0
        set(value) {
            field = value
            requestLayout()
        }

    private var downMotionX = 0f
    private var downMotionY = 0f
    private var lastMotionX = 0f
    private var lastMotionXRemainder = 0f
    private var totalMotionX = 0f

    protected var pageScrolls: IntArray = IntArray(0)

    protected var touchState: Int = TOUCH_STATE_REST

    private val touchSlop: Int
    private val maximumVelocity: Int
    protected var mAllowOverScroll = true

    protected val INVALID_POINTER = -1

    protected var activePointerId = INVALID_POINTER

    protected var isPageInTransition = false

    protected var wasInOverscroll = false

    protected var overScrollX = 0

    protected var unboundedScrollX = 0

    protected var pageIndicator: PageIndicatorDots? = null

    // Convenience/caching
    private val sTmpInvMatrix = Matrix()
    private val sTmpPoint = FloatArray(2)
    private val sTmpRect = Rect()

    protected val insets = Rect()
    protected var isRtl = false

    // Similar to the platform implementation of isLayoutValid();
    protected var mIsLayoutValid = false

    init {
        isHapticFeedbackEnabled = false
        currentPage = 0
        val configuration = ViewConfiguration.get(getContext())
        touchSlop = configuration.scaledPagingTouchSlop
        maximumVelocity = configuration.scaledMaximumFlingVelocity

        val density = resources.displayMetrics.density
        flingThresholdVelocity = (FLING_THRESHOLD_VELOCITY * density).toInt()
        minFlingVelocity = (MIN_FLING_VELOCITY * density).toInt()
        minSnapVelocity = (MIN_SNAP_VELOCITY * density).toInt()
    }

    fun getPageCount() = childCount

    fun getPageAt(index: Int) = getChildAt(index)

    fun indexToPage(index: Int) = index

    fun scrollAndForceFinish(scrollX: Int) {
        scrollTo(scrollX, 0)
        scroller.finalX = scrollX
        forceFinishScroller(true)
    }

    private fun forceFinishScroller(resetNextPage: Boolean) {
        scroller.forceFinished(true)
        // We need to clean up the next page here to avoid computeScrollHelper from
        // updating current page on the pass.
        if (resetNextPage) {
            nextPage = INVALID_PAGE
            pageEndTransition()
        }
    }

    /**
     * Updates the scroll of the current page immediately to its final scroll position.  We use this
     * in CustomizePagedView to allow tabs to share the same PagedView while resetting the scroll of
     * the previous tab page.
     */
    private fun updateCurrentPageScroll() {
        // If the current page is invalid, just reset the scroll position to zero
        var newX = 0
        if (0 <= currentPage && currentPage < getPageCount()) {
            newX = getScrollForPage(currentPage)
        }
        scrollAndForceFinish(newX)
    }

    private fun abortScrollerAnimation(resetNextPage: Boolean) {
        scroller.abortAnimation()
        // We need to clean up the next page here to avoid computeScrollHelper from
        // updating current page on the pass.
        if (resetNextPage) {
            nextPage = INVALID_PAGE
            pageEndTransition()
        }
    }

    private fun validateNewPage(newPage: Int): Int {
        // Ensure that it is clamped by the actual set of children in all cases
        return Utilities.boundToRange(newPage, 0, getPageCount() - 1)
    }

    /**
     * Should be called whenever the page changes. In the case of a scroll, we wait until the page
     * has settled.
     */
    protected fun notifyPageSwitchListener(prevPage: Int) {
        updatePageIndicator()
    }

    private fun updatePageIndicator() {
        if (pageIndicator != null) {
            pageIndicator!!.setActiveMarker(nextPage)
        }
    }

    protected fun pageBeginTransition() {
        if (!isPageInTransition) {
            isPageInTransition = true
            onPageBeginTransition()
        }
    }

    protected fun pageEndTransition() {
        if (isPageInTransition) {
            isPageInTransition = false
            onPageEndTransition()
        }
    }

    /**
     * Called when the page starts moving as part of the scroll. Subclasses can override this
     * to provide custom behavior during animation.
     */
    protected fun onPageBeginTransition() {}

    /**
     * Called when the page ends moving as part of the scroll. Subclasses can override this
     * to provide custom behavior during animation.
     */
    protected fun onPageEndTransition() {
        wasInOverscroll = false
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollTo(unboundedScrollX + x, scrollY + y)
    }

    override fun scrollTo(x: Int, y: Int) {
        // In free scroll mode, we clamp the scrollX
        var x = x
        if (freeScroll) {
            // If the scroller is trying to move to a location beyond the maximum allowed
            // in the free scroll mode, we make sure to end the scroll operation.
            if (!scroller.isFinished && (x > maxScrollX || x < 0)) {
                forceFinishScroller(false)
            }
            x = Utilities.boundToRange(x, 0, maxScrollX)
        }
        unboundedScrollX = x
        val isXBeforeFirstPage = if (isRtl) x > maxScrollX else x < 0
        val isXAfterLastPage = if (isRtl) x < 0 else x > maxScrollX
        if (isXBeforeFirstPage) {
            super.scrollTo(if (isRtl) maxScrollX else 0, y)
            if (mAllowOverScroll) {
                wasInOverscroll = true
                if (isRtl) {
                    overScroll(x - maxScrollX.toFloat())
                } else {
                    overScroll(x.toFloat())
                }
            }
        } else if (isXAfterLastPage) {
            super.scrollTo(if (isRtl) 0 else maxScrollX, y)
            if (mAllowOverScroll) {
                wasInOverscroll = true
                if (isRtl) {
                    overScroll(x.toFloat())
                } else {
                    overScroll(x - maxScrollX.toFloat())
                }
            }
        } else {
            if (wasInOverscroll) {
                overScroll(0f)
                wasInOverscroll = false
            }
            overScrollX = x
            super.scrollTo(x, y)
        }
    }

    // we moved this functionality to a helper function so SmoothPagedView can reuse it
    protected fun computeScrollHelper(): Boolean {
        return computeScrollHelper(true)
    }

    protected fun computeScrollHelper(shouldInvalidate: Boolean): Boolean {
        if (scroller.computeScrollOffset()) {
            // Don't bother scrolling if the page does not need to be moved
            if (unboundedScrollX != scroller.getCurrX() || scrollY != scroller.getCurrY() || overScrollX != scroller.getCurrX()
            ) {
                scrollTo(scroller.getCurrX(), scroller.getCurrY())
            }
            if (shouldInvalidate) {
                invalidate()
            }
            return true
        } else if (nextPage != INVALID_PAGE && shouldInvalidate) {
            val prevPage: Int = currentPage
            currentPage = validateNewPage(nextPage)
            nextPage = INVALID_PAGE
            notifyPageSwitchListener(prevPage)

            // We don't want to trigger a page end moving unless the page has settled
            // and the user has stopped scrolling
            if (touchState == TOUCH_STATE_REST) {
                pageEndTransition()
            }
        }
        return false
    }

    override fun computeScroll() {
        computeScrollHelper()
    }

    fun getExpectedHeight(): Int {
        return measuredHeight
    }

    fun getNormalChildHeight(): Int {
        return (getExpectedHeight() - paddingTop - paddingBottom -
            insets.top - insets.bottom)
    }

    fun getExpectedWidth(): Int {
        return measuredWidth
    }

    fun getNormalChildWidth(): Int {
        return (getExpectedWidth() - paddingLeft - paddingRight -
            insets.left - insets.right)
    }

    override fun requestLayout() {
        mIsLayoutValid = false
        super.requestLayout()
    }

    override fun forceLayout() {
        mIsLayoutValid = false
        super.forceLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // We measure the dimensions of the PagedView to be larger than the pages so that when we
        // zoom out (and scale down), the view is still contained in the parent
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // Return early if we aren't given a proper dimension
        if (widthSize <= 0 || heightSize <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        Log.d(
            TAG,
            "PagedView.onMeasure(): $widthSize, $heightSize"
        )
        val myWidthSpec = MeasureSpec.makeMeasureSpec(
            widthSize - insets.left - insets.right, MeasureSpec.EXACTLY
        )
        val myHeightSpec = MeasureSpec.makeMeasureSpec(
            heightSize - insets.top - insets.bottom, MeasureSpec.EXACTLY
        )

        // measureChildren takes accounts for content padding, we only need to care about extra
        // space due to insets.
        measureChildren(myWidthSpec, myHeightSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    protected fun restoreScrollOnLayout() {
        currentPage = nextPage
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        mIsLayoutValid = true
        val childCount = childCount
        var pageScrollChanged = false
        if (childCount != pageScrolls.size) {
            pageScrolls = IntArray(childCount)
            pageScrollChanged = true
        }
        if (childCount == 0) {
            return
        }
        Log.d(TAG, "PagedView.onLayout()")
        if (getPageScrolls(pageScrolls, true, SIMPLE_SCROLL_LOGIC)) {
            pageScrollChanged = true
        }
        val transition = layoutTransition
        // If the transition is running defer updating max scroll, as some empty pages could
        // still be present, and a max scroll change could cause sudden jumps in scroll.
        if (transition != null && transition.isRunning) {
            transition.addTransitionListener(object : LayoutTransition.TransitionListener {
                override fun startTransition(
                    transition: LayoutTransition,
                    container: ViewGroup,
                    view: View,
                    transitionType: Int
                ) {
                }

                override fun endTransition(
                    transition: LayoutTransition,
                    container: ViewGroup,
                    view: View,
                    transitionType: Int
                ) {
                    // Wait until all transitions are complete.
                    if (!transition.isRunning) {
                        transition.removeTransitionListener(this)
                        updateMaxScrollX()
                    }
                }
            })
        } else {
            updateMaxScrollX()
        }
        if (firstLayout && currentPage >= 0 && currentPage < childCount) {
            updateCurrentPageScroll()
            firstLayout = false
        }
        if (scroller.isFinished() && pageScrollChanged) {
            restoreScrollOnLayout()
        }
    }

    val SIMPLE_SCROLL_LOGIC: ComputePageScrollsLogic = { v -> v.visibility != View.GONE }

    /**
     * Initializes `outPageScrolls` with scroll positions for view at that index. The length
     * of `outPageScrolls` should be same as the the childCount
     *
     */
    protected fun getPageScrolls(
        outPageScrolls: IntArray,
        layoutChildren: Boolean,
        scrollLogic: ComputePageScrollsLogic
    ): Boolean {
        val childCount = childCount
        val startIndex = if (isRtl) childCount - 1 else 0
        val endIndex = if (isRtl) -1 else childCount
        val delta = if (isRtl) -1 else 1
        val verticalCenter =
            (paddingTop + measuredHeight + insets.top - insets.bottom - paddingBottom) / 2
        val scrollOffsetLeft = insets.left + paddingLeft
        var pageScrollChanged = false
        var i = startIndex
        var childLeft = scrollOffsetLeft + offsetForPageScrolls()
        while (i != endIndex) {
            val child = getPageAt(i)
            if (scrollLogic(child)) {
                val childTop = verticalCenter - child.measuredHeight / 2
                val childWidth = child.measuredWidth
                if (layoutChildren) {
                    val childHeight = child.measuredHeight
                    child.layout(
                        childLeft, childTop,
                        childLeft + child.measuredWidth, childTop + childHeight
                    )
                }
                val pageScroll = childLeft - scrollOffsetLeft
                if (outPageScrolls[i] != pageScroll) {
                    pageScrollChanged = true
                    outPageScrolls[i] = pageScroll
                }
                childLeft += childWidth + pageSpacing + getChildGap()
            }
            i += delta
        }
        return pageScrollChanged
    }

    protected fun getChildGap(): Int {
        return 0
    }

    private fun updateMaxScrollX() {
        maxScrollX = computeMaxScrollX()
    }

    protected fun computeMaxScrollX(): Int {
        val childCount = childCount
        return if (childCount > 0) {
            val index = if (isRtl) 0 else childCount - 1
            getScrollForPage(index)
        } else {
            0
        }
    }

    protected fun offsetForPageScrolls(): Int {
        return 0
    }

    private fun dispatchPageCountChanged() {
        if (pageIndicator != null) {
            pageIndicator!!.setMarkersCount(childCount)
        }
        // This ensures that when children are added, they get the correct transforms / alphas
        // in accordance with any scroll effects.
        invalidate()
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        dispatchPageCountChanged()
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        currentPage = validateNewPage(currentPage)
        dispatchPageCountChanged()
    }

    protected fun getChildOffset(index: Int): Int {
        return if (index < 0 || index > childCount - 1) 0 else getPageAt(index).left
    }

    override fun requestChildRectangleOnScreen(
        child: View?,
        rectangle: Rect?,
        immediate: Boolean
    ): Boolean {
        val page = indexToPage(indexOfChild(child))
        if (page != currentPage || !scroller.isFinished()) {
            if (immediate) {
                currentPage = page
            } else {
                snapToPage(page)
            }
            return true
        }
        return false
    }

    override fun onRequestFocusInDescendants(
        direction: Int,
        previouslyFocusedRect: Rect?
    ): Boolean {
        val focusablePage: Int
        if (nextPage != INVALID_PAGE) {
            focusablePage = nextPage
        } else {
            focusablePage = currentPage
        }
        val v = getPageAt(focusablePage)
        return v?.requestFocus(direction, previouslyFocusedRect) ?: false
    }

    override fun dispatchUnhandledMove(focused: View?, direction: Int): Boolean {
        var direction = direction
        if (super.dispatchUnhandledMove(focused, direction)) {
            return true
        }
        if (isRtl) {
            if (direction == View.FOCUS_LEFT) {
                direction = View.FOCUS_RIGHT
            } else if (direction == View.FOCUS_RIGHT) {
                direction = View.FOCUS_LEFT
            }
        }
        if (direction == View.FOCUS_LEFT) {
            if (currentPage > 0) {
                snapToPage(currentPage - 1)
                return true
            }
        } else if (direction == View.FOCUS_RIGHT) {
            if (currentPage < getPageCount() - 1) {
                snapToPage(currentPage + 1)
                return true
            }
        }
        return false
    }

    override fun addFocusables(
        views: ArrayList<View?>?,
        direction: Int,
        focusableMode: Int
    ) {
        if (descendantFocusability == FOCUS_BLOCK_DESCENDANTS) {
            return
        }

        // XXX-RTL: This will be fixed in a future CL
        if (currentPage >= 0 && currentPage < getPageCount()) {
            getPageAt(currentPage).addFocusables(views, direction, focusableMode)
        }
        if (direction == View.FOCUS_LEFT) {
            if (currentPage > 0) {
                getPageAt(currentPage - 1).addFocusables(views, direction, focusableMode)
            }
        } else if (direction == View.FOCUS_RIGHT) {
            if (currentPage < getPageCount() - 1) {
                getPageAt(currentPage + 1).addFocusables(views, direction, focusableMode)
            }
        }
    }

    /**
     * If one of our descendant views decides that it could be focused now, only
     * pass that along if it's on the current page.
     *
     * This happens when live folders requery, and if they're off page, they
     * end up calling requestFocus, which pulls it on page.
     */
    override fun focusableViewAvailable(focused: View) {
        val current = getPageAt(currentPage)
        var v = focused
        while (true) {
            if (v === current) {
                super.focusableViewAvailable(focused)
                return
            }
            if (v === this) {
                return
            }
            val parent = v.parent
            v = if (parent is View) {
                v.parent as View
            } else {
                return
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            // We need to make sure to cancel our long press if
            // a scrollable widget takes over touch events
            val currentPage = getPageAt(currentPage)
            currentPage.cancelLongPress()
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    /** Returns whether x and y originated within the buffered viewport  */
    private fun isTouchPointInViewportWithBuffer(x: Int, y: Int): Boolean {
        sTmpRect.set(
            -measuredWidth / 2,
            0,
            3 * measuredWidth / 2,
            measuredHeight
        )
        return sTmpRect.contains(x, y)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */
        acquireVelocityTrackerAndAddMovement(ev)

        // Skip touch handling if there are no pages to swipe
        if (childCount <= 0) return super.onInterceptTouchEvent(ev)

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        val action = ev.action
        if (action == MotionEvent.ACTION_MOVE &&
            touchState == TOUCH_STATE_SCROLLING
        ) {
            return true
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {

                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */if (activePointerId != INVALID_POINTER) {
                    determineScrollingStart(ev)
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y
                // Remember location of down touch
                downMotionX = x
                downMotionY = y
                lastMotionX = x
                lastMotionXRemainder = 0f
                totalMotionX = 0f
                activePointerId = ev.getPointerId(0)

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  scroller.isFinished should be false when
                 * being flinged.
                 */
                val xDist: Int = Math.abs(scroller.getFinalX() - scroller.getCurrX())
                val finishedScrolling =
                    scroller.isFinished() || xDist < touchSlop / 3
                if (finishedScrolling) {
                    touchState = TOUCH_STATE_REST
                    if (!scroller.isFinished() && !freeScroll) {
                        currentPage = nextPage
                        pageEndTransition()
                    }
                } else {
                    touchState = if (isTouchPointInViewportWithBuffer(
                            downMotionX.toInt(),
                            downMotionY.toInt()
                        )
                    ) {
                        TOUCH_STATE_SCROLLING
                    } else {
                        TOUCH_STATE_REST
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> resetTouchState()
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                releaseVelocityTracker()
            }
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */return touchState != TOUCH_STATE_REST
    }

    fun isHandlingTouch(): Boolean {
        return touchState != TOUCH_STATE_REST
    }

    fun determineScrollingStart(ev: MotionEvent) {
        determineScrollingStart(ev, 1.0f)
    }

    /*
     * Determines if we should change the touch state to start scrolling after the
     * user moves their touch point too far.
     */
    protected fun determineScrollingStart(
        ev: MotionEvent,
        touchSlopScale: Float
    ) {
        // Disallow scrolling if we don't have a valid pointer index
        val pointerIndex = ev.findPointerIndex(activePointerId)
        if (pointerIndex == -1) return

        // Disallow scrolling if we started the gesture from outside the viewport
        val x = ev.getX(pointerIndex)
        val y = ev.getY(pointerIndex)
        if (!isTouchPointInViewportWithBuffer(x.toInt(), y.toInt())) return
        val xDiff = Math.abs(x - lastMotionX).toInt()
        val touchSlop = Math.round(touchSlopScale * touchSlop).toInt()
        val xMoved = xDiff > touchSlop
        if (xMoved) {
            // Scroll if the user moved far enough along the X axis
            touchState = TOUCH_STATE_SCROLLING
            totalMotionX += Math.abs(lastMotionX - x)
            lastMotionX = x
            lastMotionXRemainder = 0f
            onScrollInteractionBegin()
            pageBeginTransition()
            // Stop listening for things like pinches.
            requestDisallowInterceptTouchEvent(true)
        }
    }

    protected fun cancelCurrentPageLongPress() {
        // Try canceling the long press. It could also have been scheduled
        // by a distant descendant, so use the mAllowLongPress flag to block
        // everything
        val currentPage = getPageAt(currentPage)
        currentPage?.cancelLongPress()
    }

    protected fun getScrollProgress(
        screenCenter: Int,
        v: View,
        page: Int
    ): Float {
        val halfScreenSize = measuredWidth / 2
        val delta = screenCenter - (getScrollForPage(page) + halfScreenSize)
        val count = childCount
        val totalDistance: Int
        var adjacentPage = page + 1
        if (delta < 0 && !isRtl || delta > 0 && isRtl) {
            adjacentPage = page - 1
        }
        totalDistance = if (adjacentPage < 0 || adjacentPage > count - 1) {
            v.measuredWidth + pageSpacing
        } else {
            Math.abs(getScrollForPage(adjacentPage) - getScrollForPage(page))
        }
        var scrollProgress = delta / (totalDistance * 1.0f)
        scrollProgress =
            Math.min(scrollProgress, MAX_SCROLL_PROGRESS)
        scrollProgress =
            Math.max(scrollProgress, -MAX_SCROLL_PROGRESS)
        return scrollProgress
    }

    fun getScrollForPage(index: Int): Int {
        return if (index >= pageScrolls.size || index < 0) {
            0
        } else {
            pageScrolls[index]
        }
    }

    // While layout transitions are occurring, a child's position may stray from its baseline
    // position. This method returns the magnitude of this stray at any given time.
    fun getLayoutTransitionOffsetForPage(index: Int): Int {
        return if (index >= pageScrolls.size || index < 0) {
            0
        } else {
            val child = getChildAt(index)
            val scrollOffset = if (isRtl) paddingRight else paddingLeft
            val baselineX = pageScrolls[index] + scrollOffset
            (child.x - baselineX).toInt()
        }
    }

    protected fun dampedOverScroll(amount: Float) {
        if (java.lang.Float.compare(amount, 0f) == 0) return
        val overScrollAmount: Int = OverScroll.dampedScroll(amount, measuredWidth)
        if (amount < 0) {
            overScrollX = overScrollAmount
            super.scrollTo(overScrollX, scrollY)
        } else {
            overScrollX = maxScrollX + overScrollAmount
            super.scrollTo(overScrollX, scrollY)
        }
        invalidate()
    }

    protected fun overScroll(amount: Float) {
        dampedOverScroll(amount)
    }

    protected fun enableFreeScroll(settleOnPageInFreeScroll: Boolean) {
        setEnableFreeScroll(true)
        this.settleOnPageInFreeScroll = settleOnPageInFreeScroll
    }

    private fun setEnableFreeScroll(freeScroll: Boolean) {
        val wasFreeScroll = this.freeScroll
        this.freeScroll = freeScroll
        if (this.freeScroll) {
            currentPage = nextPage
        } else if (wasFreeScroll) {
            snapToPage(nextPage)
        }
        setEnableOverscroll(!freeScroll)
    }

    protected fun setEnableOverscroll(enable: Boolean) {
        mAllowOverScroll = enable
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        super.onTouchEvent(ev)

        // Skip touch handling if there are no pages to swipe
        if (childCount <= 0) return super.onTouchEvent(ev)
        acquireVelocityTrackerAndAddMovement(ev)
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */if (!scroller.isFinished()) {
                    abortScrollerAnimation(false)
                }
                run {
                    lastMotionX = ev.x
                    downMotionX = lastMotionX
                }
                downMotionY = ev.y
                lastMotionXRemainder = 0f
                totalMotionX = 0f
                activePointerId = ev.getPointerId(0)
                if (touchState == TOUCH_STATE_SCROLLING) {
                    onScrollInteractionBegin()
                    pageBeginTransition()
                }
            }
            MotionEvent.ACTION_MOVE -> if (touchState == TOUCH_STATE_SCROLLING) {
                // Scroll to follow the motion event
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return true
                val x = ev.getX(pointerIndex)
                val deltaX = lastMotionX + lastMotionXRemainder - x
                totalMotionX += Math.abs(deltaX)

                // Only scroll and update mLastMotionX if we have moved some discrete amount.  We
                // keep the remainder because we are actually testing if we've moved from the last
                // scrolled position (which is discrete).
                if (Math.abs(deltaX) >= 1.0f) {
                    scrollBy(deltaX.toInt(), 0)
                    lastMotionX = x
                    lastMotionXRemainder = deltaX - deltaX.toInt()
                } else {
                    awakenScrollBars()
                }
            } else {
                determineScrollingStart(ev)
            }
            MotionEvent.ACTION_UP -> {
                if (touchState == TOUCH_STATE_SCROLLING) {
                    val activePointerId = activePointerId
                    val pointerIndex = ev.findPointerIndex(activePointerId)
                    val x = ev.getX(pointerIndex)
                    val velocityTracker = velocityTracker!!
                    velocityTracker.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                    val velocityX = velocityTracker.getXVelocity(activePointerId).toInt()
                    val deltaX = (x - downMotionX).toInt()
                    val pageWidth = getPageAt(currentPage).measuredWidth
                    val isSignificantMove: Boolean = Math.abs(deltaX) > pageWidth *
                        SIGNIFICANT_MOVE_THRESHOLD
                    totalMotionX += Math.abs(lastMotionX + lastMotionXRemainder - x)
                    val isFling =
                        totalMotionX > touchSlop && shouldFlingForVelocity(velocityX)
                    if (!freeScroll) {
                        // In the case that the page is moved far to one direction and then is flung
                        // in the opposite direction, we use a threshold to determine whether we should
                        // just return to the starting page, or if we should skip one further.
                        var returnToOriginalPage = false
                        if (Math.abs(deltaX) > pageWidth * RETURN_TO_ORIGINAL_PAGE_THRESHOLD && Math.signum(
                                velocityX.toFloat()
                            ) != Math.signum(
                                deltaX.toFloat()
                            ) && isFling
                        ) {
                            returnToOriginalPage = true
                        }
                        val finalPage: Int
                        // We give flings precedence over large moves, which is why we short-circuit our
                        // test for a large move if a fling has been registered. That is, a large
                        // move to the left and fling to the right will register as a fling to the right.
                        val isDeltaXLeft = if (isRtl) deltaX > 0 else deltaX < 0
                        val isVelocityXLeft =
                            if (isRtl) velocityX > 0 else velocityX < 0
                        if ((isSignificantMove && !isDeltaXLeft && !isFling ||
                                isFling && !isVelocityXLeft) && currentPage > 0
                        ) {
                            finalPage = if (returnToOriginalPage) currentPage else currentPage - 1
                            snapToPageWithVelocity(finalPage, velocityX)
                        } else if ((isSignificantMove && isDeltaXLeft && !isFling ||
                                isFling && isVelocityXLeft) &&
                            currentPage < childCount - 1
                        ) {
                            finalPage = if (returnToOriginalPage) currentPage else currentPage + 1
                            snapToPageWithVelocity(finalPage, velocityX)
                        } else {
                            snapToDestination()
                        }
                    } else {
                        if (!scroller.isFinished()) {
                            abortScrollerAnimation(true)
                        }
                        val scaleX = scaleX
                        val vX = (-velocityX * scaleX).toInt()
                        val initialScrollX = (scrollX * scaleX).toInt()
                        scroller.fling(
                            initialScrollX,
                            scrollY,
                            vX,
                            0,
                            Int.MIN_VALUE,
                            Int.MAX_VALUE,
                            0,
                            0
                        )
                        val unscaledScrollX = (scroller.getFinalX() / scaleX) as Int
                        nextPage = getPageNearestToCenterOfScreen(unscaledScrollX)
                        val firstPageScroll =
                            getScrollForPage(if (!isRtl) 0 else getPageCount() - 1)
                        val lastPageScroll =
                            getScrollForPage(if (!isRtl) getPageCount() - 1 else 0)
                        if (settleOnPageInFreeScroll && unscaledScrollX > 0 && unscaledScrollX < maxScrollX
                        ) {
                            // If scrolling ends in the half of the added space that is closer to the
                            // end, settle to the end. Otherwise snap to the nearest page.
                            // If flinging past one of the ends, don't change the velocity as it will
                            // get stopped at the end anyway.
                            val finalX =
                                if (unscaledScrollX < firstPageScroll / 2) 0 else if (unscaledScrollX > (lastPageScroll + maxScrollX) / 2) maxScrollX else getScrollForPage(
                                    nextPage
                                )
                            scroller.setFinalX((finalX * getScaleX()).toInt())
                            // Ensure the scroll/snap doesn't happen too fast;
                            val extraScrollDuration: Int =
                                (OVERSCROLL_PAGE_SNAP_ANIMATION_DURATION -
                                    scroller.getDuration())
                            if (extraScrollDuration > 0) {
                                scroller.extendDuration(extraScrollDuration)
                            }
                        }
                        invalidate()
                    }
                    onScrollInteractionEnd()
                } else if (touchState == TOUCH_STATE_PREV_PAGE) {
                    // at this point we have not moved beyond the touch slop
                    // (otherwise mTouchState would be TOUCH_STATE_SCROLLING), so
                    // we can just page
                    val nextPage = Math.max(0, currentPage - 1)
                    if (nextPage != currentPage) {
                        snapToPage(nextPage)
                    } else {
                        snapToDestination()
                    }
                } else if (touchState == TOUCH_STATE_NEXT_PAGE) {
                    // at this point we have not moved beyond the touch slop
                    // (otherwise mTouchState would be TOUCH_STATE_SCROLLING), so
                    // we can just page
                    val nextPage = Math.min(childCount - 1, currentPage + 1)
                    if (nextPage != currentPage) {
                        snapToPage(nextPage)
                    } else {
                        snapToDestination()
                    }
                }

                // End any intermediate reordering states
                resetTouchState()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (touchState == TOUCH_STATE_SCROLLING) {
                    snapToDestination()
                    onScrollInteractionEnd()
                }
                resetTouchState()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                releaseVelocityTracker()
            }
        }
        return true
    }

    protected fun shouldFlingForVelocity(velocityX: Int): Boolean {
        return Math.abs(velocityX) > flingThresholdVelocity
    }

    private fun resetTouchState() {
        releaseVelocityTracker()
        touchState = TOUCH_STATE_REST
        activePointerId = INVALID_POINTER
    }

    /**
     * Triggered by scrolling via touch
     */
    protected fun onScrollInteractionBegin() {}

    protected fun onScrollInteractionEnd() {}

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {

                    // Handle mouse (or ext. device) by shifting the page depending on the scroll
                    val vscroll: Float
                    val hscroll: Float
                    if (event.metaState and KeyEvent.META_SHIFT_ON != 0) {
                        vscroll = 0f
                        hscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    } else {
                        vscroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                    }
                    if (hscroll != 0f || vscroll != 0f) {
                        val isForwardScroll =
                            if (isRtl) hscroll < 0 || vscroll < 0 else hscroll > 0 || vscroll > 0
                        if (isForwardScroll) {
                            scrollRight()
                        } else {
                            scrollLeft()
                        }
                        return true
                    }
                }
            }
        }
        return super.onGenericMotionEvent(event)
    }

    private fun acquireVelocityTrackerAndAddMovement(ev: MotionEvent) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(ev)
    }

    private fun releaseVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker!!.clear()
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex =
            ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr
                MotionEvent.ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            downMotionX = ev.getX(newPointerIndex)
            lastMotionX = downMotionX
            lastMotionXRemainder = 0f
            activePointerId = ev.getPointerId(newPointerIndex)
            if (velocityTracker != null) {
                velocityTracker!!.clear()
            }
        }
    }

    override fun requestChildFocus(
        child: View?,
        focused: View?
    ) {
        super.requestChildFocus(child, focused)
        val page = indexToPage(indexOfChild(child))
        if (page >= 0 && page != currentPage && !isInTouchMode) {
            snapToPage(page)
        }
    }

    fun getPageNearestToCenterOfScreen(): Int {
        return getPageNearestToCenterOfScreen(scrollX)
    }

    private fun getPageNearestToCenterOfScreen(scaledScrollX: Int): Int {
        val screenCenter = scaledScrollX + measuredWidth / 2
        var minDistanceFromScreenCenter = Int.MAX_VALUE
        var minDistanceFromScreenCenterIndex = -1
        val childCount = childCount
        for (i in 0 until childCount) {
            val layout = getPageAt(i)
            val childWidth = layout.measuredWidth
            val halfChildWidth = childWidth / 2
            val childCenter: Int = getChildOffset(i) + halfChildWidth
            val distanceFromScreenCenter = Math.abs(childCenter - screenCenter)
            if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                minDistanceFromScreenCenter = distanceFromScreenCenter
                minDistanceFromScreenCenterIndex = i
            }
        }
        return minDistanceFromScreenCenterIndex
    }

    protected fun snapToDestination() {
        snapToPage(getPageNearestToCenterOfScreen(), getPageSnapDuration())
    }

    protected fun isInOverScroll(): Boolean {
        return overScrollX > maxScrollX || overScrollX < 0
    }

    protected fun getPageSnapDuration(): Int {
        return if (isInOverScroll()) {
            OVERSCROLL_PAGE_SNAP_ANIMATION_DURATION
        } else PAGE_SNAP_ANIMATION_DURATION
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    private fun distanceInfluenceForSnapDuration(f: Float): Float {
        var f = f
        f -= 0.5f // center the values about 0.
        f *= (0.3f * Math.PI / 2.0f).toFloat()
        return sin(f.toDouble()).toFloat()
    }

    protected fun snapToPageWithVelocity(whichPage: Int, velocity: Int): Boolean {
        var whichPage = whichPage
        var velocity = velocity
        whichPage = validateNewPage(whichPage)
        val halfScreenSize = measuredWidth / 2
        val newX: Int = getScrollForPage(whichPage)
        val delta: Int = newX - unboundedScrollX
        var duration = 0
        if (Math.abs(velocity) < minFlingVelocity) {
            // If the velocity is low enough, then treat this more as an automatic page advance
            // as opposed to an apparent physical response to flinging
            return snapToPage(
                whichPage,
                PAGE_SNAP_ANIMATION_DURATION
            )
        }

        // Here we compute a "distance" that will be used in the computation of the overall
        // snap duration. This is a function of the actual distance that needs to be traveled;
        // we keep this value close to half screen size in order to reduce the variance in snap
        // duration as a function of the distance the page needs to travel.
        val distanceRatio =
            Math.min(1f, 1.0f * Math.abs(delta) / (2 * halfScreenSize))
        val distance = halfScreenSize + halfScreenSize *
            distanceInfluenceForSnapDuration(distanceRatio)
        velocity = Math.abs(velocity)
        velocity = Math.max(minSnapVelocity, velocity)

        // we want the page's snap velocity to approximately match the velocity at which the
        // user flings, so we scale the duration by a value near to the derivative of the scroll
        // interpolator at zero, ie. 5. We use 4 to make it a little slower.
        duration = 4 * Math.round(1000 * Math.abs(distance / velocity))
        return snapToPage(whichPage, delta, duration)
    }

    fun snapToPage(whichPage: Int): Boolean {
        return snapToPage(whichPage, PAGE_SNAP_ANIMATION_DURATION)
    }

    fun snapToPageImmediately(whichPage: Int): Boolean {
        return snapToPage(
            whichPage,
            PAGE_SNAP_ANIMATION_DURATION,
            true,
            null
        )
    }

    fun snapToPage(whichPage: Int, duration: Int): Boolean {
        return snapToPage(whichPage, duration, false, null)
    }

    fun snapToPage(
        whichPage: Int,
        duration: Int,
        interpolator: TimeInterpolator?
    ): Boolean {
        return snapToPage(whichPage, duration, false, interpolator)
    }

    protected fun snapToPage(
        whichPage: Int,
        duration: Int,
        immediate: Boolean,
        interpolator: TimeInterpolator?
    ): Boolean {
        var whichPage = whichPage
        whichPage = validateNewPage(whichPage)
        val newX: Int = getScrollForPage(whichPage)
        val delta: Int = newX - unboundedScrollX
        return snapToPage(whichPage, delta, duration, immediate, interpolator)
    }

    protected fun snapToPage(whichPage: Int, delta: Int, duration: Int): Boolean {
        return snapToPage(whichPage, delta, duration, false, null)
    }

    protected fun snapToPage(
        whichPage: Int,
        delta: Int,
        duration: Int,
        immediate: Boolean,
        interpolator: TimeInterpolator?
    ): Boolean {
        var whichPage = whichPage
        var duration = duration
        if (firstLayout) {
            currentPage = whichPage
            return false
        }
        whichPage = validateNewPage(whichPage)
        nextPage = whichPage
        awakenScrollBars(duration)
        if (immediate) {
            duration = 0
        } else if (duration == 0) {
            duration = Math.abs(delta)
        }
        if (duration != 0) {
            pageBeginTransition()
        }
        if (!scroller.isFinished()) {
            abortScrollerAnimation(false)
        }
        scroller.startScroll(unboundedScrollX, 0, delta, 0, duration)
        updatePageIndicator()

        // Trigger a compute() to finish switching pages if necessary
        if (immediate) {
            computeScroll()
            pageEndTransition()
        }
        invalidate()
        return Math.abs(delta) > 0
    }

    fun scrollLeft(): Boolean {
        if (nextPage > 0) {
            snapToPage(nextPage - 1)
            return true
        }
        return false
    }

    fun scrollRight(): Boolean {
        if (nextPage < childCount - 1) {
            snapToPage(nextPage + 1)
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "PagedView"

        const val INVALID_PAGE = -1

        const val PAGE_SNAP_ANIMATION_DURATION = 750
        private const val OVERSCROLL_PAGE_SNAP_ANIMATION_DURATION = 270

        private const val RETURN_TO_ORIGINAL_PAGE_THRESHOLD = 0.33f

        // Move to next page on touch up if page is moved more than this threshold
        private const val SIGNIFICANT_MOVE_THRESHOLD = 0.4f

        private const val MAX_SCROLL_PROGRESS = 1.0f

        // Scaled based on density
        private const val FLING_THRESHOLD_VELOCITY = 500
        private const val MIN_SNAP_VELOCITY = 1500
        private const val MIN_FLING_VELOCITY = 250

        const val TOUCH_STATE_REST = 0
        const val TOUCH_STATE_SCROLLING = 1
        const val TOUCH_STATE_PREV_PAGE = 2
        const val TOUCH_STATE_NEXT_PAGE = 3
    }
}