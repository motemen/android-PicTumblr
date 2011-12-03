package net.tokyoenvious.droid.pictumblr

import android.widget.ImageView
import android.util.Log

class ForgetfulBitmapImageView (val entry : Entry, val activity : PicTumblrActivity2)
        extends ImageView(activity) {

    val TAG = "ForgetfulBitmapImageView"

    setScaleType(ImageView.ScaleType.CENTER_INSIDE)
    setAdjustViewBounds(true)
    setImageBitmap(entry.bitmap)

    def getBitmap () = {
        getDrawable().asInstanceOf[android.graphics.drawable.BitmapDrawable].getBitmap()
    }

    override def onDraw (canvas : android.graphics.Canvas) {
        val bitmap = getBitmap()
        if (bitmap != null && bitmap.isRecycled()) {
            Log.v(TAG, "bitmap is recycled")
            val imageContainer = getParent().asInstanceOf[android.view.ViewGroup]
            // TODO rather reuse than remove self
            imageContainer.removeView(this)
            entry.task = activity.createLoadPhotoTask(entry, imageContainer)
            entry.task.execute()
        } else {
            super.onDraw(canvas)
        }
    }
}
