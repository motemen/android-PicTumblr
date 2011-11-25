package net.tokyoenvious.droid.pictumblr

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.preference.PreferenceManager
import android.util.Log

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider

import scala.collection.mutable.Queue

class PicTumblrActivity2 extends TypedActivity {
    val CONSUMER_KEY    = LocalDefs.TUMBLR_OAUTH_CONSUMER_KEY
    val CONSUMER_SECRET = LocalDefs.TUMBLR_OAUTH_CONSUMER_SECRET

    val CALLBACK_URL = "pictumblr://oauth/callback"

    val PREFERENCE_NAME_OAUTH = "OAuth"
    val PREFERENCE_KEY_TOKEN        = "token"
    val PREFERENCE_KEY_TOKEN_SECRET = "token_secret"

    lazy val consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    lazy val provider = new CommonsHttpOAuthProvider(
        "http://www.tumblr.com/oauth/request_token",
        "http://www.tumblr.com/oauth/access_token",
        "http://www.tumblr.com/oauth/authorize"
    )

    lazy val preferences = PreferenceManager.getDefaultSharedPreferences(this)

    lazy val steppedHorizontalScrollView = findView(TR.layout_scrollview)
    lazy val imagesContainer             = findView(TR.images_container)
    lazy val captionTextView             = findView(TR.textview_caption)

    lazy val displayWidth = getSystemService(Context.WINDOW_SERVICE)
                .asInstanceOf[WindowManager].getDefaultDisplay().getWidth

    lazy val maxWidth = displayWidth // TODO make configurable

    val posts = new Queue[Tumblr2#PhotoPost]();

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        steppedHorizontalScrollView.onNext = () => {
            updateCaption(+1)
        }

        steppedHorizontalScrollView.onPrev = () => {
            updateCaption(-1)
        }

        val uri = getIntent().getData()
        Log.d("PicTumblrActivity#onCreate", "Got intent: " + uri)

        val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val token       = prefs.getString(PREFERENCE_KEY_TOKEN, null)
        val tokenSecret = prefs.getString(PREFERENCE_KEY_TOKEN_SECRET, null)
        Log.v("PicTumblrActivity#onCreate", "token: " + token + " tokenSecret: " + tokenSecret)

        if (token != null && tokenSecret != null) {
            consumer.setTokenWithSecret(token, tokenSecret)
        }

        if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
            val verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER)
            provider.setOAuth10a(true)
            provider.retrieveAccessToken(consumer, verifier)

            val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
            val editor = prefs.edit()
            editor.putString(PREFERENCE_KEY_TOKEN,        consumer.getToken())
            editor.putString(PREFERENCE_KEY_TOKEN_SECRET, consumer.getTokenSecret())
            editor.commit()

            Log.d("PicTumblrActivity#onCreate", "Got token: " + consumer.getToken())
            Log.d("PicTumblrActivity#onCreate", "Got token_secret: " + consumer.getTokenSecret())
        }

        val tumblr = new Tumblr2(consumer)
        val task = new LoadDashboardTask2(this, tumblr, 0)
        task.execute()
    }

    def startOAuth () {
        val url = provider.retrieveRequestToken(consumer, CALLBACK_URL)

        Log.d("PicTumblrActivity#startOAuth", "Generated token: "        + consumer.getToken())
        Log.d("PicTumblrActivity#startOAuth", "Generated token_secret: " + consumer.getTokenSecret())

        val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val editor = prefs.edit()
        editor.putString(PREFERENCE_KEY_TOKEN,        consumer.getToken())
        editor.putString(PREFERENCE_KEY_TOKEN_SECRET, consumer.getTokenSecret())
        editor.commit()

        val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent) // tumblr.com -> (user allows) -[callback]-> this Activity
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
        = posts.get(getCurrentIndex() + delta)

    def updateCaption (delta : Int = 0) {
        captionTextView.setText(
            getCurrentPost(delta).map { _.plainCaption }.getOrElse("default")
        )
    }
}
