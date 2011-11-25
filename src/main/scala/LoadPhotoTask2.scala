package net.tokyoenvious.droid.pictumblr

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.graphics.{ Bitmap, BitmapFactory }

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.BufferedHttpEntity

class LoadPhotoTask2 (maxWidth : Int, imageContainer : ViewGroup)
        extends AsyncTask1[Tumblr2#PhotoPost, java.lang.Void, Bitmap] {

    override def doInBackground (photoPost : Tumblr2#PhotoPost) : Bitmap = {
        // これだと読み込みに失敗することが多い
        // ref. http://stackoverflow.com/questions/1630258/android-problem-bug-with-threadsafeclientconnmanager-downloading-images
        /*
        val bitmap = BitmapFactory.decodeStream(
            new java.net.URL(photoPost.photoUrl).openConnection.getInputStream, null, options
        )
        */

        try {
            val httpClient   = new DefaultHttpClient
            val httpGet      = new HttpGet(photoPost.largestPhotoWithMaxWidth(maxWidth).url)
            val httpResponse = httpClient.execute(httpGet)

            // 単純に Drawable.createFromStream() するとメモリを食うので Bitmap.Config.RGB_565 を指定
            val options = new BitmapFactory.Options
            options.inPreferredConfig = Bitmap.Config.RGB_565
            // options.inSampleSize = 2 /// test

            val bitmap = BitmapFactory.decodeStream(
                new BufferedHttpEntity(httpResponse.getEntity).getContent(), null, options
            )
            // Log.d("LoadPhotoTask", "doInBackground: loaded " + photoPost.photoUrl)

            return bitmap
        } catch {
            // TODO 何かしら表示する
            case e => {
                e.printStackTrace
                return null
            }
        }
    }

    override def onPreExecute () {
        val progressBar = new ProgressBar(imageContainer.getContext())
        imageContainer.addView(progressBar)
    }

    override def onPostExecute (bitmap : Bitmap) {
        imageContainer.removeViewAt(0)

        val imageView = new ImageView(imageContainer.getContext())
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        imageView.setImageBitmap(bitmap)
        imageView.setAdjustViewBounds(true)

        imageContainer.addView(imageView)
    }
}

