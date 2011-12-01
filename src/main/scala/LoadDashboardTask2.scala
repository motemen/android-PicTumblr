package net.tokyoenvious.droid.pictumblr

import android.util.Log

class LoadDashboardTask2 (
        tumblr  : Tumblr2,
        onLoad  : (Seq[Tumblr2#PhotoPost]) => Unit,
        onError : (Throwable) => Unit
    )
    extends AsyncTask1[Int, java.lang.Void, Either[Throwable, Seq[Tumblr2#PhotoPost]]] {

    val TAG = "LoadDashboardTask2"

    // val imagesContainer = activity.imagesContainer

    override def onPreExecute () {
        Log.v(TAG, "onPreExecute")
    }

    override def doInBackground (offset : Int) : Either[Throwable, Seq[Tumblr2#PhotoPost]]
        = tumblr.dashboardPhotoPosts("offset" -> offset.toString())

    override def onPostExecute (result : Either[Throwable, Seq[Tumblr2#PhotoPost]]) {
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
