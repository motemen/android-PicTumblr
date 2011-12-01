package net.tokyoenvious.droid.pictumblr

import android.view.View
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.HorizontalScrollView

class SteppedHorizontalScrollView (context : android.content.Context, attrs : android.util.AttributeSet)
        extends HorizontalScrollView(context, attrs) {

    var onNext : () => Unit = null
    var onPrev : () => Unit = null
    var onLongPress : () => Unit = null
    var onDoubleTap : () => Unit = null

    val gestureListener = new GestureDetector.SimpleOnGestureListener() {
        override def onFling (e1 : MotionEvent, e2 : MotionEvent, vx : Float, vy : Float) : Boolean = {
            if (e1 == null || e2 == null) {
                return true
            }

            val scrollView = SteppedHorizontalScrollView.this
            val width      = scrollView.getWidth()
            val scrollX    = scrollView.getScrollX()

            if (e1.getX() - e2.getX() < 0) {
                if (scrollView.onPrev != null) {
                    scrollView.onPrev()
                }
                scrollView.smoothScrollTo(
                    scala.math.floor(scrollX / width.toDouble - 1.0).toInt * width, 0
                )
            } else {
                if (scrollView.onNext != null) {
                    scrollView.onNext()
                }
                scrollView.smoothScrollTo(
                    scala.math.floor(scrollX / width.toDouble + 1.0).toInt * width, 0
                )
            }

            return true
        }

        override def onLongPress (e : MotionEvent) {
            val scrollView = SteppedHorizontalScrollView.this
            if (scrollView.onLongPress != null)
                scrollView.onLongPress()
        }

        override def onDoubleTap (e : MotionEvent) : Boolean = {
            val scrollView = SteppedHorizontalScrollView.this
            if (scrollView.onDoubleTap != null)
                scrollView.onDoubleTap()
            return true
        }
    }
    
    lazy val gestureDetector = new GestureDetector(gestureListener)

    setOnTouchListener(
        new View.OnTouchListener() {
            override def onTouch (v : View, event : MotionEvent) : Boolean = {
                if (gestureDetector.onTouchEvent(event)) {
                    return true
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // do not move
                    return true
                } else {
                    return false
                }
            }
        }
    );
}
