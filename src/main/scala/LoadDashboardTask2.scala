package net.tokyoenvious.droid.pictumblr

import android.util.Log

class LoadDashboardTask2 (
        tumblr  : Tumblr2,
        onLoad  : (Seq[TumblrPhotoPost]) => Unit,
        onError : (Throwable) => Unit
    )
    extends AsyncTask1[Int, java.lang.Void, Either[Throwable, Seq[TumblrPhotoPost]]] {

    val TAG = "LoadDashboardTask2"

    // val imagesContainer = activity.imagesContainer

    override def onPreExecute () {
        Log.v(TAG, "onPreExecute")
    }

    override def doInBackground (offset : Int) : Either[Throwable, Seq[TumblrPhotoPost]]
        = tumblr.dashboardPhotoPosts("offset" -> offset.toString())

    override def onPostExecute (result : Either[Throwable, Seq[TumblrPhotoPost]]) {
        Log.v(TAG, "onPostExecute: " + result)

        result match {
            case Left(error) => {
                onError(error)
            }

            case Right(posts) => {
                onLoad(posts)
            }
        }
    }
}
