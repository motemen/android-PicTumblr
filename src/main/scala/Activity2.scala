package net.tokyoenvious.droid.pictumblr

import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.Bitmap
import android.util.Log

import scala.collection.mutable.Queue

class PicTumblrActivity2 extends TypedActivity with TumblrOAuthable {
    val TAG = "PicTumblrActivity2"

    lazy val steppedHorizontalScrollView = findView(TR.layout_scrollview)
    lazy val imagesContainer             = findView(TR.images_container)
    lazy val captionTextView             = findView(TR.textview_caption)

    lazy val displayWidth = getSystemService(android.content.Context.WINDOW_SERVICE)
                .asInstanceOf[android.view.WindowManager].getDefaultDisplay().getWidth()

    lazy val maxWidth = displayWidth // TODO make configurable

    val forwardOffset = 5 // TODO make configurable?

    case class Entry (post : Tumblr2#PhotoPost, var task : LoadPhotoTask2, var bitmap : Bitmap)

    val entries = new Queue[Entry]();

    var tumblr : Tumblr2 = null
    var offset : Int = 0

    override def onCreate (savedInstanceState : android.os.Bundle) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        steppedHorizontalScrollView.onNext = () => {
            updateCaption(+1)
            loadNewPosts()
            for (i <- 0 to getCurrentIndex - 1; entry <- entries.get(i)) {
                if (entry.bitmap != null) {
                    entry.bitmap.recycle()
                }
            }
        }

        steppedHorizontalScrollView.onPrev = () => {
            updateCaption(-1)
        }

        tumblr = new Tumblr2(oauthAuthorize())
        createLoadDashboardTask().execute(0)
    }

    def addNewImageContainer () = {
        val layout = new android.widget.RelativeLayout(PicTumblrActivity2.this)
        layout.setGravity(android.view.Gravity.CENTER)

        imagesContainer.addView(
            layout,
            new ViewGroup.LayoutParams(
                displayWidth,
                ViewGroup.LayoutParams.FILL_PARENT
            )
        )
        
        layout
    }

    def onDashboardLoad (loadedPosts : Seq[Tumblr2#PhotoPost]) {
        Log.d(TAG, "loaded: " + loadedPosts.map { _.id }.mkString(","))

        val newEntries = loadedPosts.map { Entry(_, null, null) }

        for ((entry, i) <- newEntries.zipWithIndex) {
            val imageContainer = addNewImageContainer()

            if (imagesContainer.getChildCount() > 20) {
            }

            entry.task = new LoadPhotoTask2(
                maxWidth = maxWidth,
                imageContainer = imageContainer,
                photoPost = entry.post,
                onLoad = (bitmap : Bitmap) => {
                    entry.bitmap = bitmap

                    val imageView = new ImageView(PicTumblrActivity2.this)
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
                    imageView.setImageBitmap(bitmap)
                    imageView.setAdjustViewBounds(true)

                    imageContainer.addView(imageView)
                }
            )
            entry.task.execute()
        }

        entries ++= newEntries
        offset += entries.length
        Log.d(TAG, "offset=" + offset)

        updateCaption()
    }

    def createLoadDashboardTask () = {
        new LoadDashboardTask2(
            tumblr  = tumblr,
            onLoad  = onDashboardLoad,
            onError = (error : Throwable) => {
                error.printStackTrace()
                Log.w(TAG, error.getMessage())
                startOAuth() // TODO check error type
            }
        )
    }

    def getCurrentIndex () : Int = {
        val scrollX = steppedHorizontalScrollView.getScrollX()
        val index = scrollX / displayWidth
        val delta = scrollX % displayWidth

        if (delta != 0) {
            if (delta * 2 < displayWidth) {
                steppedHorizontalScrollView.smoothScrollBy(-delta - displayWidth, 0)
                return index
            } else {
                steppedHorizontalScrollView.smoothScrollBy(-delta + displayWidth, 0)
                return index + 1
            }
        } else {
            return index
        }
 
        return index
    }

    def getCurrentPost (delta : Int = 0) : Option[Tumblr2#PhotoPost]
        = entries.get(getCurrentIndex() + delta) map { _.post }

    def updateCaption (delta : Int = 0) {
        captionTextView.setText(
            getCurrentPost(delta).map { _.plainCaption }.getOrElse(getText(R.string.default_caption))
        )
    }

    def loadNewPosts () {
        var index = getCurrentIndex()
        if (entries.length - index <= forwardOffset) {
            createLoadDashboardTask().execute(offset)
        }
    }
}
