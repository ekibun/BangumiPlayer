package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.animation.Animator
import android.view.MotionEvent
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.ViewConfiguration
import com.github.chrisbanes.photoview.PhotoView
import java.util.*


class DragPhotoView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    private val mPaint: Paint = Paint()

    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()

    private var mTranslateY: Float = 0.toFloat()
    private var mTranslateX: Float = 0.toFloat()
    private var mScale = 1f
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var minScale = 0.5f
    private var mAlpha = 255
    private var canFinish = 0
    private var isAnimate = false

    //is event on PhotoView
    private var isTouchEvent = false
    var mTapListener: (()->Unit)? = null
    var mExitListener: (()->Unit)? = null
    var mLongClickListener: (()->Unit)? = null

    private val alphaAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofInt(mAlpha, 255)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mAlpha = valueAnimator.animatedValue as Int }
            return animator
        }

    private val translateYAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mTranslateY, 0f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mTranslateY = valueAnimator.animatedValue as Float }
            return animator
        }

    private val translateXAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mTranslateX, 0f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator -> mTranslateX = valueAnimator.animatedValue as Float }
            return animator
        }

    private val scaleAnimation: ValueAnimator
        get() {
            val animator = ValueAnimator.ofFloat(mScale, 1f)
            animator.duration = DURATION
            animator.addUpdateListener { valueAnimator ->
                mScale = valueAnimator.animatedValue as Float
                invalidate()
            }

            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    isAnimate = true
                }
                override fun onAnimationEnd(animator: Animator) {
                    isAnimate = false
                    animator.removeAllListeners()
                }
                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
            return animator
        }

    init {
        mPaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        mPaint.alpha = mAlpha
        canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mPaint)
        canvas.translate(mTranslateX, mTranslateY)
        canvas.scale(mScale, mScale, mWidth.toFloat() / 2, mHeight.toFloat() / 2)
        super.onDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = w
        mHeight = h
    }

    private var timer = Timer()
    private var timeoutTask: TimerTask? = null
    private var longClick = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val moveY = event.y
        val moveX = event.x
        val translateX = moveX - mDownX
        val translateY = moveY - mDownY

        //only scale == 1 can drag
        if (scale == 1f) {

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event)
                    longClick = false
                    timeoutTask?.cancel()
                    timeoutTask = object: TimerTask(){
                        override fun run() {
                            if (scale != 1f) return
                            this@DragPhotoView.post {mLongClickListener?.invoke()}
                            longClick = true
                        }
                    }
                    timer.schedule(timeoutTask, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if(translateY != 0f || translateX != 0f){
                        timeoutTask?.cancel()
                    }
                    //in viewpager
                    //如果不消费事件，则不作操作
                    if (!isTouchEvent && Math.abs(translateY) < Math.abs(translateX)) {
                        mScale = 1f
                        performAnimation()
                        return super.dispatchTouchEvent(event)
                    }

                    //single finger drag  down
                    //如果有上下位移 则不交给viewpager
                    if (event.pointerCount == 1 ) {
                        if(isTouchEvent)
                            onActionMove(event)

                        if (Math.abs(translateY) > Math.abs(translateX)) {
                            isTouchEvent = true
                        }
                        return true
                    }

                    //防止下拉的时候双手缩放
                    if (isTouchEvent) {
                        return true
                    }
                }

                MotionEvent.ACTION_UP ->
                    //防止下拉的时候双手缩放
                    if (event.pointerCount == 1) {
                        timeoutTask?.cancel()
                        if (translateX == 0f && translateY == 0f &&!longClick){
                            timeoutTask = object: TimerTask(){
                                override fun run() {
                                    if (scale != 1f) return
                                    this@DragPhotoView.post { mTapListener?.invoke() }
                                }
                            }
                            timer.schedule(timeoutTask, ViewConfiguration.getDoubleTapTimeout().toLong())
                        }

                        if (mTranslateY > MAX_TRANSLATE_Y) {
                            mExitListener?.invoke()
                        } else {
                            performAnimation()
                        }
                        isTouchEvent = false
                    }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun onActionMove(event: MotionEvent) {
        val moveY = event.y
        val moveX = event.x
        mTranslateX = moveX - mDownX
        mTranslateY = moveY - mDownY

        val percent = Math.max(0f, mTranslateY) / MAX_TRANSLATE_Y

        if (mScale in minScale..1f) {
            mScale = (1 - percent)* 0.5f + 0.5f

            mAlpha = (155 * (1 - percent)).toInt() + 100
            if (mAlpha > 255) {
                mAlpha = 255
            } else if (mAlpha < 100) {
                mAlpha = 100
            }
        }
        if (mScale < minScale) {
            mScale = minScale
        } else if (mScale > 1f) {
            mScale = 1f
        }
        if(mTranslateY > 0){
            mTranslateX += (mDownX - mWidth /2) * (1-mScale)
            mTranslateY += (mDownY - mHeight/2) * (1-mScale)
        }

        invalidate()
    }

    private fun performAnimation() {
        scaleAnimation.start()
        translateXAnimation.start()
        translateYAnimation.start()
        alphaAnimation.start()
    }

    private fun onActionDown(event: MotionEvent) {
        mDownX = event.x
        mDownY = event.y
    }

    companion object {
        private const val MAX_TRANSLATE_Y = 500
        private const val DURATION: Long = 300
    }
}