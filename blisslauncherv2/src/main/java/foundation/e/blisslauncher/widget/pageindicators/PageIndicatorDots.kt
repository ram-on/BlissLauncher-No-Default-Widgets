package foundation.e.blisslauncher.widget.pageindicators

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import foundation.e.blisslauncher.R
import kotlin.math.abs

/**
 * [PageIndicator] which shows dots per page. The active page is shown with the current
 * accent color.
 */
class PageIndicatorDots @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {
    private val circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotRadius: Float
    private val activeColor: Int
    private val inActiveColor: Int
    private val isRtl: Boolean = false
    private val sTempRect: RectF = RectF()

    private var numPages = 0
    private var activePage = 0

    /**
     * The current position of the active dot including the animation progress.
     * For ex:
     * 0.0  => Active dot is at position 0
     * 0.33 => Active dot is at position 0 and is moving towards 1
     * 0.50 => Active dot is at position [0, 1]
     * 0.77 => Active dot has left position 0 and is collapsing towards position 1
     * 1.0  => Active dot is at position 1
     */
    private var mCurrentPosition = 0f
    private var mFinalPosition = 0f
    private var mAnimator: ObjectAnimator? = null
    private var mEntryAnimationRadiusFactors: FloatArray? = null

    init {
        circlePaint.style = Paint.Style.FILL
        dotRadius = resources.getDimension(R.dimen.dotSize) / 2
        outlineProvider = MyOutlineProver()
        activeColor = resources.getColor(R.color.dot_on_color)
        inActiveColor = resources.getColor(R.color.dot_on_color)
        //mIsRtl = Utilities.isRtl(getResources())
    }

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        var currentScroll = currentScroll
        if (numPages > 1) {
            // Ignore this as of now.
            if (isRtl) {
                currentScroll = totalScroll - currentScroll
            }
            val scrollPerPage = totalScroll / (numPages - 1)
            val pageToLeft = currentScroll / scrollPerPage
            val pageToLeftScroll = pageToLeft * scrollPerPage
            val pageToRightScroll = pageToLeftScroll + scrollPerPage
            val scrollThreshold =
                SHIFT_THRESHOLD * scrollPerPage
            when {
                currentScroll < pageToLeftScroll + scrollThreshold -> { // scroll is within the left page's threshold
                    animateToPosition(pageToLeft.toFloat())
                }
                currentScroll > pageToRightScroll - scrollThreshold -> { // scroll is far enough from left page to go to the right page
                    animateToPosition(pageToLeft + 1.toFloat())
                }
                else -> { // scroll is between left and right page
                    animateToPosition(pageToLeft + SHIFT_PER_ANIMATION)
                }
            }
        }
    }

    private fun animateToPosition(position: Float) {
        mFinalPosition = position
        if (abs(mCurrentPosition - mFinalPosition) < SHIFT_THRESHOLD) {
            mCurrentPosition = mFinalPosition
        }
        if (mAnimator == null && mCurrentPosition.compareTo(mFinalPosition) != 0) {
            val positionForThisAnim =
                if (mCurrentPosition > mFinalPosition) mCurrentPosition - SHIFT_PER_ANIMATION
                else mCurrentPosition + SHIFT_PER_ANIMATION
            mAnimator = ObjectAnimator.ofFloat(this, CURRENT_POSITION, positionForThisAnim).apply {
                addListener(AnimationCycleListener())
                duration = ANIMATION_DURATION
            }
            mAnimator?.start()
        }
    }

    fun stopAllAnimations() {
        if (mAnimator != null) {
            mAnimator!!.cancel()
            mAnimator = null
        }
        mFinalPosition = activePage.toFloat()
        CURRENT_POSITION.set(this, mFinalPosition)
    }

    /**
     * Sets up up the page indicator to play the entry animation.
     * [.playEntryAnimation] must be called after this.
     */
    fun prepareEntryAnimation() {
        mEntryAnimationRadiusFactors = FloatArray(numPages)
        invalidate()
    }

    fun playEntryAnimation() {
        val count = mEntryAnimationRadiusFactors!!.size
        if (count == 0) {
            mEntryAnimationRadiusFactors = null
            invalidate()
            return
        }
        val interpolator: Interpolator =
            OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION)
        val animSet = AnimatorSet()
        for (i in 0 until count) {
            val anim = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(ENTER_ANIMATION_DURATION.toLong())
            anim!!.addUpdateListener { animation ->
                mEntryAnimationRadiusFactors!![i] =
                    (animation!!.animatedValue as Float)
                invalidate()
            }
            anim.interpolator = interpolator
            anim.startDelay =
                ENTER_ANIMATION_START_DELAY + ENTER_ANIMATION_STAGGERED_DELAY * i.toLong()
            animSet.play(anim)
        }
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                mEntryAnimationRadiusFactors = null
                invalidateOutline()
                invalidate()
            }
        })
        animSet.start()
    }

    override fun setActiveMarker(activePage: Int) {
        if (this.activePage != activePage) {
            this.activePage = activePage
        }
    }

    override fun setMarkersCount(numMarkers: Int) {
        numPages = numMarkers
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        // Add extra spacing of mDotRadius on all sides so that entry animation could be run.
        val width =
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(
                widthMeasureSpec
            ) else ((numPages * 3 + 2) * dotRadius).toInt()
        val height =
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(
                heightMeasureSpec
            ) else (4 * dotRadius).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw all page indicators;
        var circleGap = 3 * dotRadius
        val startX = (width - numPages * circleGap + dotRadius) / 2
        var x = startX + dotRadius
        val y = height / 2.toFloat()
        if (mEntryAnimationRadiusFactors != null) {
            // During entry animation, only draw the circles
            if (isRtl) {
                x = getWidth() - x
                circleGap = -circleGap
            }
            for (i in mEntryAnimationRadiusFactors!!.indices) {
                circlePaint.setColor(if (i == activePage) activeColor else inActiveColor)
                canvas.drawCircle(
                    x,
                    y,
                    dotRadius * mEntryAnimationRadiusFactors!![i],
                    circlePaint
                )
                x += circleGap
            }
        } else {
            circlePaint.color = inActiveColor
            for (i in 0 until numPages) {
                canvas.drawCircle(x, y, dotRadius, circlePaint)
                x += circleGap
            }
            circlePaint.color = activeColor
            canvas.drawRoundRect(activeRect, dotRadius, dotRadius, circlePaint)
        }
    } // Dot is leaving the left circle.

    // dot is capturing the right circle.
    private val activeRect: RectF?
        get() {
            val startCircle: Float = mCurrentPosition
            var delta = mCurrentPosition - startCircle
            val diameter = 2 * dotRadius
            val circleGap = 3 * dotRadius
            val startX = (width - numPages * circleGap + dotRadius) / 2
            sTempRect!!.top = height * 0.5f - dotRadius
            sTempRect.bottom = height * 0.5f + dotRadius
            sTempRect.left = startX + startCircle * circleGap
            sTempRect.right =
                sTempRect.left + diameter
            if (delta < SHIFT_PER_ANIMATION) { // dot is capturing the right circle.
                sTempRect.right += delta * circleGap * 2
            } else { // Dot is leaving the left circle.
                sTempRect.right += circleGap
                delta -= SHIFT_PER_ANIMATION
                sTempRect.left += delta * circleGap * 2
            }
            if (isRtl) {
                val rectWidth = sTempRect.width()
                sTempRect.right =
                    width - sTempRect.left
                sTempRect.left =
                    sTempRect.right - rectWidth
            }
            return sTempRect
        }

    private inner class MyOutlineProver : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            if (mEntryAnimationRadiusFactors == null) {
                val activeRect = activeRect
                outline!!.setRoundRect(
                    activeRect!!.left.toInt(),
                    activeRect.top.toInt(),
                    activeRect.right.toInt(),
                    activeRect.bottom.toInt(),
                    dotRadius
                )
            }
        }
    }

    /**
     * Listener for keep running the animation until the final state is reached.
     */
    private inner class AnimationCycleListener : AnimatorListenerAdapter() {
        private var mCancelled = false
        override fun onAnimationCancel(animation: Animator?) {
            mCancelled = true
        }

        override fun onAnimationEnd(animation: Animator?) {
            if (!mCancelled) {
                mAnimator = null
                animateToPosition(mFinalPosition)
            }
        }
    }

    companion object {
        private const val SHIFT_PER_ANIMATION = 0.5f
        private const val SHIFT_THRESHOLD = 0.1f
        private const val ANIMATION_DURATION: Long = 150
        private const val ENTER_ANIMATION_START_DELAY = 300
        private const val ENTER_ANIMATION_STAGGERED_DELAY = 150
        private const val ENTER_ANIMATION_DURATION = 400

        // This value approximately overshoots to 1.5 times the original size.
        private const val ENTER_ANIMATION_OVERSHOOT_TENSION = 4.9f
        private val CURRENT_POSITION: Property<PageIndicatorDots, Float> =
            object : Property<PageIndicatorDots, Float>(
                Float::class.java, "current_position"
            ) {
                override fun get(obj: PageIndicatorDots): Float {
                    return obj.mCurrentPosition
                }

                override fun set(obj: PageIndicatorDots, pos: Float) {
                    obj.mCurrentPosition = pos
                    obj.invalidate()
                    obj.invalidateOutline()
                }
            }
    }
}