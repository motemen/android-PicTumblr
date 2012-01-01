package net.tokyoenvious.droid.pictumblr

class ReblogPostTask2 (tumblr : Tumblr2, callback : => Unit)
        extends AsyncTask1[TumblrPhotoPost, java.lang.Void, Unit] {

    override def doInBackground (post : TumblrPhotoPost) : Unit = {
        tumblr.reblog(post)
    }

    override def onPostExecute (u : Unit) {
        callback
    }
}

class LikePostTask2 (tumblr : Tumblr2, callback : => Unit)
        extends AsyncTask1[TumblrPhotoPost, java.lang.Void, Unit] {

    override def doInBackground (post : TumblrPhotoPost) : Unit = {
        tumblr.like(post)
    }

    override def onPostExecute (u : Unit) {
        callback
    }
}
