package net.tokyoenvious.droid.pictumblr

import android.widget.ImageView
import android.widget.ProgressBar
import android.graphics.{ Bitmap, BitmapFactory }
import android.util.Log

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.BufferedHttpEntity

class LoadPhotoTask2 (
        maxWidth : Int,
        imageContainer : android.view.ViewGroup,
        photoPost : Tumblr2#PhotoPost,
        onLoad : (Bitmap) => Unit
    )
    extends AsyncTask0[java.lang.Void, Bitmap] {

    val TAG = "LoadPhotoTask2"

    override def doInBackground () : Bitmap = {
        try {
            val httpClient   = new DefaultHttpClient
            val httpGet      = new HttpGet(photoPost.largestPhotoWithMaxWidth(maxWidth).url)
            val httpResponse = httpClient.execute(httpGet)

            val options = new BitmapFactory.Options
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = BitmapFactory.decodeStream(
                new BufferedHttpEntity(httpResponse.getEntity).getContent(), null, options
            )

            return bitmap
        } catch {
            // TODO user feedback
            case e => {
                e.printStackTrace
                Log.w(TAG, e.getMessage())
                return null
            }
        }
    }

    override def onPreExecute () {
        val progressBar = new ProgressBar(imageContainer.getContext())
        imageContainer.addView(progressBar)
    }

    override def onPostExecute (bitmap : Bitmap) {
        // remove progressbar
        imageContainer.removeViewAt(0)

        onLoad(bitmap)
    }
}

