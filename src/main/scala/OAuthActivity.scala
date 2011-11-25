package net.tokyoenvious.droid.pictumblr

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider

class PicTumblrOAuthActivity extends Activity {
    val CONSUMER_KEY    = LocalDefs.TUMBLR_OAUTH_CONSUMER_KEY
    val CONSUMER_SECRET = LocalDefs.TUMBLR_OAUTH_CONSUMER_SECRET

    val CALLBACK_URL = "pictumblr://oauth/callback"

    val PREFERENCE_NAME_OAUTH = "OAuth"

    lazy val consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    lazy val provider = new CommonsHttpOAuthProvider(
        "http://www.tumblr.com/oauth/request_token",
        "http://www.tumblr.com/oauth/access_token",
        "http://www.tumblr.com/oauth/authorize"
    )

    lazy val preferences = PreferenceManager.getDefaultSharedPreferences(this)

    override def onCreate (savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState)

        val uri = getIntent().getData()
        Log.d("PicTumblrOAuthActivity#onCreate", "Got intent: " + uri)

        val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val token = prefs.getString("token", null)
        val tokenSecret = prefs.getString("token_secret", null)
        Log.v("PicTumblrOAuthActivity#onCreate", "token: " + token + " tokenSecret: " + tokenSecret)

        if (token != null && tokenSecret != null) {
            consumer.setTokenWithSecret(token, tokenSecret)
        }

        if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
            val verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER)
            provider.setOAuth10a(true)
            provider.retrieveAccessToken(consumer, verifier)

            val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
            val editor = prefs.edit()
            editor.putString("token", consumer.getToken())
            editor.putString("token_secret", consumer.getTokenSecret())
            editor.commit()

            Log.d("PicTumblrOAuthActivity#onCreate", "Got token: " + consumer.getToken())
            Log.d("PicTumblrOAuthActivity#onCreate", "Got token_secret: " + consumer.getTokenSecret())
        }

        val tumblr = new Tumblr2(oauthConsumer = consumer)
        Log.d("PicTumblrOAuthActivity#onCreate", "dashboard: " + tumblr.dashboardPhotoPosts("type" -> "photo").toString())

        val button = new android.widget.Button(this)
        button.setOnClickListener(new android.view.View.OnClickListener() { def onClick(v : android.view.View) { PicTumblrOAuthActivity.this.startOAuth }})
        this.setContentView(button)
    }

    def startOAuth {
        val url = provider.retrieveRequestToken(consumer, CALLBACK_URL)

        Log.d("PicTumblrOAuthActivity#startOAuth", "Generated token: " + consumer.getToken())
        Log.d("PicTumblrOAuthActivity#startOAuth", "Generated token_secret: " + consumer.getTokenSecret())

        val prefs = getSharedPreferences(PREFERENCE_NAME_OAUTH, 0)
        val editor = prefs.edit()
        editor.putString("token", consumer.getToken())
        editor.putString("token_secret", consumer.getTokenSecret())
        editor.commit()

        val intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent) // tumblr.com -> (user allows) -[callback]-> onNewIntent()
    }
}
