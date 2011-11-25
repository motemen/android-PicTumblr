package net.tokyoenvious.droid.pictumblr

import android.view.ViewGroup
import android.widget.RelativeLayout
import android.util.Log

class LoadDashboardTask2 (activity : PicTumblrActivity2, tumblr : Tumblr2, offset : Int)
    extends AsyncTask0[java.lang.Void, Either[Throwable, Seq[Tumblr2#PhotoPost]]] {

    val imagesContainer = activity.imagesContainer

    override def onPreExecute () {
        Log.v("PicTumblr", "onPreExecute")
    }

    override def doInBackground () : Either[Throwable, Seq[Tumblr2#PhotoPost]] = {
        return tumblr.dashboardPhotoPosts("offset" -> offset.toString())
    }

    override def onPostExecute (result : Either[Throwable, Seq[Tumblr2#PhotoPost]]) {
        Log.v("PicTumblr", "onPostExecute: " + result)

        result match {
            case Left(error) => {
                error.printStackTrace()
                activity.startOAuth()
            }

            case Right(posts) => {
                for (post <- posts) {
                    activity.posts += post

                    val layout = new RelativeLayout(imagesContainer.getContext())
                    layout.setGravity(android.view.Gravity.CENTER)

                    imagesContainer.addView(
                        layout,
                        new ViewGroup.LayoutParams(
                            activity.displayWidth,
                            ViewGroup.LayoutParams.FILL_PARENT
                        )
                    )

                    val task = new LoadPhotoTask2(activity.maxWidth, layout)
                    task.execute(post)
                }

                activity.updateCaption()
            }
        }
    }
}
