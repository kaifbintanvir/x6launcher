package com.kaif.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class GestureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY = 100

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap?.invoke()
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            val dx = e2.x - (e1?.x ?: 0f)
            val dy = e2.y - (e1?.y ?: 0f)
            return if (abs(dx) > abs(dy)) {
                if (abs(dx) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY) {
                    if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                    true
                } else false
            } else {
                if (abs(dy) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY) {
                    if (dy > 0) onSwipeDown?.invoke() else onSwipeUp?.invoke()
                    true
                } else false
            }
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return true
    }
}
