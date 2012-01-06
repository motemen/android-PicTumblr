package net.tokyoenvious.droid.pictumblr

class ReblogPostTask2 (tumblr : Tumblr2, callback : (Long) => Unit)
        extends AsyncTask1[TumblrPhotoPost, java.lang.Void, Long] {

    override def doInBackground (post : TumblrPhotoPost) : Long = {
        tumblr.reblog(post)
    }

    override def onPostExecute (postId : Long) {
        callback(postId)
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

class DeletePostTask2 (tumblr : Tumblr2, callback : => Unit)
        extends AsyncTask1[Long, java.lang.Void, Unit] {

    override def doInBackground (postId : Long) : Unit = {
        tumblr.deletePost(postId)
    }

    override def onPostExecute (u : Unit) {
        callback
    }
}
